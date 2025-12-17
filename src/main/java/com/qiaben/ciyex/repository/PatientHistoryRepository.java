package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientHistoryRepository extends JpaRepository<PatientHistory, Long> {
    Optional<PatientHistory> findByPatientId(Long patientId);
}