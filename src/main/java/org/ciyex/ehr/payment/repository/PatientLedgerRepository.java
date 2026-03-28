package org.ciyex.ehr.payment.repository;

import org.ciyex.ehr.payment.entity.PatientLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface PatientLedgerRepository extends JpaRepository<PatientLedger, Long> {

    List<PatientLedger> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    @Query("SELECT COALESCE(SUM(l.amount),0) FROM PatientLedger l WHERE l.orgAlias = :org AND l.patientId = :pid")
    BigDecimal sumByPatient(@Param("org") String orgAlias, @Param("pid") Long patientId);

    @Query("SELECT l FROM PatientLedger l WHERE l.orgAlias = :org AND l.patientId = :pid ORDER BY l.createdAt DESC LIMIT 1")
    PatientLedger findLatestByPatient(@Param("org") String orgAlias, @Param("pid") Long patientId);
}
