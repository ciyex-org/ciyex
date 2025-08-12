package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.InsuranceCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, Long> {
    Optional<InsuranceCompany> findById(Long id);
    Optional<InsuranceCompany> findByName(String name);
}
