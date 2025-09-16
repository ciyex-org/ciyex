package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientEducationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientEducationAssignmentRepository extends JpaRepository<PatientEducationAssignment, Long> {
    List<PatientEducationAssignment> findByPatientId(Long patientId);
}
