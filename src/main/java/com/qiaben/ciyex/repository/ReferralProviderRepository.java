package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ReferralProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReferralProviderRepository extends JpaRepository<ReferralProvider, Long> {

    // Always bring practice when loading a single provider
    @Query("SELECT p FROM ReferralProvider p JOIN FETCH p.practice WHERE p.id = :id")
    Optional<ReferralProvider> findByIdWithPractice(@Param("id") Long id);

    // List with practice eagerly loaded (no name=null)
    @Query("SELECT p FROM ReferralProvider p JOIN FETCH p.practice")
    List<ReferralProvider> findAllWithPractice();

    // By practice id with practice loaded
    @Query("SELECT p FROM ReferralProvider p JOIN FETCH p.practice pr WHERE pr.id = :practiceId")
    List<ReferralProvider> findByPracticeIdWithPractice(@Param("practiceId") Long practiceId);
}
