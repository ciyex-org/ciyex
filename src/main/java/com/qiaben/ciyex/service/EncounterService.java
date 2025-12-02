






package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.entity.EncounterStatus;
import com.qiaben.ciyex.repository.EncounterRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final com.qiaben.ciyex.repository.PatientRepository patientRepository;
    private final ExternalStorageResolver externalStorageResolver;
    private final OrgIntegrationConfigProvider orgIntegrationConfigProvider;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public EncounterService(EncounterRepository encounterRepository, 
                           com.qiaben.ciyex.repository.PatientRepository patientRepository,
                           ExternalStorageResolver externalStorageResolver, 
                           OrgIntegrationConfigProvider orgIntegrationConfigProvider) {
        this.encounterRepository = encounterRepository;
        this.patientRepository = patientRepository;
        this.externalStorageResolver = externalStorageResolver;
        this.orgIntegrationConfigProvider = orgIntegrationConfigProvider;
    }

    @Transactional
    public EncounterDto createEncounter(Long patientId, EncounterDto dto) {
        // Step 1: Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        Encounter encounter = mapToEntity(dto);
        encounter.setId(null);
        encounter.setPatientId(patientId);
        long now = System.currentTimeMillis();
        encounter.setEncounterDate(dto.getEncounterDate());

        encounter = encounterRepository.save(encounter);

        // External sync
        String storageType = orgIntegrationConfigProvider.getStorageType();
        if (!"none".equals(storageType)) {
            try {
                log.info("Attempting FHIR sync for Encounter ID: {}", encounter.getId());
                ExternalStorage<EncounterDto> ext = externalStorageResolver.resolve(EncounterDto.class);
                log.info("Resolved external storage: {}", ext.getClass().getName());

                EncounterDto snapshot = mapToDto(encounter);
                String externalId = ext.create(snapshot);
                log.info("FHIR create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    encounter.setExternalId(externalId);
                    encounter = encounterRepository.save(encounter);
                    log.info("Created FHIR resource for Encounter ID: {} with externalId: {}", encounter.getId(), externalId);
                } else {
                    log.warn("FHIR create returned null or empty externalId for Encounter ID: {}", encounter.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to sync Encounter to external storage", ex);
            }
        }

        if (encounter.getExternalId() == null) {
            String generatedId = "ENC-" + System.currentTimeMillis();
            encounter.setExternalId(generatedId);
            encounter.setFhirId(generatedId);
            encounter = encounterRepository.save(encounter);
            log.info("Auto-generated externalId: {}", generatedId);
        } else {
            encounter.setFhirId(encounter.getExternalId());
            encounter = encounterRepository.save(encounter);
        }

        return mapToDto(encounter);
    }

    @Transactional(readOnly = true)
    public List<EncounterDto> listByPatient(Long patientId) {
        // Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }
        return encounterRepository.findByPatientId(patientId)
                .stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public EncounterDto getByIdForPatient(Long id, Long patientId) {
        // Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        Encounter encounter = encounterRepository
                .findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct.",
                        id, patientId)
                ));
        return mapToDto(encounter);
    }

    @Transactional
    public EncounterDto updateEncounter(Long id, Long patientId, EncounterDto dto) {
        // Step 1: Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        // Step 2: Find encounter
        Encounter encounter = encounterRepository
                .findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct.",
                        id, patientId)
                ));

        // Step 3: Check if encounter is signed - prevent modification
        validateEncounterNotSigned(id, patientId);
        
        encounter.setVisitCategory(dto.getVisitCategory());
        encounter.setEncounterProvider(dto.getEncounterProvider());
        encounter.setType(dto.getType());
        encounter.setSensitivity(dto.getSensitivity());
        encounter.setDischargeDisposition(dto.getDischargeDisposition());
        encounter.setReasonForVisit(dto.getReasonForVisit());
        encounter.setEncounterDate(dto.getEncounterDate());
        encounter = encounterRepository.save(encounter);

        // External sync
        String storageType = orgIntegrationConfigProvider.getStorageType();
        if (!"none".equals(storageType)) {
            ExternalStorage<EncounterDto> ext = externalStorageResolver.resolve(EncounterDto.class);
            EncounterDto snapshot = mapToDto(encounter);
            if (encounter.getExternalId() != null) {
                ext.update(snapshot, encounter.getExternalId());
            }
        }

        if (encounter.getExternalId() == null) {
            String generatedId = "ENC-" + System.currentTimeMillis();
            encounter.setExternalId(generatedId);
            encounter.setFhirId(generatedId);
            encounter = encounterRepository.save(encounter);
            log.info("Auto-generated externalId for update: {}", generatedId);
        }

        return mapToDto(encounter);
    }

    @Transactional
    public void deleteEncounter(Long id, Long patientId) {
        // Step 1: Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        // Step 2: Find encounter
        Encounter encounter = encounterRepository
                .findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct.",
                        id, patientId)
                ));

        // Step 3: Check if encounter is signed - prevent modification
        validateEncounterNotSigned(id, patientId);

        // External sync
        String storageType = orgIntegrationConfigProvider.getStorageType();
        if (!"none".equals(storageType) && encounter.getExternalId() != null) {
            ExternalStorage<EncounterDto> ext = externalStorageResolver.resolve(EncounterDto.class);
            ext.delete(encounter.getExternalId());
        }

        encounterRepository.deleteByIdAndPatientId(id, patientId);
    }
    @Transactional
    public EncounterDto signEncounter(Long id, Long patientId) {
        return updateStatus(id, patientId,  EncounterStatus.SIGNED);
    }

    @Transactional
    public EncounterDto unsignEncounter(Long id, Long patientId) {
        return updateStatus(id, patientId,  EncounterStatus.UNSIGNED);
    }

    @Transactional
    public EncounterDto markIncomplete(Long id, Long patientId) {
        return updateStatus(id, patientId,  EncounterStatus.INCOMPLETE);
    }

    private EncounterDto updateStatus(Long id, Long patientId,  EncounterStatus status) {
        // Validate Patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException(
                String.format("Patient not found with ID: %d. Please provide a valid Patient ID.", patientId)
            );
        }

        Encounter encounter = encounterRepository
                .findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct.",
                        id, patientId)
                ));
        encounter.setStatus(status);
        encounter = encounterRepository.save(encounter);

        // External sync if externalId exists
        if (encounter.getExternalId() != null) {
            String storageType = orgIntegrationConfigProvider.getStorageType();
            if (!"none".equals(storageType)) {
                try {
                    ExternalStorage<EncounterDto> ext = externalStorageResolver.resolve(EncounterDto.class);
                    EncounterDto snapshot = mapToDto(encounter);
                    ext.update(snapshot, encounter.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync status update to external storage", ex);
                }
            }
        }

        return mapToDto(encounter);
    }

    /**
     * Checks if an encounter is signed and throws an exception if it is.
     * This prevents modifications to signed encounters.
     * @param encounterId the encounter ID to check
     * @param patientId the patient ID for scoping
     * @throws IllegalStateException if the encounter is signed
     * @throws RuntimeException if the encounter is not found
     */
    @Transactional(readOnly = true)
    public void validateEncounterNotSigned(Long encounterId, Long patientId) {
        Encounter encounter = encounterRepository
                .findByIdAndPatientId(encounterId, patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct.",
                        encounterId, patientId)
                ));

        if (encounter.getStatus() == EncounterStatus.SIGNED) {
            throw new IllegalStateException("Cannot modify data for a signed encounter. Please unsign the encounter first.");
        }
    }

    /**
     * Gets the status of an encounter
     * @param encounterId the encounter ID
     * @param patientId the patient ID for scoping
     * @return the encounter status
     */
    @Transactional(readOnly = true)
    public EncounterStatus getEncounterStatus(Long encounterId, Long patientId) {
        Encounter encounter = encounterRepository
                .findByIdAndPatientId(encounterId, patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Encounter not found with ID: %d for Patient ID: %d. Please verify both Patient ID and Encounter ID are correct.",
                        encounterId, patientId)
                ));
        return encounter.getStatus() != null ? encounter.getStatus() : EncounterStatus.UNSIGNED;
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
        e.setEncounterDate(dto.getEncounterDate());
        e.setStatus(dto.getStatus());
        e.setExternalId(dto.getExternalId());
        e.setFhirId(dto.getFhirId());
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
        dto.setEncounterDate(e.getEncounterDate());

        dto.setStatus(e.getStatus());
        String idValue = e.getFhirId() != null ? e.getFhirId() : ("ENC-" + e.getId());
        dto.setExternalId(idValue);
        dto.setFhirId(idValue);

        EncounterDto.Audit a = new EncounterDto.Audit();
        if (e.getCreatedDate() != null) a.setCreatedDate(DAY.format(e.getCreatedDate().atZone(ZoneId.systemDefault())));
        if (e.getLastModifiedDate() != null) a.setLastModifiedDate(DAY.format(e.getLastModifiedDate().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);

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
//    public List<EncounterDto> list(Long patientId, EncounterStatus status) {
//        List<Encounter> list = (status == null)

//                : repo.findByPatientIdAndStatusOrderByIdDesc(patientId, status);
//        return list.stream().map(EncounterMapper::toDto).toList();
//    }
//
//    public EncounterDto create(Long patientId, EncounterDto dto) {
//        Encounter e = EncounterMapper.toEntity(dto);
//        e.setId(null);
//
//        e.setPatientId(patientId);
//        if (e.getStatus() == null) e.setStatus(EncounterStatus.UNSIGNED);
//        return EncounterMapper.toDto(repo.save(e));
//    }
//
//    public EncounterDto update(Long patientId, Long id, EncounterDto dto) {
//        Encounter e = repo.findByIdAndPatientId(id, patientId)
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
//    public void delete(Long patientId, Long id) {
//        long n = repo.deleteByIdAndPatientId(id, patientId);
//        if (n == 0) throw new NoSuchElementException("Encounter not found");
//    }
//
//    public EncounterDto mark(Long patientId, Long id, EncounterStatus s) {
//        Encounter e = repo.findByIdAndPatientId(id, patientId)
//                .orElseThrow(() -> new NoSuchElementException("Encounter not found"));
//        e.setStatus(s);
//        return EncounterMapper.toDto(repo.save(e));
//    }
//
//    public Map<EncounterStatus, Long> reviewCounts(String provider, Instant from, Instant to) {
//        Map<EncounterStatus, Long> m = new EnumMap<>(EncounterStatus.class);

//            m.put((EncounterStatus) row[0], (Long) row[1]);
//        }
//        for (EncounterStatus s : EncounterStatus.values()) m.putIfAbsent(s, 0L);
//        return m;
//    }
//
//    public Page<EncounterReviewRowDto> reviewList(EncounterStatus status, String provider,
//                                                  Instant from, Instant to, Pageable pageable) {

//                .map(EncounterMapper::toReviewRow);
//    }
//
//    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
//}
