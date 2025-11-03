package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Communication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunicationRepository extends JpaRepository<Communication, Long> {

    List<Communication> findAllByPatientId(Long patientId);

    List<Communication> findAllByPatientIdAndProviderId(Long patientId, Long providerId);

    @Query("SELECT c FROM Communication c WHERE c.patientId = :patientId OR c.providerId = :providerId ORDER BY c.createdDate DESC")
    List<Communication> findAllByPatientIdOrProviderId(@Param("patientId") Long patientId, @Param("providerId") Long providerId);
}