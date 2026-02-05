package com.qiaben.ciyex.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Appointment Service - FHIR Only.
 * All appointment data is stored in HAPI FHIR server as Appointment resources.
 */
@Service
@Slf4j
public class AppointmentService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private static final int DEFAULT_DAYS_AHEAD = 7;

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

    @Autowired
    public AppointmentService(FhirClientService fhirClientService, PracticeContextService practiceContextService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    // ✅ Create appointment in FHIR
    public AppointmentDTO create(AppointmentDTO dto) {
        validateMandatoryFields(dto);
        log.info("Creating appointment in FHIR");

        Appointment fhirAppointment = toFhirAppointment(dto);

        MethodOutcome outcome = fhirClientService.create(fhirAppointment, getPracticeId());

        String fhirId = outcome.getId().getIdPart();
        log.info("Created FHIR Appointment with ID: {}", fhirId);
        
        // Re-read to get full metadata including audit info
        return getByFhirId(fhirId);
    }

    // ✅ Get appointment by FHIR ID
    public AppointmentDTO getById(Long id) {
        return getByFhirId(String.valueOf(id));
    }

    public AppointmentDTO getByFhirId(String fhirId) {
        log.debug("Reading FHIR Appointment with ID: {}", fhirId);
        try {
            Appointment fhirAppointment = fhirClientService.read(Appointment.class, fhirId, getPracticeId());
            return toAppointmentDto(fhirAppointment);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Appointment not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Get all appointments with pagination
    public Page<AppointmentDTO> getAll(Pageable pageable) {
        log.debug("Getting all FHIR Appointments");

        Bundle bundle = fhirClientService.search(Appointment.class, getPracticeId());
        List<AppointmentDTO> allAppointments = extractAppointments(bundle);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allAppointments.size());

        List<AppointmentDTO> pageContent = start < allAppointments.size()
                ? allAppointments.subList(start, end)
                : new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, allAppointments.size());
    }

    // ✅ Get appointments by patient
    public List<AppointmentDTO> getByPatientId(Long patientId) {
        String patientFhirId = String.valueOf(patientId);
        log.debug("Getting FHIR Appointments for patient: {}", patientFhirId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Appointment.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patientFhirId))
                
                .returnBundle(Bundle.class)
                .execute();

        return extractAppointments(bundle);
    }

    public Page<AppointmentDTO> getByPatientId(Long patientId, Pageable pageable) {
        List<AppointmentDTO> all = getByPatientId(patientId);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<AppointmentDTO> pageContent = start < all.size() ? all.subList(start, end) : new ArrayList<>();
        return new PageImpl<>(pageContent, pageable, all.size());
    }

    public AppointmentDTO getLatestByPatientId(Long patientId) {
        List<AppointmentDTO> appointments = getByPatientId(patientId);
        return appointments.stream()
                .max(Comparator.comparing(a -> {
                    if (a.getAppointmentStartDate() != null && a.getAppointmentStartTime() != null) {
                        return LocalDateTime.of(a.getAppointmentStartDate(), a.getAppointmentStartTime());
                    }
                    return LocalDateTime.MIN;
                }))
                .orElse(null);
    }

    // ✅ Update appointment in FHIR
    public AppointmentDTO update(Long id, AppointmentDTO dto) {
        return updateByFhirId(String.valueOf(id), dto);
    }

    public AppointmentDTO updateByFhirId(String fhirId, AppointmentDTO dto) {
        log.info("Updating FHIR Appointment with ID: {}", fhirId);

        Appointment fhirAppointment = toFhirAppointment(dto);
        fhirAppointment.setId(fhirId);

        fhirClientService.update(fhirAppointment, getPracticeId());

        log.info("Updated FHIR Appointment with ID: {}", fhirId);
        
        // Re-read to get updated metadata
        return getByFhirId(fhirId);
    }

    // ✅ Delete appointment from FHIR
    public void delete(Long id) {
        deleteByFhirId(String.valueOf(id));
    }

    public void deleteByFhirId(String fhirId) {
        log.info("Deleting FHIR Appointment with ID: {}", fhirId);
        fhirClientService.delete(Appointment.class, fhirId, getPracticeId());
        log.info("Deleted FHIR Appointment with ID: {}", fhirId);
    }

    public void deleteByPatientId(Long patientId) {
        log.info("Deleting all appointments for patient: {}", patientId);
        List<AppointmentDTO> appointments = getByPatientId(patientId);
        for (AppointmentDTO appointment : appointments) {
            if (appointment.getFhirId() != null) {
                deleteByFhirId(appointment.getFhirId());
            }
        }
        log.info("Deleted {} appointments for patient {}", appointments.size(), patientId);
    }

    // ✅ Update status only
    public AppointmentDTO updateStatus(Long id, String newStatus) {
        String fhirId = String.valueOf(id);
        String normalized = normalizeStatus(newStatus);

        if (!"CHECKED".equals(normalized) && !"UNCHECKED".equals(normalized)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Allowed: Checked, Unchecked.");
        }

        try {
            Appointment fhirAppointment = fhirClientService.read(Appointment.class, fhirId, getPracticeId());
            fhirAppointment.setStatus(mapToFhirStatus(normalized));
            fhirClientService.update(fhirAppointment, getPracticeId());
            return toAppointmentDto(fhirAppointment);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Appointment not found with FHIR ID: " + fhirId);
        }
    }

    // ✅ Available slots (generated, not from FHIR)
    public List<AppointmentDTO> getFirstAvailableSlotsForProvider(Long providerId, int limit) {
        return getAvailableSlots(providerId, DEFAULT_DAYS_AHEAD, limit);
    }

    public List<AppointmentDTO> getAvailableSlots(Long providerId, int daysAhead, int limit) {
        List<AppointmentDTO> slots = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < daysAhead && slots.size() < limit; i++) {
            LocalDate date = today.plusDays(i);
            slots.addAll(generateSlotsForDate(providerId, date, limit - slots.size()));
        }
        return slots.stream().limit(limit).toList();
    }

    public List<AppointmentDTO> getAvailableSlotsForDate(Long providerId, LocalDate date, int limit) {
        return generateSlotsForDate(providerId, date, limit);
    }

    private List<AppointmentDTO> generateSlotsForDate(Long providerId, LocalDate date, int limit) {
        // Get existing appointments for this provider on this date from FHIR
        List<AppointmentDTO> existing = getAppointmentsByProviderAndDate(providerId, date);

        List<AppointmentDTO> slots = new ArrayList<>();
        LocalTime workStart = LocalTime.of(9, 0);
        LocalTime workEnd = LocalTime.of(17, 0);
        List<Integer> slotDurations = List.of(15, 30);

        int slotsPerDuration = (limit + slotDurations.size() - 1) / slotDurations.size();

        for (int durationMinutes : slotDurations) {
            if (slots.size() >= limit) break;

            LocalTime current = workStart;
            int slotsForThisDuration = Math.min(slotsPerDuration, limit - slots.size());

            for (int i = 0; i < slotsForThisDuration && !current.plusMinutes(durationMinutes).isAfter(workEnd); i++) {
                LocalTime slotStart = current;
                LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

                boolean booked = existing.stream().anyMatch(appt ->
                        appt.getAppointmentStartDate() != null &&
                        appt.getAppointmentStartTime() != null &&
                        date.equals(appt.getAppointmentStartDate()) &&
                        slotStart.equals(appt.getAppointmentStartTime())
                );

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

                current = slotEnd;
            }
        }

        log.info("Generated {} available slots for provider {} on date {}", slots.size(), providerId, date);
        return slots.stream().limit(limit).toList();
    }

    private List<AppointmentDTO> getAppointmentsByProviderAndDate(Long providerId, LocalDate date) {
        try {
            Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                    .forResource(Appointment.class)
                    .where(new ReferenceClientParam("practitioner").hasId("Practitioner/" + providerId))
                    .where(new DateClientParam("date").exactly().day(date.toString()))
                    
                    .returnBundle(Bundle.class)
                    .execute();
            return extractAppointments(bundle);
        } catch (Exception e) {
            log.warn("Could not fetch appointments for provider {} on {}: {}", providerId, date, e.getMessage());
            return new ArrayList<>();
        }
    }

    public long count() {
        Bundle bundle = fhirClientService.search(Appointment.class, getPracticeId());
        return bundle.getTotal();
    }

    // ========== FHIR Mapping Methods ==========

    private Appointment toFhirAppointment(AppointmentDTO dto) {
        Appointment appointment = new Appointment();

        // Status
        appointment.setStatus(mapToFhirStatus(dto.getStatus()));

        // Priority
        if (dto.getPriority() != null) {
            appointment.setPriority(mapPriorityToInt(dto.getPriority()));
        }

        // Start/End time
        if (dto.getAppointmentStartDate() != null && dto.getAppointmentStartTime() != null) {
            LocalDateTime startDateTime = LocalDateTime.of(dto.getAppointmentStartDate(), dto.getAppointmentStartTime());
            appointment.setStart(Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (dto.getAppointmentEndDate() != null && dto.getAppointmentEndTime() != null) {
            LocalDateTime endDateTime = LocalDateTime.of(dto.getAppointmentEndDate(), dto.getAppointmentEndTime());
            appointment.setEnd(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        }

        // Patient participant
        if (dto.getPatientId() != null) {
            appointment.addParticipant()
                    .setActor(new Reference("Patient/" + dto.getPatientId()))
                    .setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        // Provider participant
        if (dto.getProviderId() != null) {
            appointment.addParticipant()
                    .setActor(new Reference("Practitioner/" + dto.getProviderId()))
                    .setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        // Location participant
        if (dto.getLocationId() != null) {
            appointment.addParticipant()
                    .setActor(new Reference("Location/" + dto.getLocationId()))
                    .setStatus(Appointment.ParticipationStatus.ACCEPTED);
        }

        // Visit type
        if (dto.getVisitType() != null) {
            appointment.addServiceType()
                    .setText(dto.getVisitType());
        }

        // Reason
        if (dto.getReason() != null) {
            appointment.addReasonCode()
                    .setText(dto.getReason());
        }

        // Meeting URL (stored as supportingInformation)
        if (dto.getMeetingUrl() != null) {
            appointment.addSupportingInformation()
                    .setDisplay(dto.getMeetingUrl());
        }

        return appointment;
    }

    private AppointmentDTO toAppointmentDto(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();

        // FHIR ID
        if (appointment.hasId()) {
            String fhirId = appointment.getIdElement().getIdPart();
            dto.setId(Long.parseLong(fhirId));
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
        }

        // Audit metadata from FHIR meta
        if (appointment.hasMeta()) {
            AppointmentDTO.Audit audit = new AppointmentDTO.Audit();
            if (appointment.getMeta().hasLastUpdated()) {
                String timestamp = appointment.getMeta().getLastUpdated().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
                audit.setCreatedDate(timestamp);
                audit.setLastModifiedDate(timestamp);
            }
            dto.setAudit(audit);
        }

        // Status
        dto.setStatus(mapFromFhirStatus(appointment.getStatus()));

        // Priority
        dto.setPriority(mapPriorityFromInt(appointment.getPriority()));

        // Start/End time
        if (appointment.hasStart()) {
            LocalDateTime startDateTime = appointment.getStart().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            dto.setAppointmentStartDate(startDateTime.toLocalDate());
            dto.setAppointmentStartTime(startDateTime.toLocalTime());
        }
        if (appointment.hasEnd()) {
            LocalDateTime endDateTime = appointment.getEnd().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            dto.setAppointmentEndDate(endDateTime.toLocalDate());
            dto.setAppointmentEndTime(endDateTime.toLocalTime());
        }

        // Extract participants
        for (Appointment.AppointmentParticipantComponent participant : appointment.getParticipant()) {
            if (participant.hasActor() && participant.getActor().hasReference()) {
                String ref = participant.getActor().getReference();
                try {
                    if (ref.startsWith("Patient/")) {
                        dto.setPatientId(Long.parseLong(ref.substring(8)));
                    } else if (ref.startsWith("Practitioner/")) {
                        dto.setProviderId(Long.parseLong(ref.substring(13)));
                    } else if (ref.startsWith("Location/")) {
                        dto.setLocationId(Long.parseLong(ref.substring(9)));
                    }
                } catch (NumberFormatException e) {
                    // FHIR ID is not numeric, skip
                }
            }
        }

        // Visit type
        if (appointment.hasServiceType()) {
            dto.setVisitType(appointment.getServiceTypeFirstRep().getText());
        }

        // Reason
        if (appointment.hasReasonCode()) {
            dto.setReason(appointment.getReasonCodeFirstRep().getText());
        }

        // Meeting URL (from supportingInformation)
        if (appointment.hasSupportingInformation() && !appointment.getSupportingInformation().isEmpty()) {
            dto.setMeetingUrl(appointment.getSupportingInformationFirstRep().getDisplay());
        }

        // Formatted date/time
        if (dto.getAppointmentStartDate() != null && dto.getAppointmentStartTime() != null) {
            dto.setFormattedDate(dto.getAppointmentStartDate().format(dateFmt));
            dto.setFormattedTime(dto.getAppointmentStartTime().format(timeFmt));
        }

        return dto;
    }

    private List<AppointmentDTO> extractAppointments(Bundle bundle) {
        List<AppointmentDTO> appointments = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Appointment) {
                    appointments.add(toAppointmentDto((Appointment) entry.getResource()));
                }
            }
        }
        return appointments;
    }

    private Appointment.AppointmentStatus mapToFhirStatus(String status) {
        if (status == null) return Appointment.AppointmentStatus.PROPOSED;
        return switch (status.toUpperCase()) {
            case "CHECKED", "CONFIRMED" -> Appointment.AppointmentStatus.FULFILLED;
            case "UNCHECKED", "PENDING" -> Appointment.AppointmentStatus.BOOKED;
            case "CANCELLED" -> Appointment.AppointmentStatus.CANCELLED;
            case "AVAILABLE" -> Appointment.AppointmentStatus.PROPOSED;
            default -> Appointment.AppointmentStatus.BOOKED;
        };
    }

    private String mapFromFhirStatus(Appointment.AppointmentStatus status) {
        if (status == null) return "PENDING";
        return switch (status) {
            case FULFILLED, CHECKEDIN -> "CHECKED";
            case BOOKED, PENDING -> "UNCHECKED";
            case CANCELLED, NOSHOW -> "CANCELLED";
            default -> "PENDING";
        };
    }

    private int mapPriorityToInt(String priority) {
        if (priority == null) return 5;
        return switch (priority.toUpperCase()) {
            case "URGENT", "HIGH" -> 1;
            case "NORMAL", "MEDIUM" -> 5;
            case "LOW" -> 9;
            default -> 5;
        };
    }

    private String mapPriorityFromInt(int priority) {
        if (priority <= 2) return "HIGH";
        if (priority <= 6) return "NORMAL";
        return "LOW";
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

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
