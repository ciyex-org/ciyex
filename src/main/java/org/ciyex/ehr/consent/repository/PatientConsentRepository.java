package org.ciyex.ehr.consent.repository;

import org.ciyex.ehr.consent.entity.PatientConsent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientConsentRepository extends JpaRepository<PatientConsent, Long> {

    Page<PatientConsent> findByOrgAlias(String orgAlias, Pageable pageable);

    List<PatientConsent> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    List<PatientConsent> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status);

    List<PatientConsent> findByOrgAliasAndConsentTypeOrderByCreatedAtDesc(String orgAlias, String consentType);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    @Query("SELECT c FROM PatientConsent c WHERE c.orgAlias = :org AND (" +
           "LOWER(c.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.consentType) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.signedBy) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.witnessName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.status) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "CAST(c.patientId AS text) = :q" +
           ") ORDER BY c.createdAt DESC")
    List<PatientConsent> search(@Param("org") String orgAlias, @Param("q") String query);
}
