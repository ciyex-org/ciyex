package org.ciyex.ehr.referral.repository;

import org.ciyex.ehr.referral.entity.Referral;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Page<Referral> findByOrgAlias(String orgAlias, Pageable pageable);

    List<Referral> findByOrgAliasAndPatientIdOrderByReferralDateDesc(String orgAlias, Long patientId);

    List<Referral> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    @Query("SELECT r FROM Referral r WHERE r.orgAlias = :org AND (" +
           "LOWER(r.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.specialistName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.reason) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.facilityName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.specialty) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(r.authorizationNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "CAST(r.patientId AS text) = :q" +
           ") ORDER BY r.createdAt DESC")
    List<Referral> search(@Param("org") String orgAlias, @Param("q") String query);
}
