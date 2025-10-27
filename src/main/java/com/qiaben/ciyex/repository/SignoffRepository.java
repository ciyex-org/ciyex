//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Signoff;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface SignoffRepository extends JpaRepository<Signoff, Long> {
//    List<Signoff> findByPatientId(Long patientId);
//    List<Signoff> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//    Optional<Signoff> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Signoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SignoffRepository extends JpaRepository<Signoff, Long> {
    List<Signoff> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<Signoff> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
