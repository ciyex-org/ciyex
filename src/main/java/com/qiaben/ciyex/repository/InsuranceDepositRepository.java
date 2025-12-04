package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.InsuranceDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceDepositRepository extends JpaRepository<InsuranceDeposit, Long> {
    List<InsuranceDeposit> findByPatientIdOrderByDepositDateDesc(Long patientId);
    Optional<InsuranceDeposit> findByIdAndPatientId(Long id, Long patientId);
}
