//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface HistoryOfPresentIllnessRepository extends JpaRepository<HistoryOfPresentIllness, Long> {
//
//    List<HistoryOfPresentIllness> findByPatientId(Long patientId);
//
//    List<HistoryOfPresentIllness> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
//
//    Optional<HistoryOfPresentIllness> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id
//    );
//}





package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryOfPresentIllnessRepository extends JpaRepository<HistoryOfPresentIllness, Long> {
    List<HistoryOfPresentIllness> findByPatientId(Long patientId);

    List<HistoryOfPresentIllness> findByPatientIdAndEncounterId(Long patientId, Long encounterId);

    Optional<HistoryOfPresentIllness> findByPatientIdAndEncounterIdAndId(
            Long patientId, Long encounterId, Long id
    );
}
