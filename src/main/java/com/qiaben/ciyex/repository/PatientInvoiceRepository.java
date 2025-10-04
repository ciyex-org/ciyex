package com.qiaben.ciyex.repository;



import com.qiaben.ciyex.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PatientInvoiceRepository extends JpaRepository<PatientInvoice, Long> {
    List<PatientInvoice> findByOrgIdAndPatientIdOrderByIdDesc(Long orgId, Long patientId);
    Optional<PatientInvoice> findByIdAndOrgIdAndPatientId(Long id, Long orgId, Long patientId);
}
