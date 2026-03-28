package org.ciyex.ehr.prescription.repository;

import org.ciyex.ehr.prescription.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    List<Prescription> findByOrgAliasAndEncounterIdOrderByCreatedAtDesc(String orgAlias, Long encounterId);

    List<Prescription> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status);

    Page<Prescription> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status, Pageable pageable);

    Page<Prescription> findByOrgAliasAndPriorityOrderByCreatedAtDesc(String orgAlias, String priority, Pageable pageable);

    Page<Prescription> findByOrgAliasAndStatusAndPriorityOrderByCreatedAtDesc(String orgAlias, String status, String priority, Pageable pageable);

    Page<Prescription> findByOrgAliasOrderByCreatedAtDesc(String orgAlias, Pageable pageable);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    @Query("SELECT p FROM Prescription p WHERE p.orgAlias = :org AND (" +
           "LOWER(p.medicationName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.prescriberName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.medicationCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.sig) LIKE LOWER(CONCAT('%',:q,'%'))" +
           ") ORDER BY p.createdAt DESC")
    List<Prescription> search(@Param("org") String orgAlias, @Param("q") String query);
}
