package com.qiaben.ciyex.repository;



import com.qiaben.ciyex.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PatientInvoiceRepository extends JpaRepository<PatientInvoice, Long> {
    List<PatientInvoice> findByPatientIdOrderByIdDesc(Long patientId);
    Optional<PatientInvoice> findByIdAndPatientId(Long id, Long patientId);
}
