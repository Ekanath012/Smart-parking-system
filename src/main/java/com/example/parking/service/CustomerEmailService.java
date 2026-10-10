package com.example.parking.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** Sends customer-facing parking and password-reset emails using the same configured SMTP account. */
@Service
public class CustomerEmailService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${admin.notification.from:}")
    private String fromOverride;

    public CustomerEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    /** Booking/checkout mail. Failure is isolated from the database transaction. */
    @Async("mailExecutor")
    public void sendParkingEventAsync(NotificationContent content, String accentColor) {
        String recipient = content.customerEmail();
        if (isBlank(recipient)) {
            logger.warn("Customer email skipped for booking {}: customer email is empty.", content.bookingReference());
            return;
        }
        try {
            if (isBlank(mailHost) || isBlank(mailUsername) || isBlank(mailPassword)) {
                logger.warn("Customer email for booking {} was not sent because SMTP credentials are not configured.", content.bookingReference());
                return;
            }
            JavaMailSender sender = mailSenderProvider.getIfAvailable();
            if (sender == null) {
                logger.warn("Customer email for booking {} was not sent because JavaMailSender is unavailable.", content.bookingReference());
                return;
            }
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(resolveFrom());
            helper.setTo(recipient.trim());
            helper.setSubject(content.title() + (isBlank(content.bookingReference()) ? "" : " - " + content.bookingReference()));
            helper.setText(AdminEmailService.buildHtml(content, accentColor), true);
            sender.send(message);
            logger.info("Customer email sent for booking {} to {}.", content.bookingReference(), maskEmail(recipient));
        } catch (Exception ex) {
            logger.warn("Customer email for booking {} could not be sent ({}).", content.bookingReference(), ex.getClass().getSimpleName());
        }
    }

    /** Password reset email is intentionally synchronous so the caller knows whether the send attempt succeeded. */
    public boolean sendPasswordResetEmail(String recipient, String customerName, String resetUrl, int expiryMinutes) {
        if (isBlank(recipient) || isBlank(resetUrl)) {
            return false;
        }
        try {
            if (isBlank(mailHost) || isBlank(mailUsername) || isBlank(mailPassword)) {
                logger.warn("Password reset email was not sent because SMTP credentials are not configured.");
                return false;
            }
            JavaMailSender sender = mailSenderProvider.getIfAvailable();
            if (sender == null) {
                logger.warn("Password reset email was not sent because JavaMailSender is unavailable.");
                return false;
            }
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(resolveFrom());
            helper.setTo(recipient.trim());
            helper.setSubject("Reset Your Smart Parking Password");
            helper.setText(buildResetHtml(customerName, resetUrl, expiryMinutes), true);
            sender.send(message);
            return true;
        } catch (Exception ex) {
            logger.warn("Password reset email could not be sent ({}).", ex.getClass().getSimpleName());
            return false;
        }
    }

    private String resolveFrom() {
        return isBlank(fromOverride) ? mailUsername.trim() : fromOverride.trim();
    }

    private static String buildResetHtml(String customerName, String resetUrl, int expiryMinutes) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:24px;background:#f1f5f9;font-family:Arial,sans-serif;\">"
                + "<div style=\"max-width:600px;margin:auto;background:#fff;border-radius:12px;padding:32px;border:1px solid #e2e8f0;\">"
                + "<h2 style=\"color:#2563eb;\">Smart Parking</h2>"
                + "<p>Hello " + HtmlUtils.htmlEscape(customerName == null || customerName.isBlank() ? "there" : customerName) + ",</p>"
                + "<p>We received a request to reset your Smart Parking account password.</p>"
                + "<p style=\"margin:28px 0;\"><a href=\"" + HtmlUtils.htmlEscape(resetUrl) + "\" style=\"background:#2563eb;color:#fff;text-decoration:none;padding:12px 22px;border-radius:8px;display:inline-block;font-weight:bold;\">Reset Password</a></p>"
                + "<p>This link expires in " + expiryMinutes + " minutes and can be used only once.</p>"
                + "<p>If you did not request this, you can safely ignore this email.</p>"
                + "</div></body></html>";
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "<hidden>";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
