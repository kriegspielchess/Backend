package com.academy.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.dto.StudentPaymentDto;
import com.academy.dto.StudentPaymentRequest;
import com.academy.service.StudentPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/payments")
@RequiredArgsConstructor
public class StudentPaymentController {

    private final StudentPaymentService studentPaymentService;

    @GetMapping
    public List<StudentPaymentDto> getMyPayments() {
        return studentPaymentService.findMyPayments();
    }

    @PostMapping
    public ResponseEntity<StudentPaymentDto> submit(@Valid @RequestBody StudentPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentPaymentService.submit(request));
    }
}
