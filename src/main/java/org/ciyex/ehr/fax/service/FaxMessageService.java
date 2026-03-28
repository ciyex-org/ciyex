package org.ciyex.ehr.fax.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.fax.dto.FaxMessageDto;
import org.ciyex.ehr.fax.entity.FaxMessage;
import org.ciyex.ehr.fax.repository.FaxMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaxMessageService {

    private final FaxMessageRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── CRUD ──

    @Transactional
    public FaxMessageDto create(FaxMessageDto dto) {
        var fax = FaxMessage.builder()
                .direction(dto.getDirection() != null ? dto.getDirection() : "outbound")
                .faxNumber(dto.getFaxNumber())
                .senderName(dto.getSenderName())
                .recipientName(dto.getRecipientName())
                .subject(dto.getSubject())
                .pageCount(dto.getPageCount())
                .status(dto.getStatus() != null ? dto.getStatus() : "pending")
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .category(dto.getCategory())
                .documentUrl(dto.getDocumentUrl())
                .errorMessage(dto.getErrorMessage())
                .sentAt(parseDateTime(dto.getSentAt()))
                .receivedAt(parseDateTime(dto.getReceivedAt()))
                .processedBy(dto.getProcessedBy())
                .processedAt(parseDateTime(dto.getProcessedAt()))
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();
        fax = repo.save(fax);
        return toDto(fax);
    }

    @Transactional(readOnly = true)
    public FaxMessageDto getById(Long id) {
        return repo.findById(id)
                .filter(f -> f.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Fax message not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<FaxMessageDto> getAll(Pageable pageable) {
        return repo.findByOrgAlias(orgAlias(), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<FaxMessageDto> listByDirection(String direction, Pageable pageable) {
        return repo.findByOrgAliasAndDirection(orgAlias(), direction, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<FaxMessageDto> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findByOrgAlias(orgAlias(), Pageable.unpaged())
                    .stream().map(this::toDto).toList();
        }
        return repo.search(orgAlias(), query.trim())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public FaxMessageDto update(Long id, FaxMessageDto dto) {
        var fax = repo.findById(id)
                .filter(f -> f.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Fax message not found: " + id));

        if (dto.getDirection() != null) fax.setDirection(dto.getDirection());
        if (dto.getFaxNumber() != null) fax.setFaxNumber(dto.getFaxNumber());
        if (dto.getSenderName() != null) fax.setSenderName(dto.getSenderName());
        if (dto.getRecipientName() != null) fax.setRecipientName(dto.getRecipientName());
        if (dto.getSubject() != null) fax.setSubject(dto.getSubject());
        if (dto.getPageCount() != null) fax.setPageCount(dto.getPageCount());
        if (dto.getStatus() != null) fax.setStatus(dto.getStatus());
        if (dto.getPatientId() != null) fax.setPatientId(dto.getPatientId());
        if (dto.getPatientName() != null) fax.setPatientName(dto.getPatientName());
        if (dto.getCategory() != null) fax.setCategory(dto.getCategory());
        if (dto.getDocumentUrl() != null) fax.setDocumentUrl(dto.getDocumentUrl());
        if (dto.getErrorMessage() != null) fax.setErrorMessage(dto.getErrorMessage());
        if (dto.getNotes() != null) fax.setNotes(dto.getNotes());

        return toDto(repo.save(fax));
    }

    @Transactional
    public void delete(Long id) {
        var fax = repo.findById(id)
                .filter(f -> f.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Fax message not found: " + id));
        repo.delete(fax);
    }

    // ── Actions ──

    @Transactional
    public FaxMessageDto assignToPatient(Long id, Long patientId, String patientName, String category) {
        var fax = repo.findById(id)
                .filter(f -> f.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Fax message not found: " + id));
        fax.setPatientId(patientId);
        fax.setPatientName(patientName);
        if (category != null) fax.setCategory(category);
        fax.setStatus("categorized");
        return toDto(repo.save(fax));
    }

    @Transactional
    public FaxMessageDto markProcessed(Long id, String processedBy) {
        var fax = repo.findById(id)
                .filter(f -> f.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Fax message not found: " + id));
        fax.setProcessedBy(processedBy);
        fax.setProcessedAt(LocalDateTime.now());
        fax.setStatus("attached");
        return toDto(repo.save(fax));
    }

    // ── Stats ──

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        String org = orgAlias();
        Map<String, Object> stats = new LinkedHashMap<>();

        // Inbound stats
        Map<String, Long> inbound = new LinkedHashMap<>();
        inbound.put("total", repo.countByOrgAliasAndDirection(org, "inbound"));
        inbound.put("pending", repo.countByOrgAliasAndDirectionAndStatus(org, "inbound", "pending"));
        inbound.put("received", repo.countByOrgAliasAndDirectionAndStatus(org, "inbound", "received"));
        inbound.put("categorized", repo.countByOrgAliasAndDirectionAndStatus(org, "inbound", "categorized"));
        inbound.put("attached", repo.countByOrgAliasAndDirectionAndStatus(org, "inbound", "attached"));
        stats.put("inbound", inbound);

        // Outbound stats
        Map<String, Long> outbound = new LinkedHashMap<>();
        outbound.put("total", repo.countByOrgAliasAndDirection(org, "outbound"));
        outbound.put("pending", repo.countByOrgAliasAndDirectionAndStatus(org, "outbound", "pending"));
        outbound.put("sending", repo.countByOrgAliasAndDirectionAndStatus(org, "outbound", "sending"));
        outbound.put("sent", repo.countByOrgAliasAndDirectionAndStatus(org, "outbound", "sent"));
        outbound.put("delivered", repo.countByOrgAliasAndDirectionAndStatus(org, "outbound", "delivered"));
        outbound.put("failed", repo.countByOrgAliasAndDirectionAndStatus(org, "outbound", "failed"));
        stats.put("outbound", outbound);

        return stats;
    }

    // ── DateTime parsing ──

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.endsWith("Z") || s.contains("+")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse datetime '{}', returning null", s);
            return null;
        }
    }

    // ── Mapping ──

    private FaxMessageDto toDto(FaxMessage e) {
        return FaxMessageDto.builder()
                .id(e.getId())
                .direction(e.getDirection())
                .faxNumber(e.getFaxNumber())
                .senderName(e.getSenderName())
                .recipientName(e.getRecipientName())
                .subject(e.getSubject())
                .pageCount(e.getPageCount())
                .status(e.getStatus())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .category(e.getCategory())
                .documentUrl(e.getDocumentUrl())
                .errorMessage(e.getErrorMessage())
                .sentAt(e.getSentAt() != null ? e.getSentAt().toString() : null)
                .receivedAt(e.getReceivedAt() != null ? e.getReceivedAt().toString() : null)
                .processedBy(e.getProcessedBy())
                .processedAt(e.getProcessedAt() != null ? e.getProcessedAt().toString() : null)
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
