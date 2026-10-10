package com.example.parking.service;

import com.example.parking.model.PasswordResetToken;
import com.example.parking.model.User;
import com.example.parking.repository.PasswordResetTokenRepository;
import com.example.parking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerEmailService customerEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.expiry-minutes:15}")
    private int expiryMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                CustomerEmailService customerEmailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerEmailService = customerEmailService;
    }

    @Transactional
    public boolean requestReset(String email, String baseUrl) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        Optional<User> userOpt = normalized.isBlank() ? Optional.empty() : userRepository.findByEmail(normalized);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return false;
        }

        User user = userOpt.get();
        tokenRepository.deleteByUserId(user.getId());

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        tokenRepository.save(token);

        String resetUrl = baseUrl + "/reset-password?token=" + rawToken;
        boolean sent = customerEmailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetUrl, expiryMinutes);
        if (!sent) {
            tokenRepository.delete(token);
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public boolean isValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;
        return tokenRepository.findByTokenHashAndUsedAtIsNull(hash(rawToken))
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (rawToken == null || rawToken.isBlank() || newPassword == null || newPassword.length() < 6
                || !newPassword.equals(confirmPassword)) {
            return false;
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenHashAndUsedAtIsNull(hash(rawToken));
        if (tokenOpt.isEmpty()) return false;
        PasswordResetToken token = tokenOpt.get();
        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) return false;

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        return true;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        // Expired tokens are harmless because validation checks expiry, but removing them keeps the table small.
        tokenRepository.findAll().stream()
                .filter(t -> t.getExpiresAt().isBefore(LocalDateTime.now()) || t.getUsedAt() != null)
                .forEach(tokenRepository::delete);
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
