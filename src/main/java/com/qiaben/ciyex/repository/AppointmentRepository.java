package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // Single tenant per instance - no orgId filtering needed

    // -------- Count --------
    long countByProviderId(Long providerId);

    // -------- Provider Scoped --------
    Page<Appointment> findAllByProviderId(Long providerId, Pageable pageable);

    List<Appointment> findAllByProviderId(Long providerId);

    // Top N appointments (for "next slots" view)
    List<Appointment> findTop3ByProviderIdAndStatusOrderByAppointmentStartDateAscAppointmentStartTimeAsc(
            Long providerId, String status);

    // -------- Patient Scoped --------
    List<Appointment> findAllByPatientId(Long patientId);

    Page<Appointment> findAllByPatientId(Long patientId, Pageable pageable);

    // -------- By ID --------
    Optional<Appointment> findById(Long id);

    // -------- Scoped by Provider + Date --------
    List<Appointment> findAllByProviderIdAndAppointmentStartDate(
            Long providerId, String appointmentStartDate);

    // -------- Scoped by Provider + Date Range --------
    List<Appointment> findAllByProviderIdAndAppointmentStartDateBetween(
            Long providerId, String startDate, String endDate);

    Page<Appointment> findAllByStatus(String status, Pageable pageable);

    // -------- Org Scoped --------
    Page<Appointment> findAll(Pageable pageable);

    long count();
}