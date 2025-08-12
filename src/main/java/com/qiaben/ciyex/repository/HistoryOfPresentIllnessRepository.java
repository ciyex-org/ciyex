package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryOfPresentIllnessRepository extends JpaRepository<HistoryOfPresentIllness, Long> {
    // Custom query to fetch HPI by encounter ID
    List<HistoryOfPresentIllness> findByEncounterId(Long encounterId);
}
