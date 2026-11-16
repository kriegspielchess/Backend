package com.academy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.dto.AdminStudentPhoto;
import com.academy.dto.AdminStudentReviewDto;
import com.academy.entity.Course;
import com.academy.entity.Role;
import com.academy.entity.StudentProfileStatus;
import com.academy.entity.User;
import com.academy.exception.ResourceNotFoundException;
import com.academy.repository.CourseRepository;
import com.academy.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStudentReviewService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public List<AdminStudentReviewDto> findPendingStudents() {
        return userRepository.findStudentReviewsByStatus(Role.STUDENT, StudentProfileStatus.PENDING_REVIEW);
    }

    @Transactional
    public AdminStudentReviewDto verifyStudent(Long studentId, Long courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only student profiles can be verified");
        }
        if (student.getProfileStatus() != StudentProfileStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("This student profile is no longer awaiting review");
        }

        Course approvedCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Approved class not found"));
        student.setClassName(approvedCourse.getTitle());
        student.setTeacherName(approvedCourse.getInstructor());
        student.setProfileStatus(StudentProfileStatus.VERIFIED);
        userRepository.save(student);
        return toDto(student);
    }

    public AdminStudentPhoto getStudentPhoto(Long studentId) {
        User student = userRepository.findById(studentId)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        if (student.getStudentPhoto() == null || student.getStudentPhotoContentType() == null) {
            throw new ResourceNotFoundException("Student photo not found");
        }
        return new AdminStudentPhoto(student.getStudentPhoto(), student.getStudentPhotoContentType(),
                "student-" + student.getId() + "-photo");
    }

    private AdminStudentReviewDto toDto(User student) {
        return new AdminStudentReviewDto(student.getId(), student.getName(), student.getEmail(), student.getAge(),
                student.getRequestedClassName(), student.getRequestedTeacherName(), student.getWhatsappNumber(),
                student.getGuardianName(), student.getRated(), student.getFideId(), student.getStudentPhotoContentType(),
                student.getProfileStatus(), student.getCreatedAt());
    }
}
