//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.ProviderNote;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface ProviderNoteRepository extends JpaRepository<ProviderNote, Long> {
//    List<ProviderNote> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//    Optional<ProviderNote> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ProviderNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderNoteRepository extends JpaRepository<ProviderNote, Long> {
    List<ProviderNote> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<ProviderNote> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
