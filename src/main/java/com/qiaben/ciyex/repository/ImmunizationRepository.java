package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Immunization;
import com.qiaben.ciyex.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {
    List<Immunization> findByPatientId(Long patientId);
    List<Immunization> findByOrgId(Long orgId);

//    interface InvoiceRepository extends JpaRepository<Invoice, Long> {
//        List<Invoice> findByOrgIdAndPatientId(Long orgId, Long patientId);
//        List<Invoice> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//        Optional<Invoice> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
//    }
}
