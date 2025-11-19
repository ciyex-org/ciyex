//package com.qiaben.ciyex.repository;
//
//
//
//import com.qiaben.ciyex.entity.PatientClaim;
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
//
//public interface PatientClaimRepository extends JpaRepository<PatientClaim, Long> {
//    Optional<PatientClaim> findByInvoiceIdAndPatientId(Long invoiceId, Long patientId);
//}


package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;



public interface PatientClaimRepository extends JpaRepository<PatientClaim, Long> {

    /** Current/active claim lookup (tenant-scoped) */
    Optional<PatientClaim> findByInvoiceIdAndPatientId(Long invoiceId, Long patientId);

    /** Legacy fallback: single claim per invoice (no tenant/patient scope) */
    PatientClaim findByInvoiceId(Long invoiceId);


    /** All claims for a patient (new) */
    List<PatientClaim> findAllByPatientIdOrderByIdDesc(Long patientId);
    List<PatientClaim> findAllByPatientId(Long patientId);

    /** All claims for all patients in the org (for All Claims view) */
    List<PatientClaim> findAllByOrderByIdDesc();

    /** All (historical) claims for a specific invoice (new) */
    List<PatientClaim> findAllByInvoiceIdAndPatientIdOrderByIdDesc(Long invoiceId, Long patientId);
    
    /** All claims for a specific invoice (for statement generation) */
    List<PatientClaim> findByInvoiceIdOrderByIdAsc(Long invoiceId);





}
