package com.academy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.dto.AdminPaymentDto;
import com.academy.dto.AdminPaymentSlip;
import com.academy.entity.PaymentStatus;
import com.academy.entity.StudentPayment;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.StudentPaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final StudentPaymentRepository paymentRepository;

    public List<AdminPaymentDto> findAll() {
        return paymentRepository.findAdminDtos();
    }

    @Transactional
    public AdminPaymentDto updateStatus(Long paymentId, PaymentStatus status) {
        if (status != PaymentStatus.CHECKED && status != PaymentStatus.VERIFIED) {
            throw new IllegalArgumentException("Payment status must be CHECKED or VERIFIED");
        }

        StudentPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() == PaymentStatus.APPROVED || payment.getStatus() == PaymentStatus.REJECTED
                || (payment.getStatus() == PaymentStatus.VERIFIED && status == PaymentStatus.CHECKED)) {
            throw new IllegalArgumentException("This payment cannot move to the requested review status");
        }

        payment.setStatus(status);
        return paymentRepository.findAdminDtoById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    public AdminPaymentSlip getSlip(Long paymentId) {
        StudentPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return new AdminPaymentSlip(payment.getPaymentSlip(), payment.getPaymentSlipContentType(), payment.getPaymentSlipFileName());
    }
}
