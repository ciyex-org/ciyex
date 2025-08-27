package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.AppointmentDTO;

import java.util.List;

/**
 * Defines contract for external storage providers handling Appointment synchronization.
 * Example: FHIR server, third-party scheduling system, etc.
 */
public interface ExternalAppointmentStorage extends ExternalStorage<AppointmentDTO> {

    @Override
    String create(AppointmentDTO entityDto);

    @Override
    void update(AppointmentDTO entityDto, String externalId);

    @Override
    AppointmentDTO get(String externalId);

    @Override
    void delete(String externalId);

    @Override
    List<AppointmentDTO> searchAll();

    @Override
    boolean supports(Class<?> entityType);
}
