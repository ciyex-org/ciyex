package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Communication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunicationRepository extends JpaRepository<Communication, Long> {

    @Query("SELECT COUNT(c) FROM Communication c WHERE c.orgId = :orgId")
    long countByOrgId(Long orgId);

    List<Communication> findAllByOrgId(Long orgId);

    Page<Communication> findByOrgId(Long orgId, Pageable pageable);

    Optional<Communication> findByExternalIdAndOrgId(String externalId, Long orgId);

    List<Communication> findByInResponseToAndOrgId(String inResponseTo, Long orgId);

    @Query("SELECT c FROM Communication c WHERE c.orgId = :orgId AND (c.subject = :ref OR c.recipients LIKE CONCAT('%', :ref, '%'))")
    Page<Communication> findCommunicationsForPatientRef(Long orgId, String ref, Pageable pageable);

    @Query("SELECT c FROM Communication c WHERE c.orgId = :orgId AND (c.sender = :ref OR c.recipients LIKE CONCAT('%', :ref, '%'))")
    Page<Communication> findCommunicationsForProviderRef(Long orgId, String ref, Pageable pageable);
}