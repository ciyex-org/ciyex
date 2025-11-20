


package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.VitalsDto;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Vitals;
import com.qiaben.ciyex.repository.VitalsRepository;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.storage.ExternalVitalsStorage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class VitalsService {
    public List<VitalsDto> getAllByPatient(Long patientId) {
        return repository.findByPatientId(patientId)
            .stream().map(this::toDto).toList();
    }
    private final VitalsRepository repository;
    private final PortalPatientRepository portalPatientRepository;
    private final ExternalVitalsStorage externalStorage;

    
    @PersistenceContext
    private EntityManager entityManager;

    public VitalsDto create( Long patientId, Long encounterId, VitalsDto dto) {
        Vitals entity = toEntity(dto);
        entity.setPatientId(patientId);
        entity.setEncounterId(encounterId);
        Vitals saved = repository.save(entity);
        externalStorage.save(saved);
        return toDto(saved);
    }

    public VitalsDto get(Long patientId, Long encounterId, Long id) {
        return repository.findById(id)
                .filter(v -> v.getPatientId().equals(patientId)
                        && v.getEncounterId().equals(encounterId))
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
    }

    public List<VitalsDto> getByEncounter(Long patientId, Long encounterId) {
        return repository.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public VitalsDto update(Long patientId, Long encounterId, Long id, VitalsDto dto) {
        Vitals existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (!existing.getPatientId().equals(patientId) || !existing.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException(
                String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
            );
        }
        existing.setWeightKg(dto.getWeightKg());
        existing.setBpSystolic(dto.getBpSystolic());
        existing.setBpDiastolic(dto.getBpDiastolic());
        existing.setPulse(dto.getPulse());
        existing.setRespiration(dto.getRespiration());
        existing.setTemperatureC(dto.getTemperatureC());
        existing.setOxygenSaturation(dto.getOxygenSaturation());
        existing.setNotes(dto.getNotes());
        Vitals updated = repository.save(existing);
        externalStorage.save(updated);
        return toDto(updated);
    }

    public void delete(Long patientId, Long encounterId, Long id) {
        Vitals existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (!existing.getPatientId().equals(patientId) || !existing.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException(
                String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
            );
        }
        repository.delete(existing);
        externalStorage.delete(id);
    }

    public VitalsDto eSign(Long patientId, Long encounterId, Long id) {
        Vitals vitals = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (!vitals.getPatientId().equals(patientId) || !vitals.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException(
                String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
            );
        }
        vitals.setSigned(true);
        Vitals saved = repository.save(vitals);
        externalStorage.save(saved);
        return toDto(saved);
    }

    public byte[] print(Long patientId, Long encounterId, Long id) {
        Vitals vitals = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        if (!vitals.getPatientId().equals(patientId) || !vitals.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException(
                String.format("Vitals not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
            );
        }
        return externalStorage.print(vitals);
    }

    // 🏥 EHR Method - Get all vitals for a patient (across all encounters)
    @Transactional(readOnly = true)
    public List<VitalsDto> getVitalsByPatient(Long patientId) {
        log.info("Getting vitals for patient {} in org {}", patientId, RequestContext.get().getTenantName());
            List<Vitals> vitals = repository.findByPatientIdOrderByRecordedAtDesc(patientId);
            log.info("Found {} vitals records for patient {}", vitals.size(), patientId);
            return vitals.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
    }

    // 👩‍⚕️ Portal Method - Map portal user email to EHR patient ID
    @Transactional(readOnly = true)
    public Long getEhrPatientIdFromPortalUserEmail(String email) {
        log.info("Looking up EHR patient ID for portal user email {}", email);

        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        try {
            // Find portal user by email, then get their linked patient
            return portalPatientRepository.findAll().stream()
                    .filter(pp -> pp.getPortalUser() != null && email.equals(pp.getPortalUser().getEmail()))
                    .findFirst()
                    .map(portalPatient -> portalPatient.getEhrPatientId())
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error looking up EHR patient ID for email {}: {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * Get vitals for current portal user based on email
     */
    @Transactional
    public List<VitalsDto> getVitalsForPortalUser(String email) {
        try {
            log.info("Getting vitals for portal user with email: {}", email);

            if (email == null || email.trim().isEmpty()) {
                log.error("Email is null or empty");
                return null;
            }

            // Get EHR patient ID from portal user email
            Long patientId = getEhrPatientIdFromPortalUserEmail(email);
            log.info("Found EHR patientId {} for portal user email {}", patientId, email);

            if (patientId == null) {
                log.error("No EHR patient ID found for portal user email: {}", email);
                return null;
            }

            // Get vitals using tenant-aware query
            List<VitalsDto> vitals = getVitalsByPatient(patientId);
            log.info("Retrieved {} vitals records for patient {}", vitals != null ? vitals.size() : 0, patientId);

            return vitals;

        } catch (Exception e) {
            log.error("Error getting vitals for portal user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get vitals for user", e);
        }
    }






    private Vitals toEntity(VitalsDto dto) {
        return Vitals.builder()
                .id(dto.getId())
                .patientId(dto.getPatientId())
                .encounterId(dto.getEncounterId())
                .weightKg(dto.getWeightKg())
                .weightLbs(dto.getWeightLbs())
                .heightCm(dto.getHeightCm())
                .heightIn(dto.getHeightIn())
                .bpSystolic(dto.getBpSystolic())
                .bpDiastolic(dto.getBpDiastolic())
                .pulse(dto.getPulse())
                .respiration(dto.getRespiration())
                .temperatureC(dto.getTemperatureC())
                .temperatureF(dto.getTemperatureF())
                .oxygenSaturation(dto.getOxygenSaturation())
                .bmi(dto.getBmi())
                .notes(dto.getNotes())
                .signed(dto.getSigned())
                .recordedAt(dto.getRecordedAt())
                .build();
    }

    private VitalsDto toDto(Vitals entity) {
        return VitalsDto.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .encounterId(entity.getEncounterId())
                .weightKg(entity.getWeightKg())
                .weightLbs(entity.getWeightLbs())
                .heightCm(entity.getHeightCm())
                .heightIn(entity.getHeightIn())
                .bpSystolic(entity.getBpSystolic())
                .bpDiastolic(entity.getBpDiastolic())
                .pulse(entity.getPulse())
                .respiration(entity.getRespiration())
                .temperatureC(entity.getTemperatureC())
                .temperatureF(entity.getTemperatureF())
                .oxygenSaturation(entity.getOxygenSaturation())
                .bmi(entity.getBmi())
                .notes(entity.getNotes())
                .signed(entity.getSigned())
                .recordedAt(entity.getRecordedAt())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}
