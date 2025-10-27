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
    List<PatientInsuranceRemitLine> findAllByPatientIdOrderByIdDesc(Long patientId);
    List<PatientInsuranceRemitLine> findAllByPatientIdAndInvoiceIdOrderByIdDesc(Long patientId, Long invoiceId);

    /* Fallbacks for older code paths without explicit ordering */
    List<PatientInsuranceRemitLine> findAllByPatientId(Long patientId);
    List<PatientInsuranceRemitLine> findAllByPatientIdAndInvoiceId(Long patientId, Long invoiceId);
}
