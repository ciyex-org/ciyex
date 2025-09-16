//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Signoff;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface SignoffRepository extends JpaRepository<Signoff, Long> {
//    List<Signoff> findByOrgIdAndPatientId(Long orgId, Long patientId);
//    List<Signoff> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
//    Optional<Signoff> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
//}



package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Signoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SignoffRepository extends JpaRepository<Signoff, Long> {
    List<Signoff> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<Signoff> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
