package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientCodeListDto;
import com.qiaben.ciyex.entity.PatientCodeList;
import com.qiaben.ciyex.repository.PatientCodeListRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientCodeListService {

    private final PatientCodeListRepository repo;
    private final ExternalStorageResolver externalStorageResolver;
    private final OrgIntegrationConfigProvider orgIntegrationConfigProvider;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<PatientCodeListDto> findAll() {
        return repo.findAll()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientCodeListDto getById(Long id) {
        Optional<PatientCodeList> opt = repo.findById(id);
        return opt.map(this::toDto).orElse(null);
    }

    @Transactional
    public PatientCodeListDto create(PatientCodeListDto dto) {
        PatientCodeList entity = fromDto(dto, new PatientCodeList());
        PatientCodeList saved = repo.save(entity);
        if (saved.isDefault()) {
            repo.clearDefaultsExcept(saved.getId());
        }

        // External sync
        String storageType = orgIntegrationConfigProvider.getStorageType();
        if (!"none".equals(storageType)) {
            try {
                log.info("Attempting FHIR sync for PatientCodeList ID: {}", saved.getId());
                ExternalStorage<PatientCodeListDto> ext = externalStorageResolver.resolve(PatientCodeListDto.class);
                log.info("Resolved external storage: {}", ext.getClass().getName());

                PatientCodeListDto snapshot = toDto(saved);
                String externalId = ext.create(snapshot);
                log.info("FHIR create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    saved.setExternalId(externalId);
                    saved = repo.save(saved);
                    log.info("Created FHIR resource for PatientCodeList ID: {} with externalId: {}", saved.getId(), externalId);
                } else {
                    log.warn("FHIR create returned null or empty externalId for PatientCodeList ID: {}", saved.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to sync PatientCodeList to external storage", ex);
            }
        }

        if (saved.getExternalId() == null) {
            String generatedId = "CL-" + System.currentTimeMillis();
            saved.setExternalId(generatedId);
            saved.setFhirId(generatedId);
            saved = repo.save(saved);
            log.info("Auto-generated externalId: {}", generatedId);
        } else {
            saved.setFhirId(saved.getExternalId());
            saved = repo.save(saved);
        }

        return toDto(saved);
    }

    @Transactional
    public PatientCodeListDto update(Long id, PatientCodeListDto dto) {
        PatientCodeList entity = repo.findById(id).orElse(null);
        if (entity == null) return null;

        entity = fromDto(dto, entity);
        PatientCodeList saved = repo.save(entity);
        if (saved.isDefault()) {
            repo.clearDefaultsExcept(saved.getId());
        }

        // External sync
        String storageType = orgIntegrationConfigProvider.getStorageType();
        if (!"none".equals(storageType)) {
            ExternalStorage<PatientCodeListDto> ext = externalStorageResolver.resolve(PatientCodeListDto.class);
            PatientCodeListDto snapshot = toDto(saved);
            if (saved.getExternalId() != null) {
                ext.update(snapshot, saved.getExternalId());
            }
        }

        if (saved.getExternalId() == null) {
            String generatedId = "CL-" + System.currentTimeMillis();
            saved.setExternalId(generatedId);
            saved.setFhirId(generatedId);
            saved = repo.save(saved);
            log.info("Auto-generated externalId for update: {}", generatedId);
        }

        return toDto(saved);
    }

    @Transactional
    public boolean delete(Long id) {
        PatientCodeList entity = repo.findById(id).orElse(null);
        if (entity == null) return false;

        // External sync
        String storageType = orgIntegrationConfigProvider.getStorageType();
        if (!"none".equals(storageType) && entity.getExternalId() != null) {
            ExternalStorage<PatientCodeListDto> ext = externalStorageResolver.resolve(PatientCodeListDto.class);
            ext.delete(entity.getExternalId());
        }

        repo.deleteById(id);
        return true;
    }

    @Transactional
    public List<PatientCodeListDto> saveBulk(List<PatientCodeListDto> rows) {

        
        boolean seenDefault = false;
        for (PatientCodeListDto r : rows) {
            if (r.isDefault) {
                if (seenDefault) r.isDefault = false;
                else seenDefault = true;
            }
        }

        List<PatientCodeList> entities = rows.stream()
                .map(r -> {
                    PatientCodeList e = (r.id != null)
                            ? repo.findById(r.id).orElse(new PatientCodeList())
                            : new PatientCodeList();
                    return fromDto(r, e);
                })
                .sorted(Comparator.comparing(PatientCodeList::getOrderIndex))
                .collect(Collectors.toList());

        List<PatientCodeList> saved = repo.saveAll(entities);

        Long keepId = saved.stream().filter(PatientCodeList::isDefault)
                .findFirst().map(PatientCodeList::getId).orElse(null);
        if (keepId == null) repo.clearAllDefaults();
        else repo.clearDefaultsExcept(keepId);

        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PatientCodeListDto setDefault(Long id) {
        PatientCodeList e = repo.findById(id).orElse(null);
        if (e == null) return null;
        e.setDefault(true);
        repo.save(e);
        repo.clearDefaultsExcept(e.getId());
        return toDto(e);
    }

    // ---- mapping helpers ----

    private PatientCodeList fromDto(PatientCodeListDto dto, PatientCodeList e) {
        if (dto.title != null) e.setTitle(dto.title);
        if (dto.order != null) e.setOrderIndex(dto.order);
        e.setDefault(dto.isDefault);
        e.setActive(dto.active);
        e.setNotes(dto.notes);
        e.setCodes(dto.codes);
        e.setExternalId(dto.externalId);
        e.setFhirId(dto.fhirId);
        return e;
    }

    private PatientCodeListDto toDto(PatientCodeList e) {
        PatientCodeListDto d = new PatientCodeListDto();
        d.id = e.getId();
        d.title = e.getTitle();
        d.order = e.getOrderIndex();
        d.isDefault = e.isDefault();
        d.active = e.isActive();
        d.notes = e.getNotes();
        d.codes = e.getCodes();
        String idValue = e.getFhirId() != null ? e.getFhirId() : ("CL-" + e.getId());
        d.externalId = idValue;
        d.fhirId = idValue;

        PatientCodeListDto.Audit a = new PatientCodeListDto.Audit();
        if (e.getCreatedDate() != null) a.createdDate = DAY.format(e.getCreatedDate().atZone(ZoneId.systemDefault()));
        if (e.getLastModifiedDate() != null) a.lastModifiedDate = DAY.format(e.getLastModifiedDate().atZone(ZoneId.systemDefault()));
        d.audit = a;

        return d;
    }
}
