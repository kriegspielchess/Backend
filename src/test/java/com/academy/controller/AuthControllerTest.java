package com.academy.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.academy.entity.Role;
import com.academy.entity.StudentProfileStatus;
import com.academy.entity.User;
import com.academy.repository.UserRepository;

@SpringBootTest(properties = "app.mail-from=test@kriegspiel.com")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender mailSender;

    private String login(String email) throws Exception {
        return login(email, "password");
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void loginStudentWorks() throws Exception {
        login("student@kriegspiel.com");
    }

    @Test
    void studentRegistrationSavesEnrollmentDetailsAndPhoto() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Chess Student","email":"signup-test@kriegspiel.com","password":"secret123",
                                "age":12,"className":"Beginner","teacherName":"Nathasha Sanali",
                                "whatsappNumber":"+15550000000","guardianName":"Parent Name","rated":true,
                                "fideId":"12345678","studentPhoto":"data:image/png;base64,aGVsbG8="}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.profileStatus").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.user.requestedClassName").value("Beginner"));

        User student = userRepository.findByEmail("signup-test@kriegspiel.com").orElseThrow();
        assertEquals(12, student.getAge());
        assertNull(student.getClassName());
        assertNull(student.getTeacherName());
        assertEquals("Beginner", student.getRequestedClassName());
        assertEquals("Nathasha Sanali", student.getRequestedTeacherName());
        assertEquals(StudentProfileStatus.PENDING_REVIEW, student.getProfileStatus());
        assertEquals("image/png", student.getStudentPhotoContentType());
        assertEquals("Parent Name", student.getGuardianName());
        assertEquals("12345678", student.getFideId());
        assertArrayEquals("hello".getBytes(), student.getStudentPhoto());
    }

    @Test
    void passwordResetEmailUpdatesPasswordAndTokenCanOnlyBeUsedOnce() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@kriegspiel.com";
        userRepository.save(User.builder()
                .name("Reset Test Student")
                .email(email)
                .password(passwordEncoder.encode("before123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String resetLink = messageCaptor.getValue().getText().lines()
                .filter(line -> line.contains("/reset-password?token="))
                .findFirst()
                .orElseThrow();
        String token = resetLink.substring(resetLink.indexOf("token=") + 6);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"after123\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"after123\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"another123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointRejectsStudent() throws Exception {
        String token = extractToken(login("student@kriegspiel.com"));
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointAcceptsAdmin() throws Exception {
        String token = extractToken(login("admin@kriegspiel.com", "admin123"));
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin access granted"));
    }

    private String extractToken(String response) {
        int start = response.indexOf("\"token\":\"") + 9;
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }
}
