package com.academy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.academy.dto.AdminStudentReviewDto;
import com.academy.entity.Role;
import com.academy.entity.StudentProfileStatus;
import com.academy.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select new com.academy.dto.AdminStudentReviewDto(student.id, student.name, student.email, student.age,
                    student.requestedClassName, student.requestedTeacherName, student.whatsappNumber,
                    student.guardianName, student.rated, student.fideId, student.studentPhotoContentType,
                    student.profileStatus, student.createdAt)
            from User student
            where student.role = :role and student.profileStatus = :status
            order by student.createdAt asc
            """)
    List<AdminStudentReviewDto> findStudentReviewsByStatus(@Param("role") Role role,
                                                           @Param("status") StudentProfileStatus status);
}
