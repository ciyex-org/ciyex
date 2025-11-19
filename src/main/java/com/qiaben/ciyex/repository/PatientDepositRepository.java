package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientDepositRepository extends JpaRepository<PatientDeposit, Long> {
    List<PatientDeposit> findByPatientIdOrderByDepositDateDesc(Long patientId);
    Optional<PatientDeposit> findByIdAndPatientId(Long id, Long patientId);
}
