package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Custom query to count the number of patients for a specific organization
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.orgId = :orgId")
    long countByOrgId(Long orgId);

    // Search within an org (server-side search + pagination)
    @Query("SELECT p FROM Patient p WHERE p.orgId = :orgId AND (" +
            "LOWER(p.firstName) LIKE %:search% OR " +
            "LOWER(p.lastName) LIKE %:search% OR " +
            "LOWER(p.email) LIKE %:search% OR " +
            "LOWER(p.phoneNumber) LIKE %:search%)")
    Page<Patient> searchByOrgId(String search, Long orgId, Pageable pageable);

    // Fallback: findAll for a specific organization (paginated)
    Page<Patient> findAllByOrgId(Long orgId, Pageable pageable);

    // Non-paginated helper
    List<Patient> findAllByOrgId(Long orgId);
}