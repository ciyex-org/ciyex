package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Maintenance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    List<Maintenance> findAll();

    @Query("SELECT COUNT(m) FROM Maintenance m ")
    long count();

    Page<Maintenance> findAll(Pageable pageable);
}
