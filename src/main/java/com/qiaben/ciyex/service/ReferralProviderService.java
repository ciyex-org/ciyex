package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ReferralProviderDto;
import com.qiaben.ciyex.entity.ReferralPractice;
import com.qiaben.ciyex.entity.ReferralProvider;
import com.qiaben.ciyex.repository.ReferralPracticeRepository;
import com.qiaben.ciyex.repository.ReferralProviderRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReferralProviderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReferralProviderRepository providerRepo;
    private final ReferralPracticeRepository practiceRepo;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public ReferralProviderService(ReferralProviderRepository providerRepo,
                                   ReferralPracticeRepository practiceRepo,
                                   ExternalStorageResolver storageResolver,
                                   OrgIntegrationConfigProvider configProvider) {
        this.providerRepo = providerRepo;
        this.practiceRepo = practiceRepo;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    // ---------- CREATE ----------
    @Transactional
    public ReferralProviderDto create(ReferralProviderDto dto) {
        ReferralProvider entity = mapToEntity(dto);

        // 🔴 IMPORTANT: attach a MANAGED practice
        if (dto.getPractice() == null || dto.getPractice().getId() == null) {
            throw new RuntimeException("Practice id is required");
        }
        ReferralPractice practice = practiceRepo.findById(dto.getPractice().getId())
                .orElseThrow(() -> new RuntimeException("Referral practice not found: " + dto.getPractice().getId()));
        entity.setPractice(practice);

        String now = LocalDateTime.now().format(DATE_FORMATTER);

        // Save locally first
        entity = providerRepo.save(entity);

        // Best-effort external sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ReferralProviderDto> external = storageResolver.resolve(ReferralProviderDto.class);
                String externalId = external.create(mapToDto(entity));
                entity.setFhirId(externalId);
                entity = providerRepo.save(entity);
                log.info("External sync (create) OK, externalId={}", externalId);
            } catch (Exception e) {
                log.warn("External sync (create) failed and will be skipped: {}", e.toString());
            }
        }

        // Re-read with practice loaded to guarantee name
        entity = providerRepo.findByIdWithPractice(entity.getId())
                .orElse(entity); // fallback, but practice should be there
        return mapToDto(entity);
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public ReferralProviderDto getById(Long id) {
        ReferralProvider entity = providerRepo.findByIdWithPractice(id)
                .orElseThrow(() -> new RuntimeException("Referral provider not found"));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public List<ReferralProviderDto> getAll() {
        return providerRepo.findAllWithPractice().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReferralProviderDto getByIdWithPractice(Long id) {
        return getById(id);
    }

    // ---------- UPDATE ----------
    @Transactional
    public ReferralProviderDto update(Long id, ReferralProviderDto dto) {
        ReferralProvider entity = providerRepo.findByIdWithPractice(id)
                .orElseThrow(() -> new RuntimeException("Referral provider not found"));

        // map simple fields
        entity = updateEntityFromDto(entity, dto);

        // If practice id provided, reattach managed entity
        if (dto.getPractice() != null && dto.getPractice().getId() != null) {
            ReferralPractice p = practiceRepo.findById(dto.getPractice().getId())
                    .orElseThrow(() -> new RuntimeException("Referral practice not found: " + dto.getPractice().getId()));
            entity.setPractice(p);
        }

        entity = providerRepo.save(entity);

        // Best-effort external sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null) {
            try {
                ExternalStorage<ReferralProviderDto> external = storageResolver.resolve(ReferralProviderDto.class);
                if (entity.getFhirId() == null) {
                    String externalId = external.create(mapToDto(entity));
                    entity.setFhirId(externalId);
                    entity = providerRepo.save(entity);
                    log.info("External sync (create-on-update) OK, externalId={}", externalId);
                } else {
                    external.update(mapToDto(entity), entity.getFhirId());
                    log.info("External sync (update) OK, externalId={}", entity.getFhirId());
                }
            } catch (Exception e) {
                log.warn("External sync (update) failed and will be skipped: {}", e.toString());
            }
        }

        // Re-read with join fetch
        entity = providerRepo.findByIdWithPractice(entity.getId())
                .orElse(entity);
        return mapToDto(entity);
    }

    // ---------- DELETE ----------
    @Transactional
    public void delete(Long id) {
        ReferralProvider entity = providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Referral provider not found"));
        providerRepo.delete(entity);
    }

    // ---------- CUSTOM QUERIES ----------
    @Transactional(readOnly = true)
    public List<ReferralProviderDto> getByPracticeId(Long practiceId) {
        return providerRepo.findByPracticeIdWithPractice(practiceId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getPracticeName(Long practiceId) {
        return practiceRepo.findById(practiceId)
                .map(ReferralPractice::getName)
                .orElseThrow(() -> new RuntimeException("Referral practice not found"));
    }

    // ---------- MAPPING ----------
    private ReferralProviderDto mapToDto(ReferralProvider entity) {
        ReferralProviderDto dto = new ReferralProviderDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSpecialty(entity.getSpecialty());
        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCountry(entity.getCountry());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setEmail(entity.getEmail());
        dto.setFhirId(entity.getFhirId());

        if (entity.getPractice() != null) {
            ReferralProviderDto.PracticeInfo pi = new ReferralProviderDto.PracticeInfo();
            pi.setId(entity.getPractice().getId());
            pi.setName(entity.getPractice().getName()); // now guaranteed non-null due to fetch-join or attached entity
            dto.setPractice(pi);
        }

        ReferralProviderDto.Audit audit = new ReferralProviderDto.Audit();
        dto.setAudit(audit);

        return dto;
    }

    private ReferralProvider mapToEntity(ReferralProviderDto dto) {
        return ReferralProvider.builder()
                .name(dto.getName())
                .specialty(dto.getSpecialty())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
                // DO NOT set practice here; attach managed entity in create/update
                .build();
    }

    private ReferralProvider updateEntityFromDto(ReferralProvider entity, ReferralProviderDto dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getSpecialty() != null) entity.setSpecialty(dto.getSpecialty());
        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getPostalCode() != null) entity.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) entity.setCountry(dto.getCountry());
        if (dto.getPhoneNumber() != null) entity.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        return entity;
    }
}
