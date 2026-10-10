package com.example.parking.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import com.example.parking.model.RoleName;
import com.example.parking.repository.UserRepository;

import java.util.Arrays;
import java.util.Map;

/**
 * Sends HTML notification emails to the configured administrator address.
 * <p>
 * Runs asynchronously on its own thread pool and is completely isolated from the booking transaction:
 * every failure is caught here, logged WITHOUT credentials, and turned into a dashboard "system notice".
 * Nothing in this class can make a booking or checkout fail.
 * <p>
 * All settings come from configuration (application.properties / environment variables) - nothing is hardcoded.
 */
@Service
public class AdminEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AdminEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AdminNotificationService notificationService;
    private final UserRepository userRepository;

    @Value("${admin.notification.email:}")
    private String adminEmail;

    @Value("${admin.notification.from:}")
    private String fromOverride;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    public AdminEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                             AdminNotificationService notificationService,
                             UserRepository userRepository) {
        this.mailSenderProvider = mailSenderProvider;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Sends the email in the background. Never throws.
     *
     * @param accentColor header colour of the email (hex), e.g. "#2563eb"
     */
    @Async("mailExecutor")
    public void sendAsync(NotificationContent content, String accentColor) {
        String subject = content.title() + (content.bookingReference() == null ? "" : " - " + content.bookingReference());
        try {
            String[] recipients = resolveRecipients();
            String skipReason = notConfiguredReason(recipients);
            if (skipReason != null) {
                logger.warn("Admin email '{}' not sent: {}. The dashboard notification was still created.", subject, skipReason);
                recordEmailSystemNotice(subject, content, skipReason);
                return;
            }
            JavaMailSender sender = mailSenderProvider.getIfAvailable();
            if (sender == null) {
                String reason = "no JavaMailSender is available";
                logger.warn("Admin email '{}' not sent: {}.", subject, reason);
                recordEmailSystemNotice(subject, content, reason);
                return;
            }

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(resolveFrom());
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(buildHtml(content, accentColor), true);
            sender.send(message);

            logger.info("Admin email '{}' sent.", subject);
        } catch (Exception ex) {
            handleFailure(subject, content, ex);
        }
    }

    private void recordEmailSystemNotice(String subject, NotificationContent content, String reason) {
        try {
            notificationService.recordSystemNotice(
                    "Admin email not delivered",
                    "Email '" + subject + "' was not sent. " + reason + ".",
                    content.bookingReference());
        } catch (Exception inner) {
            logger.warn("Could not record the email configuration notice: {}", inner.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------ failure handling

    private void handleFailure(String subject, NotificationContent content, Exception ex) {
        // Log class names and a credential-masked message only - never the stack trace or settings.
        logger.warn("Admin email '{}' could not be sent ({}): {}. Booking data and dashboard notification are unaffected.",
                subject, ex.getClass().getSimpleName(), safeMessage(ex));
        try {
            String reason = (ex instanceof MailAuthenticationException)
                    ? "SMTP authentication failed. Check MAIL_USERNAME / MAIL_PASSWORD."
                    : "SMTP server unreachable or rejected the message.";
            notificationService.recordSystemNotice(
                    "Admin email not delivered",
                    "Email '" + subject + "' could not be sent. " + reason,
                    content.bookingReference());
        } catch (Exception inner) {
            logger.warn("Could not record the email-failure notice: {}", inner.getClass().getSimpleName());
        }
    }

    private String safeMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String text = root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage());
        if (mailPassword != null && !mailPassword.isBlank()) {
            text = text.replace(mailPassword, "****");
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            text = text.replace(mailUsername, "<mail-user>");
        }
        text = text.replaceAll("[\\r\\n]+", " ");
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    // ------------------------------------------------------------------ configuration

    /** @return null when sending is possible, otherwise a human-readable reason (never contains secrets). */
    private String notConfiguredReason(String[] recipients) {
        if (recipients == null || recipients.length == 0) {
            return "no enabled administrator email address is available";
        }
        if (isBlank(mailHost)) {
            return "spring.mail.host is not configured";
        }
        if (smtpAuth && (isBlank(mailUsername) || isBlank(mailPassword))) {
            return "SMTP credentials (MAIL_USERNAME / MAIL_PASSWORD) are not configured";
        }
        return null;
    }

    private String resolveFrom() {
        if (!isBlank(fromOverride)) {
            return fromOverride.trim();
        }
        return mailUsername.trim();
    }

    private String[] resolveRecipients() {
        if (!isBlank(adminEmail)) {
            return Arrays.stream(adminEmail.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        }
        return userRepository.findEnabledByRoleName(RoleName.ROLE_ADMIN).stream()
                .map(u -> u.getEmail())
                .filter(s -> !isBlank(s))
                .map(String::trim)
                .distinct()
                .toArray(String[]::new);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ------------------------------------------------------------------ HTML

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    /** Clean, inline-styled HTML (email clients ignore external CSS). All dynamic values are HTML-escaped. */
    static String buildHtml(NotificationContent c, String accent) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;\">")
          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f1f5f9;padding:24px 0;\"><tr><td align=\"center\">")
          .append("<table role=\"presentation\" width=\"620\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:620px;width:100%;background:#ffffff;border-radius:10px;overflow:hidden;border:1px solid #e2e8f0;\">");

        // header
        sb.append("<tr><td style=\"background:").append(esc(accent)).append(";padding:22px 28px;color:#ffffff;\">")
          .append("<div style=\"font-size:12px;letter-spacing:1px;text-transform:uppercase;opacity:.85;\">Smart Parking Management System</div>")
          .append("<div style=\"font-size:22px;font-weight:bold;margin-top:6px;\">").append(esc(c.title())).append("</div>");
        if (c.bookingReference() != null) {
            sb.append("<div style=\"font-size:14px;margin-top:4px;opacity:.95;\">Reference: ")
              .append(esc(c.bookingReference())).append("</div>");
        }
        sb.append("</td></tr>");

        // sections
        sb.append("<tr><td style=\"padding:8px 28px 20px 28px;\">");
        for (Map.Entry<String, Map<String, String>> section : c.sections().entrySet()) {
            sb.append("<div style=\"font-size:15px;font-weight:bold;color:#0f172a;margin:20px 0 8px 0;padding-bottom:6px;border-bottom:2px solid ")
              .append(esc(accent)).append(";\">").append(esc(section.getKey())).append("</div>")
              .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
            for (Map.Entry<String, String> row : section.getValue().entrySet()) {
                sb.append("<tr>")
                  .append("<td style=\"padding:6px 0;width:38%;font-size:13px;color:#64748b;vertical-align:top;\">").append(esc(row.getKey())).append("</td>")
                  .append("<td style=\"padding:6px 0;font-size:14px;color:#0f172a;font-weight:600;vertical-align:top;\">").append(esc(row.getValue())).append("</td>")
                  .append("</tr>");
            }
            sb.append("</table>");
        }
        sb.append("</td></tr>");

        // footer
        sb.append("<tr><td style=\"background:#f8fafc;padding:14px 28px;font-size:12px;color:#94a3b8;border-top:1px solid #e2e8f0;\">")
          .append("This is an automated message for administrators. Please do not reply.")
          .append("</td></tr></table></td></tr></table></body></html>");
        return sb.toString();
    }
}
