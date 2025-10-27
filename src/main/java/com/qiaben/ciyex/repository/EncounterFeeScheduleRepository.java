



package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.EncounterFeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EncounterFeeScheduleRepository extends JpaRepository<EncounterFeeSchedule, Long> {

    List<EncounterFeeSchedule> findByPatientId(Long patientId);

    List<EncounterFeeSchedule> findByPatientIdAndEncounterId(
            Long patientId, Long encounterId);
}
