package org.ciyex.ehr.growth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.growth.dto.GrowthMeasurementDto;
import org.ciyex.ehr.growth.entity.GrowthMeasurement;
import org.ciyex.ehr.growth.repository.GrowthMeasurementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthMeasurementService {

    private final GrowthMeasurementRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public GrowthMeasurementDto create(Long patientId, GrowthMeasurementDto dto) {
        BigDecimal bmi = calculateBmi(dto.getWeightKg(), dto.getHeightCm());

        var measurement = GrowthMeasurement.builder()
                .patientId(patientId)
                .patientName(dto.getPatientName())
                .measurementDate(parseDate(dto.getMeasurementDate()))
                .ageMonths(dto.getAgeMonths())
                .gender(dto.getGender())
                .weightKg(dto.getWeightKg())
                .heightCm(dto.getHeightCm())
                .bmi(bmi)
                .headCircCm(dto.getHeadCircCm())
                .weightPercentile(dto.getWeightPercentile())
                .heightPercentile(dto.getHeightPercentile())
                .bmiPercentile(dto.getBmiPercentile())
                .headCircPercentile(dto.getHeadCircPercentile())
                .chartStandard(dto.getChartStandard() != null ? dto.getChartStandard() : "WHO")
                .encounterId(dto.getEncounterId())
                .measuredBy(dto.getMeasuredBy())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();

        measurement = repo.save(measurement);
        return toDto(measurement);
    }

    @Transactional(readOnly = true)
    public List<GrowthMeasurementDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdOrderByMeasurementDateDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GrowthMeasurementDto getById(Long id) {
        return repo.findByIdAndOrgAlias(id, orgAlias())
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Measurement not found: " + id));
    }

    @Transactional
    public GrowthMeasurementDto update(Long id, GrowthMeasurementDto dto) {
        var measurement = repo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Measurement not found: " + id));

        if (dto.getPatientName() != null) measurement.setPatientName(dto.getPatientName());
        if (dto.getMeasurementDate() != null) measurement.setMeasurementDate(parseDate(dto.getMeasurementDate()));
        if (dto.getAgeMonths() != null) measurement.setAgeMonths(dto.getAgeMonths());
        if (dto.getGender() != null) measurement.setGender(dto.getGender());
        if (dto.getWeightKg() != null) measurement.setWeightKg(dto.getWeightKg());
        if (dto.getHeightCm() != null) measurement.setHeightCm(dto.getHeightCm());
        if (dto.getHeadCircCm() != null) measurement.setHeadCircCm(dto.getHeadCircCm());
        if (dto.getWeightPercentile() != null) measurement.setWeightPercentile(dto.getWeightPercentile());
        if (dto.getHeightPercentile() != null) measurement.setHeightPercentile(dto.getHeightPercentile());
        if (dto.getBmiPercentile() != null) measurement.setBmiPercentile(dto.getBmiPercentile());
        if (dto.getHeadCircPercentile() != null) measurement.setHeadCircPercentile(dto.getHeadCircPercentile());
        if (dto.getChartStandard() != null) measurement.setChartStandard(dto.getChartStandard());
        if (dto.getEncounterId() != null) measurement.setEncounterId(dto.getEncounterId());
        if (dto.getMeasuredBy() != null) measurement.setMeasuredBy(dto.getMeasuredBy());
        if (dto.getNotes() != null) measurement.setNotes(dto.getNotes());

        // Recalculate BMI if weight or height changed
        BigDecimal weight = dto.getWeightKg() != null ? dto.getWeightKg() : measurement.getWeightKg();
        BigDecimal height = dto.getHeightCm() != null ? dto.getHeightCm() : measurement.getHeightCm();
        BigDecimal bmi = calculateBmi(weight, height);
        if (bmi != null) measurement.setBmi(bmi);

        return toDto(repo.save(measurement));
    }

    @Transactional
    public void delete(Long id) {
        var measurement = repo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Measurement not found: " + id));
        repo.delete(measurement);
    }

    @Transactional(readOnly = true)
    public List<GrowthMeasurementDto> getChartData(Long patientId) {
        // Return measurements ordered by age_months ascending for chart plotting
        // First try to find with gender filter from the most recent measurement
        var all = repo.findByOrgAliasAndPatientIdOrderByMeasurementDateDesc(orgAlias(), patientId);
        if (all.isEmpty()) return List.of();

        String gender = all.get(0).getGender();
        if (gender != null && !gender.isBlank()) {
            return repo.findByOrgAliasAndPatientIdAndGenderOrderByAgeMonthsAsc(
                    orgAlias(), patientId, gender)
                    .stream().map(this::toDto).toList();
        }
        // Fallback: return all sorted by age_months
        return all.stream()
                .sorted(Comparator.comparing(GrowthMeasurement::getAgeMonths,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDto).toList();
    }

    /**
     * BMI = weight_kg / (height_cm / 100)^2
     */
    private BigDecimal calculateBmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null
                || heightCm.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', using today", s);
            return LocalDate.now();
        }
    }

    private GrowthMeasurementDto toDto(GrowthMeasurement e) {
        return GrowthMeasurementDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .measurementDate(e.getMeasurementDate() != null ? e.getMeasurementDate().toString() : null)
                .ageMonths(e.getAgeMonths())
                .gender(e.getGender())
                .weightKg(e.getWeightKg())
                .heightCm(e.getHeightCm())
                .bmi(e.getBmi())
                .headCircCm(e.getHeadCircCm())
                .weightPercentile(e.getWeightPercentile())
                .heightPercentile(e.getHeightPercentile())
                .bmiPercentile(e.getBmiPercentile())
                .headCircPercentile(e.getHeadCircPercentile())
                .chartStandard(e.getChartStandard())
                .encounterId(e.getEncounterId())
                .measuredBy(e.getMeasuredBy())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
