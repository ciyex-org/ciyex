package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.InvoiceCourtesyCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceCourtesyCreditRepository extends JpaRepository<InvoiceCourtesyCredit, Long> {

    /**
     * Find all active courtesy credits for a specific invoice
     */
    List<InvoiceCourtesyCredit> findByInvoiceIdAndIsActiveAndIsDeletedOrderByCreatedDateDesc(
        Long invoiceId, Boolean isActive, Boolean isDeleted);

    /**
     * Find all courtesy credits for a specific patient
     */
    List<InvoiceCourtesyCredit> findByPatientIdAndIsDeletedOrderByCreatedDateDesc(
        Long patientId, Boolean isDeleted);

    /**
     * Find a specific active courtesy credit for an invoice
     */
    Optional<InvoiceCourtesyCredit> findByInvoiceIdAndIsActiveAndIsDeleted(
        Long invoiceId, Boolean isActive, Boolean isDeleted);
    
    /**
     * Find all courtesy credits for a specific invoice (for statement generation)
     */
    List<InvoiceCourtesyCredit> findByInvoiceId(Long invoiceId);
    
    /**
     * Find all courtesy credits for a specific patient (for statement generation)
     */
    List<InvoiceCourtesyCredit> findByPatientId(Long patientId);

    /**
     * Find all courtesy credits for a specific invoice (including inactive)
     */
    List<InvoiceCourtesyCredit> findByInvoiceIdAndIsDeletedOrderByCreatedDateDesc(
        Long invoiceId, Boolean isDeleted);

    /**
     * Find by ID and patient ID for security
     */
    Optional<InvoiceCourtesyCredit> findByIdAndPatientIdAndIsDeleted(
        Long id, Long patientId, Boolean isDeleted);
}

