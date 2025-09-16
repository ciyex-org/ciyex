
package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.EncounterFeeSchedule;
import com.qiaben.ciyex.entity.EncounterFeeScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EncounterFeeScheduleEntryRepository extends JpaRepository<EncounterFeeScheduleEntry, Long> {

    List<EncounterFeeScheduleEntry> findBySchedule(EncounterFeeSchedule schedule);

    @Query("""
       select e from EncounterFeeScheduleEntry e
        where e.schedule.orgId = :orgId
          and e.schedule.patientId = :patientId
          and e.schedule.encounterId = :encounterId
          and (:codeType is null or e.codeType = :codeType)
          and (:active is null or e.active = :active)
          and (:q is null or :q = '' or
               lower(e.code) like lower(concat('%', :q, '%')) or
               lower(e.description) like lower(concat('%', :q, '%')) or
               lower(e.modifier) like lower(concat('%', :q, '%')))
        order by e.codeType, e.code, e.modifier
    """)
    List<EncounterFeeScheduleEntry> search(Long orgId, Long patientId, Long encounterId,
                                           String codeType, Boolean active, String q);
}
