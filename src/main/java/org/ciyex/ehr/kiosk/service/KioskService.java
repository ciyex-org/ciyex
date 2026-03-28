package org.ciyex.ehr.kiosk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.kiosk.dto.KioskCheckinDto;
import org.ciyex.ehr.kiosk.dto.KioskConfigDto;
import org.ciyex.ehr.kiosk.entity.KioskCheckin;
import org.ciyex.ehr.kiosk.entity.KioskConfig;
import org.ciyex.ehr.kiosk.repository.KioskCheckinRepository;
import org.ciyex.ehr.kiosk.repository.KioskConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KioskService {

    private final KioskConfigRepository configRepo;
    private final KioskCheckinRepository checkinRepo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    private LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }

    // ─── Config ───

    @Transactional(readOnly = true)
    public KioskConfigDto getConfig() {
        return configRepo.findByOrgAlias(orgAlias())
                .map(this::toConfigDto)
                .orElse(KioskConfigDto.builder().enabled(false).build());
    }

    @Transactional
    public KioskConfigDto saveConfig(KioskConfigDto dto) {
        var entity = configRepo.findByOrgAlias(orgAlias())
                .orElse(KioskConfig.builder().orgAlias(orgAlias()).build());
        entity.setEnabled(dto.getEnabled());
        entity.setConfig(dto.getConfig());
        entity.setWelcomeMessage(dto.getWelcomeMessage());
        entity.setCompletionMessage(dto.getCompletionMessage());
        entity.setIdleTimeoutSec(dto.getIdleTimeoutSec());
        entity = configRepo.save(entity);
        return toConfigDto(entity);
    }

    // ─── Check-in ───

    @Transactional
    public KioskCheckinDto checkIn(KioskCheckinDto dto) {
        var checkin = KioskCheckin.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .appointmentId(dto.getAppointmentId())
                .demographicsUpdated(dto.getDemographicsUpdated() != null ? dto.getDemographicsUpdated() : false)
                .insuranceUpdated(dto.getInsuranceUpdated() != null ? dto.getInsuranceUpdated() : false)
                .consentSigned(dto.getConsentSigned() != null ? dto.getConsentSigned() : false)
                .copayCollected(dto.getCopayCollected() != null ? dto.getCopayCollected() : false)
                .copayAmount(dto.getCopayAmount())
                .verificationMethod(dto.getVerificationMethod())
                .orgAlias(orgAlias())
                .build();
        return toCheckinDto(checkinRepo.save(checkin));
    }

    @Transactional(readOnly = true)
    public Page<KioskCheckinDto> getTodayCheckins(Pageable pageable) {
        return checkinRepo.findByOrgAliasAndCheckInTimeAfter(orgAlias(), todayStart(), pageable)
                .map(this::toCheckinDto);
    }

    @Transactional(readOnly = true)
    public List<KioskCheckinDto> getCheckinsByPatient(Long patientId) {
        return checkinRepo.findByOrgAliasAndPatientId(orgAlias(), patientId)
                .stream().map(this::toCheckinDto).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> todayStats() {
        String org = orgAlias();
        LocalDateTime start = todayStart();
        long total = checkinRepo.countTodayByOrgAlias(org, start);
        long demoUpdated = checkinRepo.countDemographicsUpdated(org, start);
        long consentSigned = checkinRepo.countConsentSigned(org, start);
        double demoPct = total > 0 ? Math.round(demoUpdated * 10000.0 / total) / 100.0 : 0.0;
        double consentPct = total > 0 ? Math.round(consentSigned * 10000.0 / total) / 100.0 : 0.0;
        return Map.of(
                "totalCheckins", total,
                "demographicsUpdatedPct", demoPct,
                "consentSignedPct", consentPct
        );
    }

    // ─── Mappers ───

    private KioskConfigDto toConfigDto(KioskConfig e) {
        return KioskConfigDto.builder()
                .id(e.getId())
                .enabled(e.getEnabled())
                .config(e.getConfig())
                .welcomeMessage(e.getWelcomeMessage())
                .completionMessage(e.getCompletionMessage())
                .idleTimeoutSec(e.getIdleTimeoutSec())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private KioskCheckinDto toCheckinDto(KioskCheckin e) {
        return KioskCheckinDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .appointmentId(e.getAppointmentId())
                .checkInTime(e.getCheckInTime() != null ? e.getCheckInTime().toString() : null)
                .demographicsUpdated(e.getDemographicsUpdated())
                .insuranceUpdated(e.getInsuranceUpdated())
                .consentSigned(e.getConsentSigned())
                .copayCollected(e.getCopayCollected())
                .copayAmount(e.getCopayAmount())
                .verificationMethod(e.getVerificationMethod())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
