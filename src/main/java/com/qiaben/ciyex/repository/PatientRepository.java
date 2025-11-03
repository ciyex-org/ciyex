package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // ✅ Count the number of patients for a specific organization
    @Query("SELECT COUNT(p) FROM Patient p ")
    long count();

    // ✅ Enhanced search within an org (supports MRN + Gender + contact info)
    @Query("SELECT p FROM Patient p WHERE (" +
            "LOWER(p.firstName) LIKE %:search% OR " +
            "LOWER(p.lastName) LIKE %:search% OR " +
            "LOWER(p.email) LIKE %:search% OR " +
            "LOWER(p.phoneNumber) LIKE %:search% OR " +
            "LOWER(p.medicalRecordNumber) LIKE %:search% OR " +
            "LOWER(p.gender) LIKE %:search%)")
    Page<Patient> searchBy(String search, Pageable pageable);

    // ✅ Fallback: findAll for a specific organization (paginated)
    Page<Patient> findAll(Pageable pageable);

    // ✅ Lookup patient by externalId + orgId
    @Query("SELECT p FROM Patient p WHERE p.externalId = :externalId")
    Optional<Patient> findByExternalId(String externalId);
}
