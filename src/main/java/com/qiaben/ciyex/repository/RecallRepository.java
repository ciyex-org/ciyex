package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Recall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecallRepository extends JpaRepository<Recall, Long> {
    List<Recall> findAll();
    List<Recall> findByPatientId(Long patientId);

    @Query("SELECT COUNT(r) FROM Recall r ")
    long count();

    Page<Recall> findAll(Pageable pageable);

}
