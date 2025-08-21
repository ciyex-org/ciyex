package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository // optional but nice
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByOrgIdAndPatientId(Long orgId, Long patientId);
    List<Invoice> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<Invoice> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
