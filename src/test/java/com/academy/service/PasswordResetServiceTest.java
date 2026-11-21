package com.academy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.academy.dto.ResetPasswordRequest;
import com.academy.entity.PasswordResetToken;
import com.academy.entity.User;
import com.academy.repository.PasswordResetTokenRepository;
import com.academy.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetService service;

    @Test
    void resetRequestIsSilentForUnknownEmailAndEmailsTokenForKnownUser() {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://academy.example///");
        ReflectionTestUtils.setField(service, "mailFrom", "academy@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestReset("unknown@example.com");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));

        User user = User.builder().id(12L).email("student@example.com").build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        service.requestReset("student@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).deleteAllByUser_Id(12L);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertEquals(43, savedToken.getTokenHash().length());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        assertEquals("academy@example.com", mail.getFrom());
        assertEquals("student@example.com", Objects.requireNonNull(mail.getTo())[0]);
        assertEquals("Reset your Kriegspiel Chess Academy password", mail.getSubject());
        assertTrue(Objects.requireNonNull(mail.getText()).contains("https://academy.example/reset-password?token="));
        assertTrue(mail.getText().contains("This link expires in 30 minutes"));
    }

    @Test
    void resetPasswordUpdatesPasswordAndInvalidatesAllUnusedTokens() {
        User user = User.builder().id(4L).password("old").build();
        PasswordResetToken token = PasswordResetToken.builder().user(user)
                .expiresAt(Instant.now().plusSeconds(120)).build();
        PasswordResetToken otherToken = PasswordResetToken.builder().user(user)
                .expiresAt(Instant.now().plusSeconds(120)).build();
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        when(tokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(token));
        when(tokenRepository.findAllByUser_IdAndUsedAtIsNull(4L)).thenReturn(List.of(token, otherToken));

        service.resetPassword(new ResetPasswordRequest("raw-token", "new-password"));

        assertEquals("encoded-password", user.getPassword());
        assertNotNull(token.getUsedAt());
        assertNotNull(otherToken.getUsedAt());
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void rejectsUnknownUsedAndExpiredResetTokens() {
        PasswordResetToken used = PasswordResetToken.builder().usedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        PasswordResetToken expired = PasswordResetToken.builder().expiresAt(Instant.now().minusSeconds(1)).build();
        when(tokenRepository.findByTokenHash(any(String.class)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(used))
                .thenReturn(Optional.of(expired));

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(new ResetPasswordRequest("unknown", "password")));
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(new ResetPasswordRequest("used", "password")));
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(new ResetPasswordRequest("expired", "password")));
    }

    @Test
    void resetEmailRequiresConfiguredSenderAddress() {
        ReflectionTestUtils.setField(service, "mailFrom", " ");
        User user = User.builder().id(2L).build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> service.requestReset("student@example.com"));

        ReflectionTestUtils.setField(service, "mailFrom", null);
        assertThrows(IllegalStateException.class, () -> service.requestReset("student@example.com"));
    }
}
