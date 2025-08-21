package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Custom query to count the number of patients for a specific organization
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.orgId = :orgId")
    long countByOrgId(Long orgId);

    // Optionally, add other queries as needed
    List<Patient> findAllByOrgId(Long orgId);

    @Query("SELECT p FROM Patient p WHERE p.orgId = :orgId AND p.externalId = :externalId")
    Optional<Patient> findByExternalIdAndOrgId(Long orgId, String externalId);
}
