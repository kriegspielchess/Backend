package com.academy.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.academy.dto.LoginRequest;
import com.academy.dto.RegisterRequest;
import com.academy.entity.Role;
import com.academy.entity.StudentProfileStatus;
import com.academy.entity.User;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.UserRepository;
import com.academy.security.JwtUtil;
import com.academy.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginAuthenticatesAndReturnsTokenAndUser() {
        User user = User.builder().id(1L).name("Student").email("student@example.com")
                .password("encoded").role(Role.STUDENT).profileStatus(StudentProfileStatus.VERIFIED).build();
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(principal, Role.STUDENT, "Student")).thenReturn("jwt");

        var response = service.login(new LoginRequest("student@example.com", "password"));

        assertEquals("jwt", response.token());
        assertEquals("student@example.com", response.user().email());
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void registersStudentWithDecodedPhotoAndPendingProfile() {
        RegisterRequest request = registration(false, null, "data:image/jpeg;base64,aGVsbG8=");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(jwtUtil.generateToken(any(UserPrincipal.class), org.mockito.ArgumentMatchers.eq(Role.STUDENT),
                org.mockito.ArgumentMatchers.eq("New Student"))).thenReturn("jwt");

        var response = service.register(request);

        assertEquals("jwt", response.token());
        assertEquals(Role.STUDENT, response.user().role());
        assertEquals(StudentProfileStatus.PENDING_REVIEW, response.user().profileStatus());
        assertNull(response.user().className());
        org.mockito.ArgumentCaptor<User> savedUser = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        User user = savedUser.getValue();
        assertEquals("encoded", user.getPassword());
        assertEquals("Beginner", user.getRequestedClassName());
        assertNull(user.getFideId());
        assertEquals("image/jpeg", user.getStudentPhotoContentType());
        assertArrayEquals("hello".getBytes(), user.getStudentPhoto());
    }

    @Test
    void registersRatedStudentWithFideId() {
        RegisterRequest request = registration(true, "87654321", "data:image/png;base64,aGVsbG8=");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(jwtUtil.generateToken(any(UserPrincipal.class), org.mockito.ArgumentMatchers.eq(Role.STUDENT),
                org.mockito.ArgumentMatchers.eq("New Student"))).thenReturn("jwt");

        var response = service.register(request);

        assertEquals(true, response.user().rated());
        assertEquals("87654321", response.user().fideId());
    }

    @Test
    void registrationRejectsDuplicateEmailAndRatedStudentWithoutFideId() {
        RegisterRequest duplicate = registration(false, null, "data:image/png;base64,aGVsbG8=");
        when(userRepository.existsByEmail(duplicate.email())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.register(duplicate));

        RegisterRequest missingFideId = registration(true, " ", "data:image/png;base64,aGVsbG8=");
        when(userRepository.existsByEmail(missingFideId.email())).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.register(missingFideId));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(registration(true, null, "data:image/png;base64,aGVsbG8=")));
    }

    @Test
    void registrationRejectsUnsupportedMalformedEmptyAndOversizedPhotos() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> service.register(registration(false, null, "data:image/gif;base64,aGVsbG8=")));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(registration(false, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(registration(false, null, "data:image/png;base64,not-base64!")));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(registration(false, null, "data:image/webp;base64,")));
        String oversized = "A".repeat(5 * 1024 * 1024 + 1);
        String oversizedBase64 = java.util.Base64.getEncoder().encodeToString(oversized.getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> service.register(registration(false, null, "data:image/png;base64," + oversizedBase64)));
    }

    @Test
    void currentUserRequiresAuthenticatedPrincipalAndExistingUser() {
        assertThrows(ResourceNotFoundException.class, service::getCurrentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null));
        assertThrows(ResourceNotFoundException.class, service::getCurrentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null));
        assertThrows(ResourceNotFoundException.class, service::getCurrentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("missing@example.com", null, java.util.List.of()));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, service::getCurrentUser);
    }

    private RegisterRequest registration(boolean rated, String fideId, String photo) {
        return new RegisterRequest("New Student", "new@example.com", "password", 12, "Beginner", "Coach",
                "123", "Guardian", rated, fideId, photo);
    }
}
