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

import com.academy.dto.AdminPaymentDto;
import com.academy.dto.AdminPaymentSlip;
import com.academy.dto.PaymentStatusUpdateRequest;
import com.academy.service.AdminPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping
    public List<AdminPaymentDto> getPayments() {
        return adminPaymentService.findAll();
    }

    @PatchMapping("/{paymentId}/status")
    public AdminPaymentDto updateStatus(@PathVariable Long paymentId,
                                        @Valid @RequestBody PaymentStatusUpdateRequest request) {
        return adminPaymentService.updateStatus(paymentId, request.status());
    }

    @GetMapping("/{paymentId}/slip")
    public ResponseEntity<byte[]> getPaymentSlip(@PathVariable Long paymentId) {
        AdminPaymentSlip slip = adminPaymentService.getSlip(paymentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(slip.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(slip.fileName(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(slip.data());
    }
}
