package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ChiefComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiefComplaintRepository extends JpaRepository<ChiefComplaint, Long> {
    // Custom query to fetch Chief Complaints by encounter ID
    List<ChiefComplaint> findByEncounterId(Long encounterId);
}
