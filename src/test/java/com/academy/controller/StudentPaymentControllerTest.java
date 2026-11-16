package com.academy.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.academy.entity.Course;
import com.academy.entity.User;
import com.academy.repository.CourseRepository;
import com.academy.repository.StudentPaymentRepository;
import com.academy.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class StudentPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentPaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void studentCanSubmitAndReviewTheirPaymentSlip() throws Exception {
        String token = loginAsStudent();
        Course course = courseRepository.findAll().get(0);
        User student = userRepository.findByEmail("student@kriegspiel.com").orElseThrow();
        student.setClassName(course.getTitle());
        userRepository.save(student);
        String paymentDate = LocalDate.now().toString();

        mockMvc.perform(post("/api/student/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":%d,"paymentDate":"%s","paymentSlipFileName":"transfer.png",
                                "paymentSlipDataUrl":"data:image/png;base64,aGVsbG8="}
                                """.formatted(course.getId(), paymentDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.className").value(course.getTitle()))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        mockMvc.perform(get("/api/student/payments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentSlipFileName").value("transfer.png"));

        var savedPayment = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStudent().getId().equals(student.getId())
                        && payment.getPaymentSlipFileName().equals("transfer.png"))
                .findFirst()
                .orElseThrow();
        assertArrayEquals("hello".getBytes(), savedPayment.getPaymentSlip());

        Course otherCourse = courseRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(course.getId()))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(post("/api/student/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":%d,"paymentDate":"%s","paymentSlipFileName":"other.png",
                                "paymentSlipDataUrl":"data:image/png;base64,aGVsbG8="}
                                """.formatted(otherCourse.getId(), paymentDate)))
                .andExpect(status().isBadRequest());
    }

    private String loginAsStudent() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@kriegspiel.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        int start = response.indexOf("\"token\":\"") + 9;
        int end = response.indexOf('"', start);
        return response.substring(start, end);
    }
}
