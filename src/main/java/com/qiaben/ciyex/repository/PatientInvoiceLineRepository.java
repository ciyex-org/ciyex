package com.qiaben.ciyex.repository;



import com.qiaben.ciyex.entity.PatientInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientInvoiceLineRepository extends JpaRepository<PatientInvoiceLine, Long> {
    List<PatientInvoiceLine> findByInvoiceId(Long invoiceId);
}
