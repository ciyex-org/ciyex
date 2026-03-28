package org.ciyex.ehr.cds.repository;

import org.ciyex.ehr.cds.entity.CdsAlertLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CdsAlertLogRepository extends JpaRepository<CdsAlertLog, Long> {
    Page<CdsAlertLog> findByOrgAlias(String orgAlias, Pageable pageable);
    List<CdsAlertLog> findByOrgAliasAndPatientId(String orgAlias, Long patientId);
    long countByOrgAliasAndActionTaken(String orgAlias, String actionTaken);
    long countByOrgAlias(String orgAlias);
    long countByOrgAliasAndCreatedAtAfter(String orgAlias, LocalDateTime since);
    long countByOrgAliasAndSeverity(String orgAlias, String severity);
}
