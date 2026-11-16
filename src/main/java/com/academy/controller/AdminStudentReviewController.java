package com.academy.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.dto.AdminStudentPhoto;
import com.academy.dto.AdminStudentReviewDto;
import com.academy.dto.VerifyStudentRequest;
import com.academy.service.AdminStudentReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentReviewController {

    private final AdminStudentReviewService adminStudentReviewService;

    @GetMapping("/pending")
    public List<AdminStudentReviewDto> getPendingStudents() {
        return adminStudentReviewService.findPendingStudents();
    }

    @PatchMapping("/{studentId}/verify")
    public AdminStudentReviewDto verifyStudent(@PathVariable Long studentId,
                                               @Valid @RequestBody VerifyStudentRequest request) {
        return adminStudentReviewService.verifyStudent(studentId, request.courseId());
    }

    @GetMapping("/{studentId}/photo")
    public ResponseEntity<byte[]> getStudentPhoto(@PathVariable Long studentId) {
        AdminStudentPhoto photo = adminStudentReviewService.getStudentPhoto(studentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(photo.fileName(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.data());
    }
}
