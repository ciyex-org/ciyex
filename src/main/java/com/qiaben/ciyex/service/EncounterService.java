

package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.repository.EncounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EncounterService {

    private final EncounterRepository encounterRepository;

    @Autowired
    public EncounterService(EncounterRepository encounterRepository) {
        this.encounterRepository = encounterRepository;
    }

    @Transactional
    public EncounterDto createEncounter(Long patientId, EncounterDto dto, Long orgId) {
        Encounter encounter = mapToEntity(dto);
        encounter.setId(null);
        encounter.setPatientId(patientId);                 // NEW
        encounter.setOrgId(orgId);
        long now = System.currentTimeMillis();
        encounter.setCreatedAt(now);
        encounter.setUpdatedAt(now);
        encounter.setEncounterDate(dto.getEncounterDate());

        encounter = encounterRepository.save(encounter);
        return mapToDto(encounter);
    }

    @Transactional(readOnly = true)
    public List<EncounterDto> listByPatient(Long patientId, Long orgId) {
        return encounterRepository.findByPatientIdAndOrgId(patientId, orgId)
                .stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public EncounterDto getByIdForPatient(Long id, Long patientId, Long orgId) {
        Encounter encounter = encounterRepository
                .findByIdAndPatientIdAndOrgId(id, patientId, orgId)
                .orElseThrow(() -> new RuntimeException("Encounter not found"));
        return mapToDto(encounter);
    }

    @Transactional
    public EncounterDto updateEncounter(Long id, Long patientId, EncounterDto dto, Long orgId) {
        Encounter encounter = encounterRepository
                .findByIdAndPatientIdAndOrgId(id, patientId, orgId)
                .orElseThrow(() -> new RuntimeException("Encounter not found"));

        // Do NOT overwrite id/patientId/orgId here
        encounter.setVisitCategory(dto.getVisitCategory());
        encounter.setEncounterProvider(dto.getEncounterProvider());
        encounter.setType(dto.getType());
        encounter.setSensitivity(dto.getSensitivity());
        encounter.setDischargeDisposition(dto.getDischargeDisposition());
        encounter.setReasonForVisit(dto.getReasonForVisit());
        encounter.setEncounterDate(dto.getEncounterDate());

        encounter.setUpdatedAt(System.currentTimeMillis());
        encounter = encounterRepository.save(encounter);
        return mapToDto(encounter);
    }

    @Transactional
    public void deleteEncounter(Long id, Long patientId, Long orgId) {
        long deleted = encounterRepository.deleteByIdAndPatientIdAndOrgId(id, patientId, orgId);
        if (deleted == 0) {
            throw new RuntimeException("Encounter not found");
        }
    }

    // ----- Mappers -----

    private Encounter mapToEntity(EncounterDto dto) {
        Encounter e = new Encounter();
        e.setId(dto.getId());
        e.setPatientId(dto.getPatientId()); // will be enforced from path on create
        e.setVisitCategory(dto.getVisitCategory());
        e.setEncounterProvider(dto.getEncounterProvider());
        e.setType(dto.getType());
        e.setSensitivity(dto.getSensitivity());
        e.setDischargeDisposition(dto.getDischargeDisposition());
        e.setReasonForVisit(dto.getReasonForVisit());
        e.setOrgId(dto.getOrgId());
        e.setEncounterDate(dto.getEncounterDate());
        e.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : 0L);
        e.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : 0L);
        return e;
    }

    private EncounterDto mapToDto(Encounter e) {
        EncounterDto dto = new EncounterDto();
        dto.setId(e.getId());
        dto.setPatientId(e.getPatientId());
        dto.setVisitCategory(e.getVisitCategory());
        dto.setEncounterProvider(e.getEncounterProvider());
        dto.setType(e.getType());
        dto.setSensitivity(e.getSensitivity());
        dto.setDischargeDisposition(e.getDischargeDisposition());
        dto.setReasonForVisit(e.getReasonForVisit());
        dto.setOrgId(e.getOrgId());
        dto.setEncounterDate(e.getEncounterDate());

        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}


//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.EncounterDto;
//import com.qiaben.ciyex.dto.EncounterReviewRowDto;
//import com.qiaben.ciyex.entity.Encounter;
//import com.qiaben.ciyex.entity.EncounterStatus;
//import com.qiaben.ciyex.mapper.EncounterMapper;
//import com.qiaben.ciyex.repository.EncounterRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.*;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//public class EncounterService {
//
//    private final EncounterRepository repo;
//
//    public List<EncounterDto> list(Long orgId, Long patientId, EncounterStatus status) {
//        List<Encounter> list = (status == null)
//                ? repo.findByPatientIdAndOrgIdOrderByIdDesc(patientId, orgId)
//                : repo.findByPatientIdAndOrgIdAndStatusOrderByIdDesc(patientId, orgId, status);
//        return list.stream().map(EncounterMapper::toDto).toList();
//    }
//
//    public EncounterDto create(Long orgId, Long patientId, EncounterDto dto) {
//        Encounter e = EncounterMapper.toEntity(dto);
//        e.setId(null);
//        e.setOrgId(orgId);
//        e.setPatientId(patientId);
//        if (e.getStatus() == null) e.setStatus(EncounterStatus.UNSIGNED);
//        return EncounterMapper.toDto(repo.save(e));
//    }
//
//    public EncounterDto update(Long orgId, Long patientId, Long id, EncounterDto dto) {
//        Encounter e = repo.findByIdAndPatientIdAndOrgId(id, patientId, orgId)
//                .orElseThrow(() -> new NoSuchElementException("Encounter not found"));
//        e.setVisitCategory(dto.getVisitCategory());
//        e.setEncounterProvider(dto.getEncounterProvider());
//        e.setType(dto.getType());
//        e.setSensitivity(dto.getSensitivity());
//        e.setDischargeDisposition(dto.getDischargeDisposition());
//        e.setReasonForVisit(dto.getReasonForVisit());
//        e.setEncounterDate(dto.getEncounterDate());
//        return EncounterMapper.toDto(repo.save(e));
//    }
//
//    public void delete(Long orgId, Long patientId, Long id) {
//        long n = repo.deleteByIdAndPatientIdAndOrgId(id, patientId, orgId);
//        if (n == 0) throw new NoSuchElementException("Encounter not found");
//    }
//
//    public EncounterDto mark(Long orgId, Long patientId, Long id, EncounterStatus s) {
//        Encounter e = repo.findByIdAndPatientIdAndOrgId(id, patientId, orgId)
//                .orElseThrow(() -> new NoSuchElementException("Encounter not found"));
//        e.setStatus(s);
//        return EncounterMapper.toDto(repo.save(e));
//    }
//
//    public Map<EncounterStatus, Long> reviewCounts(Long orgId, String provider, Instant from, Instant to) {
//        Map<EncounterStatus, Long> m = new EnumMap<>(EncounterStatus.class);
//        for (Object[] row : repo.countByStatus(orgId, emptyToNull(provider), from, to)) {
//            m.put((EncounterStatus) row[0], (Long) row[1]);
//        }
//        for (EncounterStatus s : EncounterStatus.values()) m.putIfAbsent(s, 0L);
//        return m;
//    }
//
//    public Page<EncounterReviewRowDto> reviewList(Long orgId, EncounterStatus status, String provider,
//                                                  Instant from, Instant to, Pageable pageable) {
//        return repo.reviewList(orgId, status, emptyToNull(provider), from, to, pageable)
//                .map(EncounterMapper::toReviewRow);
//    }
//
//    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
//}
