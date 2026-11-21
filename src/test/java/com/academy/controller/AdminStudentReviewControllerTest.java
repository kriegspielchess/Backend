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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.academy.entity.Course;
import com.academy.repository.CourseRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminStudentReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void adminVerifiesRegistrationAndAssignsApprovedClass() throws Exception {
        String email = "profile-review-" + UUID.randomUUID() + "@kriegspiel.com";
        Course requestedCourse = courseRepository.findAll().getFirst();
        Course approvedCourse = courseRepository.findAll().stream()
                .filter(course -> !course.getId().equals(requestedCourse.getId()))
                .findFirst()
                .orElseThrow();

        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Academy Student","email":"%s","password":"student123",
                                "age":11,"className":"%s","teacherName":"%s",
                                "whatsappNumber":"+15550000000","guardianName":"Student Guardian",
                                "rated":true,"fideId":"87654321","studentPhoto":"data:image/png;base64,aGVsbG8="}
                                """.formatted(email, requestedCourse.getTitle(), requestedCourse.getInstructor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.profileStatus").value("PENDING_REVIEW"))
                .andReturn();
        String studentToken = extractToken(registration.getResponse().getContentAsString());
        String adminToken = login();

        MvcResult pendingProfiles = mockMvc.perform(get("/api/admin/students/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String pendingJson = pendingProfiles.getResponse().getContentAsString();
        assertTrue(pendingJson.contains(email));
        assertTrue(pendingJson.contains(requestedCourse.getTitle()));

        long studentId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(registration.getResponse().getContentAsString()).path("user").path("id").asLong();
        mockMvc.perform(get("/api/admin/students/" + studentId + "/photo")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes("hello".getBytes()));

        mockMvc.perform(post("/api/student/payments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":%d,"paymentDate":"%s","paymentSlipFileName":"proof.png",
                                "paymentSlipDataUrl":"data:image/png;base64,aGVsbG8="}
                                """.formatted(requestedCourse.getId(), LocalDate.now())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/students/" + studentId + "/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":" + approvedCourse.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileStatus").value("VERIFIED"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value(approvedCourse.getTitle()))
                .andExpect(jsonPath("$.teacherName").value(approvedCourse.getInstructor()));

        mockMvc.perform(post("/api/student/payments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":%d,"paymentDate":"%s","paymentSlipFileName":"proof.png",
                                "paymentSlipDataUrl":"data:image/png;base64,aGVsbG8="}
                                """.formatted(approvedCourse.getId(), LocalDate.now())))
                .andExpect(status().isCreated());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + "admin@kriegspiel.com" + "\",\"password\":\"" + "admin123" + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return extractToken(result.getResponse().getContentAsString());
    }

    private String extractToken(String response) {
        int start = response.indexOf("\"token\":\"") + 9;
        int end = response.indexOf('"', start);
        return response.substring(start, end);
    }
}
