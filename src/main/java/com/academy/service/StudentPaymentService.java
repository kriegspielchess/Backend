package com.academy.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.dto.StudentPaymentDto;
import com.academy.dto.StudentPaymentRequest;
import com.academy.entity.Course;
import com.academy.entity.PaymentStatus;
import com.academy.entity.StudentPayment;
import com.academy.entity.StudentProfileStatus;
import com.academy.entity.User;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.CourseRepository;
import com.academy.repository.StudentPaymentRepository;
import com.academy.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPaymentService {

    private static final int MAX_SLIP_SIZE = 5 * 1024 * 1024;

    private final StudentPaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public List<StudentPaymentDto> findMyPayments() {
        User student = currentStudent();
        return paymentRepository.findDtosByStudentId(student.getId()).stream()
                .map(this::toStudentDto)
                .toList();
    }

    @Transactional
    public StudentPaymentDto submit(StudentPaymentRequest request) {
        if (request.paymentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }

        User student = currentStudent();
        if (student.getProfileStatus() != StudentProfileStatus.VERIFIED) {
            throw new IllegalArgumentException("Your student profile must be verified before making a payment");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!course.getTitle().equals(student.getClassName())) {
            throw new IllegalArgumentException("Payments can only be submitted for your registered class");
        }
        PaymentSlip slip = decodePaymentSlip(request.paymentSlipDataUrl());
        StudentPayment payment = StudentPayment.builder()
                .student(student)
                .className(course.getTitle())
                .paymentDate(request.paymentDate())
                .paymentSlipFileName(request.paymentSlipFileName())
                .paymentSlipContentType(slip.contentType())
                .paymentSlip(slip.data())
                .status(PaymentStatus.PENDING_REVIEW)
                .build();
        return toDto(paymentRepository.save(payment));
    }

    private User currentStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResourceNotFoundException("No authenticated student");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private PaymentSlip decodePaymentSlip(String dataUrl) {
        if (dataUrl.length() > ((MAX_SLIP_SIZE + 2) / 3 * 4) + 64) {
            throw new IllegalArgumentException("Payment slip must be 5 MB or smaller");
        }
        int separator = dataUrl.indexOf(',');
        if (separator < 0) {
            throw new IllegalArgumentException("Payment slip must be a PNG, JPEG, WebP, or PDF file");
        }

        String contentType = switch (dataUrl.substring(0, separator)) {
            case "data:image/png;base64" -> "image/png";
            case "data:image/jpeg;base64" -> "image/jpeg";
            case "data:image/webp;base64" -> "image/webp";
            case "data:application/pdf;base64" -> "application/pdf";
            default -> throw new IllegalArgumentException("Payment slip must be a PNG, JPEG, WebP, or PDF file");
        };

        byte[] data;
        try {
            data = Base64.getDecoder().decode(dataUrl.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Payment slip data is invalid");
        }
        if (data.length == 0 || data.length > MAX_SLIP_SIZE) {
            throw new IllegalArgumentException("Payment slip must be 5 MB or smaller");
        }
        return new PaymentSlip(data, contentType);
    }

    private StudentPaymentDto toDto(StudentPayment payment) {
        return new StudentPaymentDto(payment.getId(), payment.getClassName(), payment.getPaymentDate(),
                payment.getPaymentSlipFileName(), payment.getStatus(), payment.getSubmittedAt());
    }

    private StudentPaymentDto toStudentDto(StudentPaymentDto payment) {
        if (payment.status() != PaymentStatus.CHECKED) return payment;
        return new StudentPaymentDto(payment.id(), payment.className(), payment.paymentDate(),
                payment.paymentSlipFileName(), PaymentStatus.VERIFIED, payment.submittedAt());
    }

    private record PaymentSlip(byte[] data, String contentType) {
    }
}
