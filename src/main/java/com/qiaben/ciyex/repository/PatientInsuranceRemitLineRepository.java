//package com.qiaben.ciyex.repository;
//
//
//
//import com.qiaben.ciyex.entity.PatientInsuranceRemitLine;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//public interface PatientInsuranceRemitLineRepository extends JpaRepository<PatientInsuranceRemitLine, Long> {}

package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.PatientInsuranceRemitLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientInsuranceRemitLineRepository extends JpaRepository<PatientInsuranceRemitLine, Long> {

    /* Preferred (ordered) lookups */
    List<PatientInsuranceRemitLine> findAllByOrgIdAndPatientIdOrderByIdDesc(Long orgId, Long patientId);
    List<PatientInsuranceRemitLine> findAllByOrgIdAndPatientIdAndInvoiceIdOrderByIdDesc(Long orgId, Long patientId, Long invoiceId);

    /* Fallbacks for older code paths without explicit ordering */
    List<PatientInsuranceRemitLine> findAllByOrgIdAndPatientId(Long orgId, Long patientId);
    List<PatientInsuranceRemitLine> findAllByOrgIdAndPatientIdAndInvoiceId(Long orgId, Long patientId, Long invoiceId);

    /* Legacy fallback if org filter is unavailable */
    List<PatientInsuranceRemitLine> findAllByPatientId(Long patientId);
}
