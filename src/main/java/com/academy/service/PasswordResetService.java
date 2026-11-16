package com.academy.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.dto.ResetPasswordRequest;
import com.academy.entity.PasswordResetToken;
import com.academy.repository.PasswordResetTokenRepository;
import com.academy.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail-from:}")
    private String mailFrom;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = createToken();
            tokenRepository.deleteAllByUser_Id(user.getId());
            tokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hashToken(rawToken))
                    .expiresAt(Instant.now().plus(TOKEN_LIFETIME))
                    .build());
            sendResetEmail(email, rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();
        PasswordResetToken token = tokenRepository.findByTokenHash(hashToken(request.token()))
                .filter(resetToken -> resetToken.getUsedAt() == null && resetToken.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new IllegalArgumentException("This password reset link is invalid or expired."));

        token.getUser().setPassword(passwordEncoder.encode(request.password()));
        tokenRepository.findAllByUser_IdAndUsedAtIsNull(token.getUser().getId())
                .forEach(resetToken -> resetToken.setUsedAt(now));
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private void sendResetEmail(String email, String token) {
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("Configure MAIL_FROM or spring.mail.username before sending reset emails");
        }

        String resetLink = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Reset your Kriegspiel Chess Academy password");
        message.setText("A password reset was requested for your account. Use the link below to choose a new password.\n\n"
                + resetLink + "\n\nThis link expires in 30 minutes and can only be used once. If you did not request this, you can ignore this email.");
        mailSender.send(message);
    }
}
