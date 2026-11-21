package com.academy.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

@ExtendWith(MockitoExtension.class)
class StudentPaymentServiceTest {

    @Mock
    private StudentPaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private StudentPaymentService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsPaymentsAndMapsCheckedToVerifiedForStudents() {
        authenticate("student@example.com");
        User student = student();
        StudentPaymentDto checked = dto(PaymentStatus.CHECKED);
        StudentPaymentDto pending = dto(PaymentStatus.PENDING_REVIEW);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(paymentRepository.findDtosByStudentId(4L)).thenReturn(List.of(checked, pending));

        List<StudentPaymentDto> result = service.findMyPayments();

        assertEquals(PaymentStatus.VERIFIED, result.get(0).status());
        assertEquals(PaymentStatus.PENDING_REVIEW, result.get(1).status());
        assertSame(pending, result.get(1));
    }

    @Test
    void submitSavesPaymentAndSupportsEachAllowedSlipType() {
        authenticate("student@example.com");
        User student = student();
        Course course = Course.builder().id(2L).title("Beginner").build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(paymentRepository.save(any(StudentPayment.class))).thenAnswer(invocation -> {
            StudentPayment payment = invocation.getArgument(0);
            payment.setId(9L);
            return payment;
        });
        List<String> dataUrls = List.of(
                "data:image/png;base64,aGVsbG8=",
                "data:image/jpeg;base64,aGVsbG8=",
                "data:image/webp;base64,aGVsbG8=",
                "data:application/pdf;base64,aGVsbG8=");
        List<String> contentTypes = List.of("image/png", "image/jpeg", "image/webp", "application/pdf");

        for (String dataUrl : dataUrls) {
            StudentPaymentDto result = service.submit(request(dataUrl));
            assertEquals(9L, result.id());
            assertEquals(PaymentStatus.PENDING_REVIEW, result.status());
        }
        ArgumentCaptor<StudentPayment> saved = ArgumentCaptor.forClass(StudentPayment.class);
        verify(paymentRepository, org.mockito.Mockito.times(dataUrls.size())).save(saved.capture());
        for (int index = 0; index < dataUrls.size(); index++) {
            StudentPayment payment = saved.getAllValues().get(index);
            assertSame(student, payment.getStudent());
            assertEquals("Beginner", payment.getClassName());
            assertEquals(contentTypes.get(index), payment.getPaymentSlipContentType());
            assertArrayEquals("hello".getBytes(), payment.getPaymentSlip());
        }
    }

    @Test
    void submitRejectsFutureDateUnverifiedStudentAndMissingOrWrongClass() {
        authenticate("student@example.com");
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student()));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(new StudentPaymentRequest(2L, LocalDate.now().plusDays(1),
                        "data:image/png;base64,aGVsbG8=", "receipt.png")));

        User pending = student();
        pending.setProfileStatus(StudentProfileStatus.PENDING_REVIEW);
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pending));
        authenticate("pending@example.com");
        assertThrows(IllegalArgumentException.class, () -> service.submit(request("data:image/png;base64,aGVsbG8=")));

        authenticate("student@example.com");
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.submit(new StudentPaymentRequest(99L, LocalDate.now(),
                        "data:image/png;base64,aGVsbG8=", "receipt.png")));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(Course.builder().title("Advanced").build()));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(new StudentPaymentRequest(3L, LocalDate.now(),
                        "data:image/png;base64,aGVsbG8=", "receipt.png")));
    }

    @Test
    void submitRejectsUnsupportedMalformedEmptyAndOversizedSlips() {
        authenticate("student@example.com");
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student()));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(Course.builder().title("Beginner").build()));

        assertThrows(IllegalArgumentException.class, () -> service.submit(request("not-a-data-url")));
        assertThrows(IllegalArgumentException.class, () -> service.submit(request("data:image/gif;base64,aGVsbG8=")));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(request("data:image/png;base64,invalid!")));
        assertThrows(IllegalArgumentException.class, () -> service.submit(request("data:image/png;base64,")));
        String oversizedPayload = java.util.Base64.getEncoder()
                .encodeToString(new byte[5 * 1024 * 1024 + 1]);
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(request("data:image/png;base64," + oversizedPayload)));
    }

    @Test
    void currentStudentRequiresAnAuthenticatedExistingUser() {
        assertThrows(ResourceNotFoundException.class, service::findMyPayments);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null));
        assertThrows(ResourceNotFoundException.class, service::findMyPayments);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null));
        assertThrows(ResourceNotFoundException.class, service::findMyPayments);
        authenticate("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, service::findMyPayments);
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    private User student() {
        return User.builder().id(4L).email("student@example.com").className("Beginner")
                .profileStatus(StudentProfileStatus.VERIFIED).build();
    }

    private StudentPaymentRequest request(String dataUrl) {
        return new StudentPaymentRequest(2L, LocalDate.now(), dataUrl, "receipt.png");
    }

    private StudentPaymentDto dto(PaymentStatus status) {
        return new StudentPaymentDto(1L, "Beginner", LocalDate.now(), "receipt.png", status, null);
    }
}
