package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.EncounterFeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EncounterFeeScheduleRepository extends JpaRepository<EncounterFeeSchedule, Long> {

    List<EncounterFeeSchedule> findByOrgIdAndPatientId(Long orgId, Long patientId);

    List<EncounterFeeSchedule> findByOrgIdAndPatientIdAndEncounterId(
            Long orgId, Long patientId, Long encounterId);
}
