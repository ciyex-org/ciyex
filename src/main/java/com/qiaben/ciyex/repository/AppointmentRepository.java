package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Count appointments for a given provider
    long countByProviderId(Long providerId);

    // List appointments by provider (scoped to org)
    Page<Appointment> findAllByProviderIdAndOrgId(Long providerId, Long orgId, Pageable pageable);

    // List appointments by patient (scoped to org)
    List<Appointment> findAllByPatientIdAndOrgId(Long patientId, Long orgId);

    // Get appointment by id + org
    Optional<Appointment> findByIdAndOrgId(Long id, Long orgId);

    // Get all appointments for an org (paginated)
    Page<Appointment> findAllByOrgId(Long orgId, Pageable pageable);

    Page<Appointment> findAllByPatientIdAndOrgId(Long patientId, Long orgId, Pageable pageable);

}
