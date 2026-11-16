package com.academy.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.academy.entity.Course;
import com.academy.entity.PaymentStatus;
import com.academy.entity.Role;
import com.academy.entity.StudentPayment;
import com.academy.entity.User;
import com.academy.repository.CourseRepository;
import com.academy.repository.StudentPaymentRepository;
import com.academy.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentPaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanReviewPaymentAndCheckedAppearsVerifiedToStudent() throws Exception {
        String email = "review-" + UUID.randomUUID() + "@kriegspiel.com";
        Course course = courseRepository.findAll().get(0);
        User student = userRepository.save(User.builder()
                .name("Payment Review Student")
                .email(email)
                .password(passwordEncoder.encode("student123"))
                .role(Role.STUDENT)
                .active(true)
                .className(course.getTitle())
                .build());
        StudentPayment payment = paymentRepository.save(StudentPayment.builder()
                .student(student)
                .className(course.getTitle())
                .paymentDate(LocalDate.now())
                .paymentSlipFileName("receipt.png")
                .paymentSlipContentType("image/png")
                .paymentSlip("receipt".getBytes())
                .status(PaymentStatus.PENDING_REVIEW)
                .build());

        String adminToken = login("admin@kriegspiel.com", "admin123");
        MvcResult listResult = mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(listResult.getResponse().getContentAsString().contains(email));

        mockMvc.perform(get("/api/admin/payments/" + payment.getId() + "/slip")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        mockMvc.perform(patch("/api/admin/payments/" + payment.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CHECKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED"));

        String studentToken = login(email, "student123");
        mockMvc.perform(get("/api/student/payments")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("VERIFIED"));

        mockMvc.perform(patch("/api/admin/payments/" + payment.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        int start = response.indexOf("\"token\":\"") + 9;
        int end = response.indexOf('"', start);
        return response.substring(start, end);
    }
}
