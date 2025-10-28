package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {
}
