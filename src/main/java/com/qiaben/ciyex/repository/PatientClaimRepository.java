//package com.qiaben.ciyex.repository;
//
//
//
//import com.qiaben.ciyex.entity.PatientClaim;
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
//
//public interface PatientClaimRepository extends JpaRepository<PatientClaim, Long> {
//    Optional<PatientClaim> findByInvoiceIdAndOrgIdAndPatientId(Long invoiceId, Long orgId, Long patientId);
//}



package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientClaim;
import com.qiaben.ciyex.entity.PatientInsuranceRemitLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientClaimRepository extends JpaRepository<PatientClaim, Long> {

    /** Current/active claim lookup (tenant-scoped) */
    Optional<PatientClaim> findByInvoiceIdAndOrgIdAndPatientId(Long invoiceId, Long orgId, Long patientId);

    /** Legacy fallback: single claim per invoice (no tenant/patient scope) */
    PatientClaim findByInvoiceId(Long invoiceId);

    /** All claims for a patient (new) */
    List<PatientClaim> findAllByOrgIdAndPatientIdOrderByIdDesc(Long orgId, Long patientId);
    List<PatientClaim> findAllByOrgIdAndPatientId(Long orgId, Long patientId);

    /** All (historical) claims for a specific invoice (new) */
    List<PatientClaim> findAllByInvoiceIdAndOrgIdAndPatientIdOrderByIdDesc(Long invoiceId, Long orgId, Long patientId);
    List<PatientClaim> findAllByInvoiceIdAndOrgIdAndPatientId(Long invoiceId, Long orgId, Long patientId);


//    List<PatientClaim> findAllByIdAndOrgIdAndPatientId(Long claimId, Long orgId, Long patientId);





}
