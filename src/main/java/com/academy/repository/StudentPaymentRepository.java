package com.academy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.academy.dto.AdminPaymentDto;
import com.academy.dto.StudentPaymentDto;
import com.academy.entity.StudentPayment;

@Repository
public interface StudentPaymentRepository extends JpaRepository<StudentPayment, Long> {

    @Query("""
            select new com.academy.dto.StudentPaymentDto(payment.id, payment.className, payment.paymentDate,
                    payment.paymentSlipFileName, payment.status, payment.submittedAt)
            from StudentPayment payment
            where payment.student.id = :studentId
            order by payment.submittedAt desc
            """)
    List<StudentPaymentDto> findDtosByStudentId(@Param("studentId") Long studentId);

    @Query("""
            select new com.academy.dto.AdminPaymentDto(payment.id, payment.student.name, payment.student.email,
                    payment.student.guardianName, payment.student.whatsappNumber, payment.className,
                    payment.paymentDate, payment.paymentSlipFileName, payment.paymentSlipContentType,
                    payment.status, payment.submittedAt)
            from StudentPayment payment
            order by payment.submittedAt desc
            """)
    List<AdminPaymentDto> findAdminDtos();

    @Query("""
            select new com.academy.dto.AdminPaymentDto(payment.id, payment.student.name, payment.student.email,
                    payment.student.guardianName, payment.student.whatsappNumber, payment.className,
                    payment.paymentDate, payment.paymentSlipFileName, payment.paymentSlipContentType,
                    payment.status, payment.submittedAt)
            from StudentPayment payment
            where payment.id = :paymentId
            """)
    Optional<AdminPaymentDto> findAdminDtoById(@Param("paymentId") Long paymentId);
}
