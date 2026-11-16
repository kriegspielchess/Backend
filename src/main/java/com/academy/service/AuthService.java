package com.academy.service;

import java.util.Base64;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.dto.AuthResponse;
import com.academy.dto.LoginRequest;
import com.academy.dto.RegisterRequest;
import com.academy.dto.UserDto;
import com.academy.entity.Role;
import com.academy.entity.StudentProfileStatus;
import com.academy.entity.User;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.UserRepository;
import com.academy.security.JwtUtil;
import com.academy.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();
        String token = jwtUtil.generateToken(principal, user.getRole(), user.getName());
        return new AuthResponse(token, toDto(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if (request.rated() && (request.fideId() == null || request.fideId().isBlank())) {
            throw new IllegalArgumentException("FIDE ID is required for rated students");
        }

        StudentPhoto studentPhoto = decodeStudentPhoto(request.studentPhoto());
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.STUDENT)
                .active(true)
                .age(request.age())
                .requestedClassName(request.className())
                .requestedTeacherName(request.teacherName())
                .className(null)
                .teacherName(null)
                .profileStatus(StudentProfileStatus.PENDING_REVIEW)
                .whatsappNumber(request.whatsappNumber())
                .guardianName(request.guardianName())
                .rated(request.rated())
                .fideId(request.rated() ? request.fideId() : null)
                .studentPhotoContentType(studentPhoto.contentType())
                .studentPhoto(studentPhoto.data())
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(new UserPrincipal(user), user.getRole(), user.getName());
        return new AuthResponse(token, toDto(user));
    }

    public UserDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResourceNotFoundException("No authenticated user");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private StudentPhoto decodeStudentPhoto(String studentPhoto) {
        if (studentPhoto == null || !(studentPhoto.startsWith("data:image/png;base64,")
                || studentPhoto.startsWith("data:image/jpeg;base64,")
                || studentPhoto.startsWith("data:image/webp;base64,"))) {
            throw new IllegalArgumentException("Student photo must be a PNG, JPEG, or WebP image");
        }

        int separator = studentPhoto.indexOf(',');
        String contentType = studentPhoto.substring(5, studentPhoto.indexOf(';'));
        byte[] image;
        try {
            image = Base64.getDecoder().decode(studentPhoto.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Student photo is not valid base64 data");
        }
        if (image.length == 0 || image.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Student photo must be 5 MB or smaller");
        }
        return new StudentPhoto(image, contentType);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getAge(),
                user.getClassName(), user.getTeacherName(), user.getWhatsappNumber(), user.getGuardianName(),
                user.getRated(), user.getFideId(), user.getRequestedClassName(), user.getRequestedTeacherName(),
                user.getProfileStatus());
    }

    private record StudentPhoto(byte[] data, String contentType) {
    }
}
