package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Optional<Facility> findByIdAndIsActiveTrue(Long id);

    List<Facility> findAllByIsActiveTrue();

    List<Facility> findAllByIsActive(Boolean isActive);

    Optional<Facility> findByName(String name);

    List<Facility> findByNameContainingIgnoreCase(String name);

    Optional<Facility> findByNpi(String npi);

    Optional<Facility> findByTaxId(String taxId);

    List<Facility> findByBillingLocationTrue();

    List<Facility> findByServiceLocationTrue();
}