package com.qiaben.ciyex.service;


import com.qiaben.ciyex.dto.ProcedureDto;
import com.qiaben.ciyex.entity.Procedure;
import com.qiaben.ciyex.repository.ProcedureRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.storage.fhir.FhirExternalProcedureStorage;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.repository.PatientRepository;
import com.qiaben.ciyex.repository.EncounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcedureService {
    private final ProcedureRepository repo;
    private final EncounterService encounterService;
    private final PatientBillingService billingService;
    private final PatientRepository patientRepository;
    private final EncounterRepository encounterRepository;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    @Autowired(required = false)
    private FhirExternalProcedureStorage fhirStorage;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public ProcedureDto create(Long patientId, Long encounterId, ProcedureDto in) {
        boolean patientExists = patientRepository.existsById(patientId);
        boolean encounterExists = encounterRepository.findByIdAndPatientId(encounterId, patientId).isPresent();
        if (!patientExists && !encounterExists) {
            throw new IllegalArgumentException("Patient and Encounter not found");
        } else if (!patientExists) {
            throw new IllegalArgumentException("Patient not found");
        } else if (!encounterExists) {
            throw new IllegalArgumentException("Encounter not found");
        }
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        // If codeItems exist, use first item for main procedure fields
        if (in.getCodeItems() != null && !in.getCodeItems().isEmpty()) {
            ProcedureDto.CodeItem firstItem = in.getCodeItems().get(0);
            in.setCpt4(firstItem.getCpt4());
            in.setDescription(firstItem.getDescription());
            in.setUnits(firstItem.getUnits());
            in.setRate(firstItem.getRate());
            in.setRelatedIcds(firstItem.getRelatedIcds());
            in.setModifier1(firstItem.getModifier1());
        }

        Procedure p = Procedure.builder()
            .patientId(patientId)
            .encounterId(encounterId)
            .cpt4(in.getCpt4())
            .description(in.getDescription())
            .units(in.getUnits())
            .rate(in.getRate())
            .relatedIcds(in.getRelatedIcds())
            .hospitalBillingStart(in.getHospitalBillingStart())
            .hospitalBillingEnd(in.getHospitalBillingEnd())
            .modifier1(in.getModifier1())

            .note(in.getNote())
            .priceLevelTitle(in.getPriceLevelTitle())
            .priceLevelId(in.getPriceLevelId())
            .providername(in.getProvidername())
            .build();

        // Handle multiple code items
        if (in.getCodeItems() != null && !in.getCodeItems().isEmpty()) {
            try {
                String codeItemsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(in.getCodeItems());
                p.setCodeItems(codeItemsJson);
            } catch (Exception e) {
                log.error("Failed to serialize code items", e);
            }
        }

        return saveProcedureWithFhirAndInvoice(patientId, p);
    }

    public List<ProcedureDto> createMultiple(Long patientId, Long encounterId, ProcedureDto request) {
        boolean patientExists = patientRepository.existsById(patientId);
        boolean encounterExists = encounterRepository.findByIdAndPatientId(encounterId, patientId).isPresent();
        if (!patientExists && !encounterExists) {
            throw new IllegalArgumentException("Patient and Encounter not found");
        } else if (!patientExists) {
            throw new IllegalArgumentException("Patient not found");
        } else if (!encounterExists) {
            throw new IllegalArgumentException("Encounter not found");
        }
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        List<ProcedureDto> createdProcedures = new java.util.ArrayList<>();
        List<ProcedureDto> itemsToProcess = request.getCodeItems() != null ? 
            java.util.Collections.singletonList(request) : java.util.Collections.emptyList();
        
        for (ProcedureDto procedureItem : itemsToProcess) {
            Procedure p = Procedure.builder()
                .patientId(patientId)
                .encounterId(encounterId)
                .cpt4(procedureItem.getCpt4())
                .description(procedureItem.getDescription())
                .units(procedureItem.getUnits())
                .rate(procedureItem.getRate())
                .relatedIcds(procedureItem.getRelatedIcds())
                .hospitalBillingStart(procedureItem.getHospitalBillingStart())
                .hospitalBillingEnd(procedureItem.getHospitalBillingEnd())
                .modifier1(procedureItem.getModifier1())

                .note(procedureItem.getNote())
                .priceLevelTitle(procedureItem.getPriceLevelTitle())
                .priceLevelId(procedureItem.getPriceLevelId())
                .providername(procedureItem.getProvidername())
                .build();

            ProcedureDto created = saveProcedureWithFhirAndInvoice(patientId, p);
            createdProcedures.add(created);
        }
        return createdProcedures;
    }

    private ProcedureDto saveProcedureWithFhirAndInvoice(Long patientId, Procedure p) {
        final Procedure saved = repo.save(p);

        String storageType = configProvider.getStorageTypeForCurrentOrg();
        log.info("Procedure create - storageType for current org: {}", storageType);

        if (storageType != null) {
            try {
                log.info("Attempting FHIR sync for Procedure ID: {}", saved.getId());
                ExternalStorage<ProcedureDto> ext = storageResolver.resolve(ProcedureDto.class);
                log.info("Resolved external storage: {}", ext.getClass().getName());

                ProcedureDto snapshot = mapToDto(saved);
                String externalId = ext.create(snapshot);
                log.info("FHIR create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    saved.setExternalId(externalId);
                    repo.save(saved);
                    log.info("Created FHIR resource for Procedure ID: {} with externalId: {}", saved.getId(), externalId);
                } else {
                    log.warn("FHIR create returned null or empty externalId for Procedure ID: {}", saved.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to sync Procedure to external storage", ex);
            }
        } else if (fhirStorage != null) {
            try {
                log.info("No storage type configured, falling back to direct FHIR storage for Procedure ID: {}", saved.getId());
                ProcedureDto snapshot = mapToDto(saved);
                String externalId = fhirStorage.create(snapshot);
                log.info("FHIR fallback create returned externalId: {}", externalId);

                if (externalId != null && !externalId.isEmpty()) {
                    saved.setExternalId(externalId);
                    repo.save(saved);
                    log.info("Created FHIR resource (fallback) for Procedure ID: {} with externalId: {}", saved.getId(), externalId);
                }
            } catch (Exception ex) {
                log.error("Failed to sync Procedure to external storage (fallback)", ex);
            }
        } else {
            log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Procedure ID: {}", saved.getId());
        }

        if (saved.getExternalId() == null) {
            String generatedId = "P-" + System.currentTimeMillis();
            saved.setExternalId(generatedId);
            saved.setFhirId(generatedId);
            repo.save(saved);
            log.info("Auto-generated externalId: {}", generatedId);
        } else {
            saved.setFhirId(saved.getExternalId());
            repo.save(saved);
        }

        // Automatically create invoice for the new procedure
        try {
            List<PatientBillingService.ProcedureLineRequest> procedureLines = new java.util.ArrayList<>();
            
            // If codeItems exist, create invoice lines for each
            if (saved.getCodeItems() != null && !saved.getCodeItems().isEmpty()) {
                try {
                    List<ProcedureDto.CodeItem> codeItems = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(saved.getCodeItems(), new com.fasterxml.jackson.core.type.TypeReference<List<ProcedureDto.CodeItem>>(){});
                    
                    for (ProcedureDto.CodeItem item : codeItems) {
                        java.math.BigDecimal rateValue;
                        try {
                            rateValue = new java.math.BigDecimal(item.getRate());
                        } catch (Exception ex) {
                            rateValue = java.math.BigDecimal.ZERO;
                            log.warn("Invalid rate format for code item: {}. Defaulting to 0.", item.getCpt4());
                        }
                        procedureLines.add(new PatientBillingService.ProcedureLineRequest(
                            item.getCpt4(),
                            item.getDescription(),
                            rateValue
                        ));
                    }
                } catch (Exception ex) {
                    log.error("Failed to parse code items for invoice", ex);
                }
            } else {
                // Fallback to single procedure line
                java.math.BigDecimal rateValue;
                try {
                    rateValue = new java.math.BigDecimal(saved.getRate());
                } catch (Exception ex) {
                    rateValue = java.math.BigDecimal.ZERO;
                    log.warn("Invalid rate format for procedure ID: {}. Defaulting to 0.", saved.getId());
                }
                procedureLines.add(new PatientBillingService.ProcedureLineRequest(
                    saved.getCpt4(),
                    saved.getDescription(),
                    rateValue
                ));
            }
            
            PatientBillingService.CreateInvoiceRequest invoiceRequest = new PatientBillingService.CreateInvoiceRequest(
                    saved.getProvidername(),
                    saved.getHospitalBillingStart(),
                    procedureLines
            );
            billingService.createInvoiceFromProcedure(patientId, invoiceRequest);
            log.info("Invoice automatically created for procedure ID: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to create invoice for procedure ID: {}", saved.getId(), e);
        }

        return mapToDto(saved);
    }

    public ProcedureDto update(Long patientId, Long encounterId, Long id, ProcedureDto in) {
        // Validate patient and encounter existence
        boolean patientExists = patientRepository.existsById(patientId);
        boolean encounterExists = encounterRepository.findByIdAndPatientId(encounterId, patientId).isPresent();
        if (!patientExists && !encounterExists) {
            throw new IllegalArgumentException("Patient and Encounter not found");
        } else if (!patientExists) {
            throw new IllegalArgumentException("Patient not found");
        } else if (!encounterExists) {
            throw new IllegalArgumentException("Encounter not found");
        }
        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        Procedure p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Procedure not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));

        // If codeItems exist, use first item for main procedure fields
        if (in.getCodeItems() != null && !in.getCodeItems().isEmpty()) {
            ProcedureDto.CodeItem firstItem = in.getCodeItems().get(0);
            in.setCpt4(firstItem.getCpt4());
            in.setDescription(firstItem.getDescription());
            in.setUnits(firstItem.getUnits());
            in.setRate(firstItem.getRate());
            in.setRelatedIcds(firstItem.getRelatedIcds());
            in.setModifier1(firstItem.getModifier1());
        }

        p.setCpt4(in.getCpt4());
        p.setDescription(in.getDescription());
        p.setUnits(in.getUnits());
        p.setRate(in.getRate());
        p.setRelatedIcds(in.getRelatedIcds());
        p.setHospitalBillingStart(in.getHospitalBillingStart());
        p.setHospitalBillingEnd(in.getHospitalBillingEnd());
        p.setModifier1(in.getModifier1());
        p.setNote(in.getNote());
        p.setPriceLevelTitle(in.getPriceLevelTitle());
        p.setPriceLevelId(in.getPriceLevelId());
        p.setProvidername(in.getProvidername());

        // Handle multiple code items
        if (in.getCodeItems() != null && !in.getCodeItems().isEmpty()) {
            try {
                String codeItemsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(in.getCodeItems());
                p.setCodeItems(codeItemsJson);
            } catch (Exception e) {
                log.error("Failed to serialize code items", e);
            }
        }

        final Procedure updated = repo.save(p);

        // Step 7: Optional external FHIR sync
        if (updated.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("Procedure update - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR sync for Procedure ID: {}", updated.getId());
                    ExternalStorage<ProcedureDto> ext = storageResolver.resolve(ProcedureDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    ProcedureDto snapshot = mapToDto(updated);
                    ext.update(snapshot, updated.getExternalId());
                    log.info("Updated FHIR resource for Procedure ID: {} with externalId: {}", updated.getId(), updated.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Procedure update to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for Procedure ID: {}", updated.getId());
                    ProcedureDto snapshot = mapToDto(updated);
                    fhirStorage.update(snapshot, updated.getExternalId());
                    log.info("Updated FHIR resource (fallback) for Procedure ID: {} with externalId: {}", updated.getId(), updated.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Procedure update to external storage (fallback)", ex);
                }
            } else {
                log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Procedure ID: {}", updated.getId());
            }
        }

        return mapToDto(updated);
    }

    public void delete(Long patientId, Long encounterId, Long id) {
        // Validate patient and encounter existence
        boolean patientExists = patientRepository.existsById(patientId);
        boolean encounterExists = encounterRepository.findByIdAndPatientId(encounterId, patientId).isPresent();
        if (!patientExists && !encounterExists) {
            throw new IllegalArgumentException("Patient and Encounter not found");
        } else if (!patientExists) {
            throw new IllegalArgumentException("Patient not found");
        } else if (!encounterExists) {
            throw new IllegalArgumentException("Encounter not found");
        }
        // Check if encounter is signed - prevent modification
        encounterService.validateEncounterNotSigned(encounterId, patientId);

        Procedure p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Procedure not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));

        final Procedure toDelete = p;
        // Step 7: Optional external FHIR sync
        if (toDelete.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            log.info("Procedure delete - storageType for current org: {}", storageType);

            if (storageType != null) {
                try {
                    log.info("Attempting FHIR sync for Procedure ID: {}", toDelete.getId());
                    ExternalStorage<ProcedureDto> ext = storageResolver.resolve(ProcedureDto.class);
                    log.info("Resolved external storage: {}", ext.getClass().getName());

                    ext.delete(toDelete.getExternalId());
                    log.info("Deleted FHIR resource for Procedure ID: {} with externalId: {}", toDelete.getId(), toDelete.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Procedure delete to external storage", ex);
                }
            } else if (fhirStorage != null) {
                try {
                    log.info("No storage type configured, falling back to direct FHIR storage for Procedure ID: {}", toDelete.getId());
                    fhirStorage.delete(toDelete.getExternalId());
                    log.info("Deleted FHIR resource (fallback) for Procedure ID: {} with externalId: {}", toDelete.getId(), toDelete.getExternalId());
                } catch (Exception ex) {
                    log.error("Failed to sync Procedure delete to external storage (fallback)", ex);
                }
            } else {
                log.warn("No storage type configured for current org and no FHIR fallback available - skipping FHIR sync for Procedure ID: {}", toDelete.getId());
            }
        }

        repo.delete(toDelete);
    }

    public ProcedureDto getOne(Long patientId, Long encounterId, Long id) {
        Procedure p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Procedure not found for Patient ID: %d, Encounter ID: %d, ID: %d", patientId, encounterId, id)
                ));
        return mapToDto(p);
    }

    public List<ProcedureDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
    }

    public List<ProcedureDto> getAllByEncounter(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    private ProcedureDto mapToDto(Procedure e) {
        ProcedureDto dto = new ProcedureDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setFhirId(e.getFhirId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());

        dto.setCpt4(e.getCpt4());
        dto.setDescription(e.getDescription());
        dto.setUnits(e.getUnits());
        dto.setRate(e.getRate());
        dto.setRelatedIcds(e.getRelatedIcds());
        dto.setHospitalBillingStart(e.getHospitalBillingStart());
        dto.setHospitalBillingEnd(e.getHospitalBillingEnd());
        dto.setModifier1(e.getModifier1());

        dto.setNote(e.getNote());
        dto.setPriceLevelId(e.getPriceLevelId());
        dto.setPriceLevelTitle(e.getPriceLevelTitle());
        dto.setProvidername(e.getProvidername());
        
        // Deserialize code items
        if (e.getCodeItems() != null && !e.getCodeItems().isEmpty()) {
            try {
                List<ProcedureDto.CodeItem> codeItems = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(e.getCodeItems(), new com.fasterxml.jackson.core.type.TypeReference<List<ProcedureDto.CodeItem>>(){});
                dto.setCodeItems(codeItems);
            } catch (Exception ex) {
                log.error("Failed to deserialize code items", ex);
            }
        }
        
        ProcedureDto.Audit a = new ProcedureDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);

        return dto;
    }
}
