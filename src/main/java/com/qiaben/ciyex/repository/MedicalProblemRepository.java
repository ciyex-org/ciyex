package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.MedicalProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface MedicalProblemRepository extends JpaRepository<MedicalProblem, Long> {

    List<MedicalProblem> findAllByPatientId(Long patientId);

    @Modifying
    void deleteAllByPatientId(Long patientId);

    @Modifying
    int deleteByIdAndPatientId(Long id, Long patientId);
}
