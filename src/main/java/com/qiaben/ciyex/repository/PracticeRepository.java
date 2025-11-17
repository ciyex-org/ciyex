package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Practice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PracticeRepository extends JpaRepository<Practice, Long> {

    Optional<Practice> findByExternalId(String externalId);

    @Query("SELECT p.externalId FROM Practice p")
    List<String> findAllExternalIds();

    Optional<Practice> findByName(String name);

    @Query("SELECT p FROM Practice p WHERE p.name LIKE %:name%")
    List<Practice> findByNameContaining(@Param("name") String name);

    @Query("SELECT p FROM Practice p WHERE p.enablePatientPractice = :enablePatientPractice")
    List<Practice> findByEnablePatientPractice(@Param("enablePatientPractice") Boolean enablePatientPractice);

    @Query("SELECT p FROM Practice p WHERE p.timeZone = :timeZone")
    List<Practice> findByTimeZone(@Param("timeZone") String timeZone);
}