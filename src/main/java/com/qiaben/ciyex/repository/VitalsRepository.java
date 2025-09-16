package com.qiaben.ciyex.repository;



import com.qiaben.ciyex.entity.Vitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VitalsRepository extends JpaRepository<Vitals, Long> {
    List<Vitals> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
}
