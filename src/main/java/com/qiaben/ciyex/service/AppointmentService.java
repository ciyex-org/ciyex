package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.entity.Appointment;
import com.qiaben.ciyex.repository.AppointmentRepository;
import com.qiaben.ciyex.storage.ExternalAppointmentStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class AppointmentService {

    private final AppointmentRepository repository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;
    private static final int DEFAULT_DAYS_AHEAD = 7;

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

    @Autowired
    public AppointmentService(AppointmentRepository repository,
                              ExternalStorageResolver storageResolver,
                              OrgIntegrationConfigProvider configProvider) {
        this.repository = repository;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }
    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    // -------- Create --------
    @Transactional
    public AppointmentDTO create(AppointmentDTO dto) {
        // Validate mandatory fields
        validateMandatoryFields(dto);

        Appointment entity = mapToEntity(dto);

        // Auto-generate externalId if not provided
        if (entity.getFhirId() == null) {
            String generatedId = "APT-" + System.currentTimeMillis();
            entity.setFhirId(generatedId);
            entity.setExternalId(generatedId);
            log.info("Auto-generated externalId: {}", generatedId);
        }

        entity = repository.save(entity);
        syncExternalCreate(entity);

        return mapToDto(entity);
    }

    // -------- Retrieve --------
    @Transactional(readOnly = true)
    public AppointmentDTO getById(Long id) {
        Appointment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDTO> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getByPatientId(Long patientId) {
        return repository.findAllByPatientId(patientId)
                .stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDTO> getByPatientId(Long patientId, Pageable pageable) {
        return repository.findAllByPatientId(patientId, pageable).map(this::mapToDto);
    }

    // -------- Update --------
    @Transactional
    public AppointmentDTO update(Long id, AppointmentDTO dto) {
        Appointment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id));

        updateEntityFromDto(entity, dto);
        entity.setLastModifiedDate(LocalDateTime.now());

        // Validate mandatory fields after update
        validateMandatoryFields(entity);

        entity = repository.save(entity);
        syncExternalUpdate(entity, dto);

        return mapToDto(entity);

    }

    // -------- Delete --------
    @Transactional
    public void delete(Long id) {
        Appointment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id));

        syncExternalDelete(entity);
        repository.delete(entity);

    }

    // -------- First Available Slots --------
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getFirstAvailableSlotsForProvider(Long providerId, int limit) {
        return getAvailableSlots(providerId, DEFAULT_DAYS_AHEAD, limit);
    }

    // -------- Available Slots: N days ahead --------
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAvailableSlots(Long providerId, int daysAhead, int limit) {
        List<AppointmentDTO> slots = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < daysAhead && slots.size() < limit; i++) {
            LocalDate date = today.plusDays(i);
            slots.addAll(generateSlotsForDate(providerId, date, limit - slots.size()));
        }
        return slots.stream().limit(limit).toList();

    }

    // -------- Available Slots: Single Date --------
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAvailableSlotsForDate(Long providerId, LocalDate date, int limit) {
        return generateSlotsForDate(providerId, date, limit);
    }

    // -------- Slot Generator --------
    private List<AppointmentDTO> generateSlotsForDate(Long providerId, LocalDate date, int limit) {
        List<Appointment> existing = repository
                .findAllByProviderIdAndAppointmentStartDate(providerId, date.toString());

        List<AppointmentDTO> slots = new ArrayList<>();
        LocalTime workStart = LocalTime.of(9, 0);
        LocalTime workEnd = LocalTime.of(17, 0);
        List<Integer> slotDurations = List.of(15, 30);

        // Generate slots for each duration type, distributing across durations
        int slotsPerDuration = (limit + slotDurations.size() - 1) / slotDurations.size(); // Ceiling division

        for (int durationMinutes : slotDurations) {
            if (slots.size() >= limit) break;

            LocalTime current = workStart;
            int slotsForThisDuration = Math.min(slotsPerDuration, limit - slots.size());

            for (int i = 0; i < slotsForThisDuration && !current.plusMinutes(durationMinutes).isAfter(workEnd); i++) {
                LocalTime slotStart = current;
                LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

                boolean booked = existing.stream().anyMatch(appt -> {
                    try {
                        return date.equals(LocalDate.parse(appt.getAppointmentStartDate())) &&
                                slotStart.equals(LocalTime.parse(appt.getAppointmentStartTime()));
                    } catch (Exception e) {
                        return false;
                    }
                });

                if (!booked) {
                    AppointmentDTO slot = new AppointmentDTO();
                    slot.setProviderId(providerId);
                    slot.setAppointmentStartDate(date);
                    slot.setAppointmentEndDate(date);
                    slot.setAppointmentStartTime(slotStart);
                    slot.setAppointmentEndTime(slotEnd);
                    slot.setFormattedDate(date.format(dateFmt));
                    slot.setFormattedTime(slotStart.format(timeFmt));
                    slot.setStatus("AVAILABLE");
                    slots.add(slot);
                }

                // Move to next potential slot time
                current = slotEnd;
            }
        }

        log.info("Generated {} available slots for provider {} on date {}", slots.size(), providerId, date);
        return slots.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    // -------- Mapping Helpers --------
    private Appointment mapToEntity(AppointmentDTO dto) {
        Appointment entity = new Appointment();
        entity.setVisitType(dto.getVisitType());
        entity.setPatientId(dto.getPatientId());
        entity.setProviderId(dto.getProviderId());
        entity.setAppointmentStartDate(dto.getAppointmentStartDate() != null ? dto.getAppointmentStartDate().toString() : null);
        entity.setAppointmentEndDate(dto.getAppointmentEndDate() != null ? dto.getAppointmentEndDate().toString() : null);
        entity.setAppointmentStartTime(dto.getAppointmentStartTime() != null ? dto.getAppointmentStartTime().toString() : null);
        entity.setAppointmentEndTime(dto.getAppointmentEndTime() != null ? dto.getAppointmentEndTime().toString() : null);
        entity.setPriority(dto.getPriority());
        entity.setLocationId(dto.getLocationId());
        entity.setStatus(dto.getStatus());
        entity.setReason(dto.getReason());

        // Use externalId if provided, otherwise use fhirId
        String fhirIdValue = dto.getExternalId() != null ? dto.getExternalId() : dto.getFhirId();
        entity.setFhirId(fhirIdValue);
        entity.setExternalId(fhirIdValue);
        // entity.setMeetingUrl(dto.getMeetingUrl());


        return entity;
    }

    private AppointmentDTO mapToDto(Appointment entity) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(entity.getId());
        dto.setVisitType(entity.getVisitType());
        dto.setPatientId(entity.getPatientId());
        dto.setProviderId(entity.getProviderId());
        dto.setAppointmentStartDate(entity.getAppointmentStartDate() != null ? LocalDate.parse(entity.getAppointmentStartDate()) : null);
        dto.setAppointmentEndDate(entity.getAppointmentEndDate() != null ? LocalDate.parse(entity.getAppointmentEndDate()) : null);
        dto.setAppointmentStartTime(entity.getAppointmentStartTime() != null ? LocalTime.parse(entity.getAppointmentStartTime()) : null);
        dto.setAppointmentEndTime(entity.getAppointmentEndTime() != null ? LocalTime.parse(entity.getAppointmentEndTime()) : null);
        dto.setPriority(entity.getPriority());
        dto.setLocationId(entity.getLocationId());
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());
        dto.setFhirId(entity.getFhirId());
        dto.setExternalId(entity.getFhirId()); // externalId is an alias for fhirId
        // dto.setMeetingUrl(entity.getMeetingUrl());

        AppointmentDTO.Audit audit = new AppointmentDTO.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().toString());
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().toString());
        }
        dto.setAudit(audit);

        if (dto.getAppointmentStartDate() != null && dto.getAppointmentStartTime() != null) {
            dto.setFormattedDate(dto.getAppointmentStartDate().format(dateFmt));
            dto.setFormattedTime(dto.getAppointmentStartTime().format(timeFmt));
        }
        return dto;
    }

    private void updateEntityFromDto(Appointment entity, AppointmentDTO dto) {
        if (dto.getVisitType() != null) entity.setVisitType(dto.getVisitType());
        if (dto.getPatientId() != null) entity.setPatientId(dto.getPatientId());
        if (dto.getProviderId() != null) entity.setProviderId(dto.getProviderId());
        if (dto.getAppointmentStartDate() != null) entity.setAppointmentStartDate(dto.getAppointmentStartDate().toString());
        if (dto.getAppointmentEndDate() != null) entity.setAppointmentEndDate(dto.getAppointmentEndDate().toString());
        if (dto.getAppointmentStartTime() != null) entity.setAppointmentStartTime(dto.getAppointmentStartTime().toString());
        if (dto.getAppointmentEndTime() != null) entity.setAppointmentEndTime(dto.getAppointmentEndTime().toString());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());
        if (dto.getLocationId() != null) entity.setLocationId(dto.getLocationId());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getReason() != null) entity.setReason(dto.getReason());

        // Update fhirId if externalId or fhirId is provided
        String fhirIdValue = dto.getExternalId() != null ? dto.getExternalId() : dto.getFhirId();
        if (fhirIdValue != null) {
            entity.setFhirId(fhirIdValue);
            entity.setExternalId(fhirIdValue);
        }
        // if (dto.getMeetingUrl() != null) entity.setMeetingUrl(dto.getMeetingUrl());
    }


    // -------- External Sync Methods --------
    private void syncExternalCreate(Appointment entity) {
        try {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null && !"fhir".equals(storageType)) { // Temporarily disable FHIR sync
                ExternalAppointmentStorage externalStorage =
                        (ExternalAppointmentStorage) storageResolver.resolve(AppointmentDTO.class);
                externalStorage.create(mapToDto(entity));
            }
        } catch (Exception e) {
            log.error("External sync create failed: {}", e.getMessage());
            // Don't rethrow - external sync failure shouldn't fail the main transaction
        }
    }

    private void syncExternalUpdate(Appointment entity, AppointmentDTO dto) {
        try {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null && !"fhir".equals(storageType)) { // Temporarily disable FHIR sync
                ExternalAppointmentStorage externalStorage =
                        (ExternalAppointmentStorage) storageResolver.resolve(AppointmentDTO.class);
                externalStorage.update(dto, String.valueOf(entity.getId()));
            }
        } catch (Exception e) {
            log.error("External sync update failed: {}", e.getMessage());
            // Don't rethrow - external sync failure shouldn't fail the main transaction
        }
    }

    private void syncExternalDelete(Appointment entity) {
        try {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null && !"fhir".equals(storageType)) { // Temporarily disable FHIR sync
                ExternalAppointmentStorage externalStorage =
                        (ExternalAppointmentStorage) storageResolver.resolve(AppointmentDTO.class);
                externalStorage.delete(String.valueOf(entity.getId()));
            }
        } catch (Exception e) {
            log.error("External sync delete failed: {}", e.getMessage());
            // Don't rethrow - external sync failure shouldn't fail the main transaction
        }
    }
    // =========================================================
    // Update STATUS only (for the UI dropdown)
    // =========================================================
    @Transactional
    public AppointmentDTO updateStatus(Long id, String newStatus) {
        Appointment entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id));

        String normalized = normalizeStatus(newStatus);

        // Business rule: UI updates only to CHECKED / UNCHECKED. Relax if you want more.
        if (!"CHECKED".equals(normalized) && !"UNCHECKED".equals(normalized)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Allowed: Checked, Unchecked.");
        }

        entity.setStatus(normalized);
        entity.setLastModifiedDate(LocalDateTime.now());
        entity = repository.save(entity);

        // Optional sync to external systems
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && !"fhir".equals(storageType)) { // Temporarily disable FHIR sync
            try {
                ExternalAppointmentStorage externalStorage =
                        (ExternalAppointmentStorage) storageResolver.resolve(AppointmentDTO.class);
                externalStorage.update(mapToDto(entity), String.valueOf(entity.getId()));
                log.info("Updated status for appointment {} in external storage", entity.getId());
            } catch (Exception e) {
                log.error("Failed to sync status to external storage: {}", e.getMessage());
                // Don't fail the main transaction for external sync issues
            }
        }

        return mapToDto(entity);
    }

    // ---- Validation helpers ----
    private void validateMandatoryFields(AppointmentDTO dto) {
        if (dto == null) throw new IllegalArgumentException("appointment payload is required");
        if (dto.getAppointmentStartDate() == null) throw new IllegalArgumentException("appointmentStartDate is required");
        if (dto.getAppointmentStartTime() == null) throw new IllegalArgumentException("appointmentStartTime is required");
        if (dto.getAppointmentEndDate() == null) throw new IllegalArgumentException("appointmentEndDate is required");
        if (dto.getAppointmentEndTime() == null) throw new IllegalArgumentException("appointmentEndTime is required");
        if (isBlank(dto.getPriority())) throw new IllegalArgumentException("priority is required");
        if (dto.getLocationId() == null) throw new IllegalArgumentException("locationId is required");
        if (isBlank(dto.getStatus())) throw new IllegalArgumentException("status is required");
    }

    private void validateMandatoryFields(Appointment entity) {
        if (entity == null) throw new IllegalArgumentException("appointment is required");
        if (isBlank(entity.getAppointmentStartDate())) throw new IllegalArgumentException("appointmentStartDate is required");
        if (isBlank(entity.getAppointmentStartTime())) throw new IllegalArgumentException("appointmentStartTime is required");
        if (isBlank(entity.getAppointmentEndDate())) throw new IllegalArgumentException("appointmentEndDate is required");
        if (isBlank(entity.getAppointmentEndTime())) throw new IllegalArgumentException("appointmentEndTime is required");
        if (isBlank(entity.getPriority())) throw new IllegalArgumentException("priority is required");
        if (entity.getLocationId() == null) throw new IllegalArgumentException("locationId is required");
        if (isBlank(entity.getStatus())) throw new IllegalArgumentException("status is required");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
