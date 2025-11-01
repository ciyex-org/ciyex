//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.DateTimeFinalized;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface DateTimeFinalizedRepository extends JpaRepository<DateTimeFinalized, Long> {
//
//    List<DateTimeFinalized> findByPatientId(Long patientId);
//
//    List<DateTimeFinalized> findByPatientIdAndEncounterId(
//            Long patientId, Long encounterId);
//
//    Optional<DateTimeFinalized> findByPatientIdAndEncounterIdAndId(
//            Long patientId, Long encounterId, Long id);
//}



package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.DateTimeFinalized;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DateTimeFinalizedRepository extends JpaRepository<DateTimeFinalized, Long> {
    List<DateTimeFinalized> findByPatientId(Long patientId);

    List<DateTimeFinalized> findByPatientIdAndEncounterId(Long patientId, Long encounterId);

    Optional<DateTimeFinalized> findByPatientIdAndEncounterIdAndId(
            Long patientId, Long encounterId, Long id
    );
}
