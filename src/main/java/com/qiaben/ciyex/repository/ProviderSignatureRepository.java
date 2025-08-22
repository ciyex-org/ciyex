package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ProviderSignature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderSignatureRepository extends JpaRepository<ProviderSignature, Long> {

    List<ProviderSignature> findByOrgIdAndPatientId(Long orgId, Long patientId);

    List<ProviderSignature> findByOrgIdAndPatientIdAndEncounterId(
            Long orgId, Long patientId, Long encounterId);

    Optional<ProviderSignature> findByOrgIdAndPatientIdAndEncounterIdAndId(
            Long orgId, Long patientId, Long encounterId, Long id);
}
