package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByExternalId(String externalId);

    @Query("SELECT p FROM Patient p WHERE p.orgId = :orgId")
    List<Patient> findAllByOrgId(Long orgId);

    // ✅ New method to get patient count for a specific org
    long countByOrgId(Long orgId);
}
