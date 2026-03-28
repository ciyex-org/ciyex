package org.ciyex.ehr.fax.repository;

import org.ciyex.ehr.fax.entity.FaxMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaxMessageRepository extends JpaRepository<FaxMessage, Long> {

    Page<FaxMessage> findByOrgAlias(String orgAlias, Pageable pageable);

    Page<FaxMessage> findByOrgAliasAndDirection(String orgAlias, String direction, Pageable pageable);

    List<FaxMessage> findByOrgAliasAndDirectionOrderByCreatedAtDesc(String orgAlias, String direction);

    List<FaxMessage> findByOrgAliasAndStatusOrderByCreatedAtDesc(String orgAlias, String status);

    List<FaxMessage> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    long countByOrgAliasAndDirectionAndStatus(String orgAlias, String direction, String status);

    long countByOrgAliasAndDirection(String orgAlias, String direction);

    @Query("SELECT f FROM FaxMessage f WHERE f.orgAlias = :org AND (" +
           "LOWER(f.faxNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(f.senderName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(f.recipientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(f.subject) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(f.patientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(f.category) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "CAST(f.patientId AS String) = :q" +
           ") ORDER BY f.createdAt DESC")
    List<FaxMessage> search(@Param("org") String orgAlias, @Param("q") String query);
}
