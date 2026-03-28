package org.ciyex.ehr.lab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.lab.dto.LabResultDto;
import org.ciyex.ehr.lab.entity.LabResult;
import org.ciyex.ehr.lab.repository.LabOrderRepository;
import org.ciyex.ehr.lab.repository.LabResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabResultService {

    private final LabResultRepository repo;
    private final LabOrderRepository orderRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public LabResultDto create(LabResultDto dto) {
        var builder = LabResult.builder()
                .patientId(dto.getPatientId())
                .encounterId(dto.getEncounterId())
                .orderNumber(dto.getOrderNumber())
                .procedureName(dto.getProcedureName())
                .testCode(dto.getTestCode())
                .testName(dto.getTestName())
                .loincCode(dto.getLoincCode())
                .status(dto.getStatus() != null ? dto.getStatus() : "Pending")
                .specimen(dto.getSpecimen())
                .collectedDate(dto.getCollectedDate() != null && !dto.getCollectedDate().isBlank()
                        ? LocalDate.parse(dto.getCollectedDate()) : null)
                .reportedDate(dto.getReportedDate() != null && !dto.getReportedDate().isBlank()
                        ? LocalDate.parse(dto.getReportedDate()) : null)
                .abnormalFlag(dto.getAbnormalFlag())
                .value(dto.getValue())
                .numericValue(dto.getNumericValue() != null ? BigDecimal.valueOf(dto.getNumericValue()) : parseNumeric(dto.getValue()))
                .units(dto.getUnits())
                .referenceLow(dto.getReferenceLow() != null ? BigDecimal.valueOf(dto.getReferenceLow()) : null)
                .referenceHigh(dto.getReferenceHigh() != null ? BigDecimal.valueOf(dto.getReferenceHigh()) : null)
                .referenceRange(dto.getReferenceRange())
                .notes(dto.getNotes())
                .recommendations(dto.getRecommendations())
                .signed(dto.getSigned() != null ? dto.getSigned() : false)
                .signedAt(dto.getSignedAt() != null && !dto.getSignedAt().isBlank() ? LocalDateTime.parse(dto.getSignedAt()) : null)
                .signedBy(dto.getSignedBy())
                .panelName(dto.getPanelName())
                .panelCode(dto.getPanelCode())
                .orgAlias(orgAlias());

        if (dto.getLabOrderId() != null) {
            orderRepo.findById(dto.getLabOrderId()).ifPresent(builder::labOrder);
        }

        // Auto-calculate abnormal flag if not provided
        var result = builder.build();
        if (result.getAbnormalFlag() == null && result.getNumericValue() != null) {
            result.setAbnormalFlag(calculateAbnormalFlag(result.getNumericValue(), result.getReferenceLow(), result.getReferenceHigh()));
        }

        result = repo.save(result);
        return toDto(result);
    }

    @Transactional(readOnly = true)
    public List<LabResultDto> getAll() {
        return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LabResultDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByCollectedDateDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LabResultDto> getByOrder(Long orderId) {
        return repo.findByOrgAliasAndLabOrderIdOrderByTestNameAsc(orgAlias(), orderId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LabResultDto getById(Long id) {
        return repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Result not found: " + id));
    }

    @Transactional
    public LabResultDto update(Long id, LabResultDto dto) {
        var result = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Result not found: " + id));

        if (dto.getTestName() != null) result.setTestName(dto.getTestName());
        if (dto.getTestCode() != null) result.setTestCode(dto.getTestCode());
        if (dto.getLoincCode() != null) result.setLoincCode(dto.getLoincCode());
        if (dto.getStatus() != null) result.setStatus(dto.getStatus());
        if (dto.getSpecimen() != null) result.setSpecimen(dto.getSpecimen());
        if (dto.getCollectedDate() != null && !dto.getCollectedDate().isBlank())
            result.setCollectedDate(LocalDate.parse(dto.getCollectedDate()));
        if (dto.getReportedDate() != null && !dto.getReportedDate().isBlank())
            result.setReportedDate(LocalDate.parse(dto.getReportedDate()));
        if (dto.getAbnormalFlag() != null) result.setAbnormalFlag(dto.getAbnormalFlag());
        if (dto.getValue() != null) {
            result.setValue(dto.getValue());
            result.setNumericValue(dto.getNumericValue() != null ? BigDecimal.valueOf(dto.getNumericValue()) : parseNumeric(dto.getValue()));
        }
        if (dto.getUnits() != null) result.setUnits(dto.getUnits());
        if (dto.getReferenceLow() != null) result.setReferenceLow(BigDecimal.valueOf(dto.getReferenceLow()));
        if (dto.getReferenceHigh() != null) result.setReferenceHigh(BigDecimal.valueOf(dto.getReferenceHigh()));
        if (dto.getReferenceRange() != null) result.setReferenceRange(dto.getReferenceRange());
        if (dto.getNotes() != null) result.setNotes(dto.getNotes());
        if (dto.getRecommendations() != null) result.setRecommendations(dto.getRecommendations());
        if (dto.getPanelName() != null) result.setPanelName(dto.getPanelName());
        if (dto.getPanelCode() != null) result.setPanelCode(dto.getPanelCode());

        if (dto.getSigned() != null && dto.getSigned() && !Boolean.TRUE.equals(result.getSigned())) {
            result.setSigned(true);
            result.setSignedAt(LocalDateTime.now());
            result.setSignedBy(dto.getSignedBy());
        }

        // Recalculate abnormal flag
        if (result.getNumericValue() != null && (dto.getAbnormalFlag() == null || dto.getAbnormalFlag().isBlank())) {
            result.setAbnormalFlag(calculateAbnormalFlag(result.getNumericValue(), result.getReferenceLow(), result.getReferenceHigh()));
        }

        return toDto(repo.save(result));
    }

    @Transactional
    public void delete(Long id) {
        var result = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Result not found: " + id));
        repo.delete(result);
    }

    // Trending: get all results for a specific LOINC code for a patient over time
    @Transactional(readOnly = true)
    public List<LabResultDto> getTrend(Long patientId, String loincCode) {
        return repo.findByOrgAliasAndPatientIdAndLoincCodeOrderByCollectedDateAsc(orgAlias(), patientId, loincCode)
                .stream().map(this::toDto).toList();
    }

    // Panel grouping: get results grouped by panel
    @Transactional(readOnly = true)
    public List<LabResultDto> getByPanel(Long patientId, String panelName) {
        return repo.findByOrgAliasAndPatientIdAndPanelNameOrderByCollectedDateDescTestNameAsc(orgAlias(), patientId, panelName)
                .stream().map(this::toDto).toList();
    }

    // Sign a result (e-signature)
    @Transactional
    public LabResultDto signResult(Long id, String signedBy) {
        var result = repo.findById(id)
                .filter(r -> r.getOrgAlias().equals(orgAlias()))
                .orElseThrow(() -> new NoSuchElementException("Result not found: " + id));
        result.setSigned(true);
        result.setSignedAt(LocalDateTime.now());
        result.setSignedBy(signedBy);
        return toDto(repo.save(result));
    }

    // Helpers

    private BigDecimal parseNumeric(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); } catch (NumberFormatException e) { return null; }
    }

    private String calculateAbnormalFlag(BigDecimal value, BigDecimal low, BigDecimal high) {
        if (value == null) return null;
        if (low != null && high != null) {
            // Critical thresholds (20% beyond range)
            BigDecimal critLow = low.subtract(low.multiply(BigDecimal.valueOf(0.2)));
            BigDecimal critHigh = high.add(high.multiply(BigDecimal.valueOf(0.2)));
            if (value.compareTo(critLow) < 0 || value.compareTo(critHigh) > 0) return "Critical";
            if (value.compareTo(low) < 0) return "Low";
            if (value.compareTo(high) > 0) return "High";
            return "Normal";
        }
        if (low != null && value.compareTo(low) < 0) return "Low";
        if (high != null && value.compareTo(high) > 0) return "High";
        return null;
    }

    private LabResultDto toDto(LabResult e) {
        return LabResultDto.builder()
                .id(e.getId())
                .labOrderId(e.getLabOrder() != null ? e.getLabOrder().getId() : null)
                .patientId(e.getPatientId())
                .encounterId(e.getEncounterId())
                .orderNumber(e.getOrderNumber())
                .procedureName(e.getProcedureName())
                .testCode(e.getTestCode())
                .testName(e.getTestName())
                .loincCode(e.getLoincCode())
                .status(e.getStatus())
                .specimen(e.getSpecimen())
                .collectedDate(e.getCollectedDate() != null ? e.getCollectedDate().toString() : null)
                .reportedDate(e.getReportedDate() != null ? e.getReportedDate().toString() : null)
                .abnormalFlag(e.getAbnormalFlag())
                .value(e.getValue())
                .numericValue(e.getNumericValue() != null ? e.getNumericValue().doubleValue() : null)
                .units(e.getUnits())
                .referenceLow(e.getReferenceLow() != null ? e.getReferenceLow().doubleValue() : null)
                .referenceHigh(e.getReferenceHigh() != null ? e.getReferenceHigh().doubleValue() : null)
                .referenceRange(e.getReferenceRange())
                .notes(e.getNotes())
                .recommendations(e.getRecommendations())
                .signed(e.getSigned())
                .signedAt(e.getSignedAt() != null ? e.getSignedAt().toString() : null)
                .signedBy(e.getSignedBy())
                .panelName(e.getPanelName())
                .panelCode(e.getPanelCode())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
