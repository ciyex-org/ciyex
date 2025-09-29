package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // -------- Count --------
    long countByProviderId(Long providerId);

    // -------- Provider Scoped --------
    Page<Appointment> findAllByProviderIdAndOrgId(Long providerId, Long orgId, Pageable pageable);

    List<Appointment> findAllByProviderIdAndOrgId(Long providerId, Long orgId);

    // Top N appointments (for “next slots” view)
    List<Appointment> findTop3ByProviderIdAndOrgIdAndStatusOrderByAppointmentStartDateAscAppointmentStartTimeAsc(
            Long providerId, Long orgId, String status);

    // -------- Patient Scoped --------
    List<Appointment> findAllByPatientIdAndOrgId(Long patientId, Long orgId);

    Page<Appointment> findAllByPatientIdAndOrgId(Long patientId, Long orgId, Pageable pageable);

    // -------- By ID --------
    Optional<Appointment> findByIdAndOrgId(Long id, Long orgId);

    // -------- Scoped by Provider + Date --------
    List<Appointment> findAllByProviderIdAndOrgIdAndAppointmentStartDate(
            Long providerId, Long orgId, String appointmentStartDate);

    // -------- Scoped by Provider + Date Range --------
    List<Appointment> findAllByProviderIdAndOrgIdAndAppointmentStartDateBetween(
            Long providerId, Long orgId, String startDate, String endDate);

    Page<Appointment> findAllByOrgIdAndStatus(Long orgId, String status, Pageable pageable);

    // -------- Org Scoped --------
    Page<Appointment> findAllByOrgId(Long orgId, Pageable pageable);

    long countByOrgId(Long orgId);

}