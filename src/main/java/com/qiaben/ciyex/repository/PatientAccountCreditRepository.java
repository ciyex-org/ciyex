package com.qiaben.ciyex.repository;



import com.qiaben.ciyex.entity.PatientAccountCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientAccountCreditRepository extends JpaRepository<PatientAccountCredit, Long> {
    Optional<PatientAccountCredit> findByPatientId(Long patientId);
}

