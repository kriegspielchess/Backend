package com.academy.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.dto.AdminStudentPhoto;
import com.academy.dto.AdminStudentReviewDto;
import com.academy.entity.Course;
import com.academy.entity.Role;
import com.academy.entity.StudentProfileStatus;
import com.academy.entity.User;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.CourseRepository;
import com.academy.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminStudentReviewServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private AdminStudentReviewService service;

    @Test
    void listsPendingStudentReviews() {
        List<AdminStudentReviewDto> reviews = List.of();
        when(userRepository.findStudentReviewsByStatus(Role.STUDENT, StudentProfileStatus.PENDING_REVIEW))
                .thenReturn(reviews);

        assertSame(reviews, service.findPendingStudents());
    }

    @Test
    void verifiesPendingStudentAndMapsReviewFields() {
        Instant createdAt = Instant.now();
        User student = User.builder().id(7L).name("Student").email("student@example.com")
                .role(Role.STUDENT).age(12).requestedClassName("Beginner").requestedTeacherName("Coach")
                .whatsappNumber("123").guardianName("Guardian").rated(true).fideId("123456")
                .studentPhotoContentType("image/png").profileStatus(StudentProfileStatus.PENDING_REVIEW)
                .createdAt(createdAt).build();
        Course course = Course.builder().title("Advanced").instructor("Teacher").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(userRepository.save(student)).thenReturn(student);

        AdminStudentReviewDto result = service.verifyStudent(7L, 3L);

        assertEquals("Advanced", student.getClassName());
        assertEquals("Teacher", student.getTeacherName());
        assertEquals(StudentProfileStatus.VERIFIED, student.getProfileStatus());
        assertEquals(7L, result.id());
        assertEquals("student@example.com", result.email());
        assertEquals("Beginner", result.requestedClassName());
        assertEquals("Coach", result.requestedTeacherName());
        assertEquals("Guardian", result.guardianName());
        assertEquals("123456", result.fideId());
        assertEquals(createdAt, result.createdAt());
        verify(userRepository).save(student);
    }

    @Test
    void rejectsMissingNonStudentOrAlreadyReviewedProfiles() {
        User teacher = User.builder().role(Role.TEACHER).build();
        User verified = User.builder().role(Role.STUDENT).profileStatus(StudentProfileStatus.VERIFIED).build();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(3L)).thenReturn(Optional.of(verified));

        assertThrows(ResourceNotFoundException.class, () -> service.verifyStudent(1L, 9L));
        assertThrows(IllegalArgumentException.class, () -> service.verifyStudent(2L, 9L));
        assertThrows(IllegalArgumentException.class, () -> service.verifyStudent(3L, 9L));
    }

    @Test
    void rejectsMissingApprovedCourse() {
        User student = User.builder().role(Role.STUDENT).profileStatus(StudentProfileStatus.PENDING_REVIEW).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.verifyStudent(1L, 9L));
    }

    @Test
    void retrievesPhotoOnlyForStudentWithPhotoAndContentType() {
        byte[] photo = {4, 5};
        User student = User.builder().id(8L).role(Role.STUDENT).studentPhoto(photo)
                .studentPhotoContentType("image/webp").build();
        User teacher = User.builder().role(Role.TEACHER).studentPhoto(photo)
                .studentPhotoContentType("image/webp").build();
        User noContentType = User.builder().role(Role.STUDENT).studentPhoto(photo).build();
        User noPhoto = User.builder().role(Role.STUDENT).studentPhotoContentType("image/png").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(3L)).thenReturn(Optional.of(noContentType));
        when(userRepository.findById(4L)).thenReturn(Optional.of(noPhoto));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        AdminStudentPhoto result = service.getStudentPhoto(1L);

        assertArrayEquals(photo, result.data());
        assertEquals("image/webp", result.contentType());
        assertEquals("student-8-photo", result.fileName());
        assertThrows(ResourceNotFoundException.class, () -> service.getStudentPhoto(2L));
        assertThrows(ResourceNotFoundException.class, () -> service.getStudentPhoto(3L));
        assertThrows(ResourceNotFoundException.class, () -> service.getStudentPhoto(4L));
        assertThrows(ResourceNotFoundException.class, () -> service.getStudentPhoto(5L));
    }
}
