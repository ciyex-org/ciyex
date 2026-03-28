package org.ciyex.ehr.kiosk.repository;

import org.ciyex.ehr.kiosk.entity.KioskCheckin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface KioskCheckinRepository extends JpaRepository<KioskCheckin, Long> {
    Page<KioskCheckin> findByOrgAliasAndCheckInTimeAfter(String orgAlias, LocalDateTime after, Pageable pageable);
    List<KioskCheckin> findByOrgAliasAndPatientId(String orgAlias, Long patientId);

    @Query("SELECT COUNT(c) FROM KioskCheckin c WHERE c.orgAlias = :org AND c.checkInTime >= :start")
    long countTodayByOrgAlias(@Param("org") String orgAlias, @Param("start") LocalDateTime start);

    @Query("SELECT COUNT(c) FROM KioskCheckin c WHERE c.orgAlias = :org AND c.checkInTime >= :start AND c.demographicsUpdated = true")
    long countDemographicsUpdated(@Param("org") String orgAlias, @Param("start") LocalDateTime start);

    @Query("SELECT COUNT(c) FROM KioskCheckin c WHERE c.orgAlias = :org AND c.checkInTime >= :start AND c.consentSigned = true")
    long countConsentSigned(@Param("org") String orgAlias, @Param("start") LocalDateTime start);
}
