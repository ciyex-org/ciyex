package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientPaymentRepository extends JpaRepository<PatientPayment, Long> {

    /**
     * Find all payments belonging to a specific patient.
     */
    List<PatientPayment> findByPatientId(Long patientId);

    /**
     * Optional: find all payments by payment method (useful for analytics or filtering).
     */
    List<PatientPayment> findByPaymentMethod(String paymentMethod);
}
