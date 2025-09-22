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
//    List<DateTimeFinalized> findByOrgIdAndPatientId(Long orgId, Long patientId);
//
//    List<DateTimeFinalized> findByOrgIdAndPatientIdAndEncounterId(
//            Long orgId, Long patientId, Long encounterId);
//
//    Optional<DateTimeFinalized> findByOrgIdAndPatientIdAndEncounterIdAndId(
//            Long orgId, Long patientId, Long encounterId, Long id);
//}



package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.DateTimeFinalized;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DateTimeFinalizedRepository extends JpaRepository<DateTimeFinalized, Long> {

    List<DateTimeFinalized> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);

    Optional<DateTimeFinalized> findByOrgIdAndPatientIdAndEncounterIdAndId(
            Long orgId, Long patientId, Long encounterId, Long id
    );
}
