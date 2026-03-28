package org.ciyex.ehr.prescription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.prescription.dto.PrescriptionDto;
import org.ciyex.ehr.prescription.entity.Prescription;
import org.ciyex.ehr.prescription.repository.PrescriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── Create ──

    @Transactional
    public PrescriptionDto create(PrescriptionDto dto) {
        var rx = Prescription.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .encounterId(dto.getEncounterId())
                .prescriberName(dto.getPrescriberName())
                .prescriberNpi(dto.getPrescriberNpi())
                .medicationName(dto.getMedicationName())
                .medicationCode(dto.getMedicationCode())
                .medicationSystem(dto.getMedicationSystem() != null ? dto.getMedicationSystem() : "NDC")
                .strength(dto.getStrength())
                .dosageForm(dto.getDosageForm())
                .sig(dto.getSig())
                .quantity(dto.getQuantity())
                .quantityUnit(dto.getQuantityUnit())
                .daysSupply(dto.getDaysSupply())
                .refills(dto.getRefills() != null ? dto.getRefills() : 0)
                .refillsRemaining(dto.getRefillsRemaining() != null ? dto.getRefillsRemaining() : (dto.getRefills() != null ? dto.getRefills() : 0))
                .pharmacyName(dto.getPharmacyName())
                .pharmacyPhone(dto.getPharmacyPhone())
                .pharmacyAddress(dto.getPharmacyAddress())
                .status(dto.getStatus() != null ? dto.getStatus() : "active")
                .priority(dto.getPriority() != null ? dto.getPriority() : "routine")
                .startDate(parseDate(dto.getStartDate()))
                .endDate(parseDate(dto.getEndDate()))
                .notes(dto.getNotes())
                .deaSchedule(dto.getDeaSchedule())
                .orgAlias(orgAlias())
                .build();
        rx = repo.save(rx);
        return toDto(rx);
    }

    // ── Read ──

    @Transactional(readOnly = true)
    public PrescriptionDto getById(Long id) {
        return repo.findById(id)
                .filter(rx -> rx.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> getByEncounter(Long encounterId) {
        return repo.findByOrgAliasAndEncounterIdOrderByCreatedAtDesc(orgAlias(), encounterId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionDto> list(Pageable pageable) {
        return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionDto> listByStatus(String status, Pageable pageable) {
        return repo.findByOrgAliasAndStatusOrderByCreatedAtDesc(orgAlias(), status, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionDto> listByPriority(String priority, Pageable pageable) {
        return repo.findByOrgAliasAndPriorityOrderByCreatedAtDesc(orgAlias(), priority, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionDto> listByStatusAndPriority(String status, String priority, Pageable pageable) {
        return repo.findByOrgAliasAndStatusAndPriorityOrderByCreatedAtDesc(orgAlias(), status, priority, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), Pageable.unpaged())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    // ── Update ──

    @Transactional
    public PrescriptionDto update(Long id, PrescriptionDto dto) {
        var rx = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));

        if (dto.getPatientName() != null) rx.setPatientName(dto.getPatientName());
        if (dto.getEncounterId() != null) rx.setEncounterId(dto.getEncounterId());
        if (dto.getPrescriberName() != null) rx.setPrescriberName(dto.getPrescriberName());
        if (dto.getPrescriberNpi() != null) rx.setPrescriberNpi(dto.getPrescriberNpi());
        if (dto.getMedicationName() != null) rx.setMedicationName(dto.getMedicationName());
        if (dto.getMedicationCode() != null) rx.setMedicationCode(dto.getMedicationCode());
        if (dto.getMedicationSystem() != null) rx.setMedicationSystem(dto.getMedicationSystem());
        if (dto.getStrength() != null) rx.setStrength(dto.getStrength());
        if (dto.getDosageForm() != null) rx.setDosageForm(dto.getDosageForm());
        if (dto.getSig() != null) rx.setSig(dto.getSig());
        if (dto.getQuantity() != null) rx.setQuantity(dto.getQuantity());
        if (dto.getQuantityUnit() != null) rx.setQuantityUnit(dto.getQuantityUnit());
        if (dto.getDaysSupply() != null) rx.setDaysSupply(dto.getDaysSupply());
        if (dto.getRefills() != null) rx.setRefills(dto.getRefills());
        if (dto.getRefillsRemaining() != null) rx.setRefillsRemaining(dto.getRefillsRemaining());
        if (dto.getPharmacyName() != null) rx.setPharmacyName(dto.getPharmacyName());
        if (dto.getPharmacyPhone() != null) rx.setPharmacyPhone(dto.getPharmacyPhone());
        if (dto.getPharmacyAddress() != null) rx.setPharmacyAddress(dto.getPharmacyAddress());
        if (dto.getStatus() != null) rx.setStatus(dto.getStatus());
        if (dto.getPriority() != null) rx.setPriority(dto.getPriority());
        if (dto.getStartDate() != null) rx.setStartDate(parseDate(dto.getStartDate()));
        if (dto.getEndDate() != null) rx.setEndDate(parseDate(dto.getEndDate()));
        if (dto.getNotes() != null) rx.setNotes(dto.getNotes());
        if (dto.getDeaSchedule() != null) rx.setDeaSchedule(dto.getDeaSchedule());

        return toDto(repo.save(rx));
    }

    // ── Delete ──

    @Transactional
    public void delete(Long id) {
        var rx = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));
        repo.delete(rx);
    }

    // ── Discontinue ──

    @Transactional
    public PrescriptionDto discontinue(Long id, String reason) {
        var rx = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));

        rx.setStatus("discontinued");
        rx.setDiscontinuedDate(LocalDate.now());
        rx.setDiscontinuedReason(reason);

        return toDto(repo.save(rx));
    }

    // ── Refill ──

    @Transactional
    public PrescriptionDto refill(Long id) {
        var rx = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));

        if (rx.getRefillsRemaining() == null || rx.getRefillsRemaining() <= 0) {
            throw new IllegalStateException("No refills remaining for prescription: " + id);
        }

        rx.setRefillsRemaining(rx.getRefillsRemaining() - 1);

        // Append audit note
        String auditNote = String.format("[%s] Refill dispensed. Remaining: %d",
                LocalDate.now(), rx.getRefillsRemaining());
        rx.setNotes(rx.getNotes() != null ? rx.getNotes() + "\n" + auditNote : auditNote);

        return toDto(repo.save(rx));
    }

    // ── Stats ──

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        String org = orgAlias();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("active", repo.countByOrgAliasAndStatus(org, "active"));
        stats.put("completed", repo.countByOrgAliasAndStatus(org, "completed"));
        stats.put("cancelled", repo.countByOrgAliasAndStatus(org, "cancelled"));
        stats.put("on_hold", repo.countByOrgAliasAndStatus(org, "on_hold"));
        stats.put("discontinued", repo.countByOrgAliasAndStatus(org, "discontinued"));
        return stats;
    }

    // ── Helpers ──

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', returning null", s);
            return null;
        }
    }

    private PrescriptionDto toDto(Prescription e) {
        return PrescriptionDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .encounterId(e.getEncounterId())
                .prescriberName(e.getPrescriberName())
                .prescriberNpi(e.getPrescriberNpi())
                .medicationName(e.getMedicationName())
                .medicationCode(e.getMedicationCode())
                .medicationSystem(e.getMedicationSystem())
                .strength(e.getStrength())
                .dosageForm(e.getDosageForm())
                .sig(e.getSig())
                .quantity(e.getQuantity())
                .quantityUnit(e.getQuantityUnit())
                .daysSupply(e.getDaysSupply())
                .refills(e.getRefills())
                .refillsRemaining(e.getRefillsRemaining())
                .pharmacyName(e.getPharmacyName())
                .pharmacyPhone(e.getPharmacyPhone())
                .pharmacyAddress(e.getPharmacyAddress())
                .status(e.getStatus())
                .priority(e.getPriority())
                .startDate(e.getStartDate() != null ? e.getStartDate().toString() : null)
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                .discontinuedDate(e.getDiscontinuedDate() != null ? e.getDiscontinuedDate().toString() : null)
                .discontinuedReason(e.getDiscontinuedReason())
                .notes(e.getNotes())
                .deaSchedule(e.getDeaSchedule())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
