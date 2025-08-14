package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ReferralPractice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralPracticeRepository extends JpaRepository<ReferralPractice, Long> {
    Optional<ReferralPractice> findById(Long id);
    Optional<ReferralPractice> findByName(String name);
}
