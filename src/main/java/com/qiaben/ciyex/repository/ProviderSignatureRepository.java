//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.ProviderSignature;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface ProviderSignatureRepository extends JpaRepository<ProviderSignature, Long> {
//
//    List<ProviderSignature> findByPatientId(Long patientId);
//
//    List<ProviderSignature> findByPatientIdAndEncounterId(
//            Long patientId, Long encounterId);
//
//    Optional<ProviderSignature> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id);
//}




package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ProviderSignature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderSignatureRepository extends JpaRepository<ProviderSignature, Long> {
    List<ProviderSignature> findByPatientId(Long patientId);
    List<ProviderSignature> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<ProviderSignature> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
