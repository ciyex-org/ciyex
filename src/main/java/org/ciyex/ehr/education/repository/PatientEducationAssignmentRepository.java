package org.ciyex.ehr.education.repository;

import org.ciyex.ehr.education.entity.PatientEducationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PatientEducationAssignmentRepository extends JpaRepository<PatientEducationAssignment, Long> {

    List<PatientEducationAssignment> findByOrgAliasAndPatientIdOrderByAssignedDateDesc(String orgAlias, Long patientId);

    List<PatientEducationAssignment> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status);

    @Query("SELECT COUNT(a) FROM PatientEducationAssignment a WHERE a.orgAlias = :org AND a.patientId = :pid AND a.status = :status")
    long countByOrgAliasAndPatientIdAndStatus(@Param("org") String orgAlias, @Param("pid") Long patientId, @Param("status") String status);
}
