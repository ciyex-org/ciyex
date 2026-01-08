package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MaintenanceDto;
import com.qiaben.ciyex.exception.ResourceNotFoundException;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FHIR-only Maintenance Service.
 * Uses FHIR Basic resource for storing maintenance records.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MaintenanceService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String MAINT_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String MAINT_TYPE_CODE = "maintenance";
    private static final String EXT_EQUIPMENT = "http://ciyex.com/fhir/StructureDefinition/equipment";
    private static final String EXT_CATEGORY = "http://ciyex.com/fhir/StructureDefinition/category";
    private static final String EXT_LOCATION = "http://ciyex.com/fhir/StructureDefinition/location";
    private static final String EXT_DUE_DATE = "http://ciyex.com/fhir/StructureDefinition/due-date";
    private static final String EXT_LAST_SERVICE = "http://ciyex.com/fhir/StructureDefinition/last-service-date";
    private static final String EXT_ASSIGNEE = "http://ciyex.com/fhir/StructureDefinition/assignee";
    private static final String EXT_VENDOR = "http://ciyex.com/fhir/StructureDefinition/vendor";
    private static final String EXT_PRIORITY = "http://ciyex.com/fhir/StructureDefinition/priority";
    private static final String EXT_STATUS = "http://ciyex.com/fhir/StructureDefinition/status";
    private static final String EXT_NOTES = "http://ciyex.com/fhir/StructureDefinition/notes";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // CREATE
    public MaintenanceDto create(MaintenanceDto dto) {
        log.debug("Creating FHIR Basic (Maintenance): {}", dto.getEquipment());

        Basic basic = toFhirBasic(dto);
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setAudit(createAudit());

        log.info("Created FHIR Basic (Maintenance) with id: {}", fhirId);
        return dto;
    }

    // GET BY ID
    public MaintenanceDto getById(String fhirId) {
        log.debug("Getting maintenance: {}", fhirId);
        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        if (basic == null) {
            throw new ResourceNotFoundException("Maintenance", "id", fhirId);
        }
        return fromFhirBasic(basic);
    }

    // UPDATE
    public MaintenanceDto update(String fhirId, MaintenanceDto dto) {
        log.debug("Updating maintenance: {}", fhirId);

        Basic basic = toFhirBasic(dto);
        basic.setId(fhirId);
        fhirClientService.update(basic, getPracticeId());

        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        dto.setAudit(createAudit());

        log.info("Updated maintenance with FHIR ID: {}", fhirId);
        return dto;
    }

    // DELETE
    public void delete(String fhirId) {
        log.debug("Deleting maintenance: {}", fhirId);
        fhirClientService.delete(Basic.class, fhirId, getPracticeId());
        log.info("Deleted maintenance with FHIR ID: {}", fhirId);
    }

    // GET ALL
    public List<MaintenanceDto> getAll() {
        log.debug("Getting all maintenance records");
        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());

        return fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isMaintenance)
                .map(this::fromFhirBasic)
                .collect(Collectors.toList());
    }

    // GET ALL (paginated)
    public Page<MaintenanceDto> getAll(Pageable pageable) {
        List<MaintenanceDto> all = getAll();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());

        if (start > all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }

        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    // UPDATE STATUS
    public MaintenanceDto updateStatus(String fhirId, String status) {
        log.debug("Updating status for maintenance {}: {}", fhirId, status);

        Basic basic = fhirClientService.read(Basic.class, fhirId, getPracticeId());
        if (basic == null) {
            throw new ResourceNotFoundException("Maintenance", "id", fhirId);
        }

        basic.getExtension().removeIf(e -> EXT_STATUS.equals(e.getUrl()));
        basic.addExtension(new Extension(EXT_STATUS, new StringType(status)));
        fhirClientService.update(basic, getPracticeId());

        return fromFhirBasic(basic);
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(MaintenanceDto dto) {
        Basic basic = new Basic();

        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(MAINT_TYPE_SYSTEM).setCode(MAINT_TYPE_CODE).setDisplay("Maintenance");
        basic.setCode(code);

        if (dto.getEquipment() != null) {
            basic.addExtension(new Extension(EXT_EQUIPMENT, new StringType(dto.getEquipment())));
        }
        if (dto.getCategory() != null) {
            basic.addExtension(new Extension(EXT_CATEGORY, new StringType(dto.getCategory())));
        }
        if (dto.getLocation() != null) {
            basic.addExtension(new Extension(EXT_LOCATION, new StringType(dto.getLocation())));
        }
        if (dto.getDueDate() != null) {
            basic.addExtension(new Extension(EXT_DUE_DATE, new StringType(dto.getDueDate())));
        }
        if (dto.getLastServiceDate() != null) {
            basic.addExtension(new Extension(EXT_LAST_SERVICE, new StringType(dto.getLastServiceDate())));
        }
        if (dto.getAssignee() != null) {
            basic.addExtension(new Extension(EXT_ASSIGNEE, new StringType(dto.getAssignee())));
        }
        if (dto.getVendor() != null) {
            basic.addExtension(new Extension(EXT_VENDOR, new StringType(dto.getVendor())));
        }
        if (dto.getPriority() != null) {
            basic.addExtension(new Extension(EXT_PRIORITY, new StringType(dto.getPriority())));
        }
        if (dto.getStatus() != null) {
            basic.addExtension(new Extension(EXT_STATUS, new StringType(dto.getStatus())));
        }
        if (dto.getNotes() != null) {
            basic.addExtension(new Extension(EXT_NOTES, new StringType(dto.getNotes())));
        }

        return basic;
    }

    private MaintenanceDto fromFhirBasic(Basic basic) {
        MaintenanceDto dto = new MaintenanceDto();

        String fhirId = basic.getIdElement().getIdPart();
        dto.setId(Long.parseLong(fhirId));
        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);

        dto.setEquipment(getStringExt(basic, EXT_EQUIPMENT));
        dto.setCategory(getStringExt(basic, EXT_CATEGORY));
        dto.setLocation(getStringExt(basic, EXT_LOCATION));
        dto.setDueDate(getStringExt(basic, EXT_DUE_DATE));
        dto.setLastServiceDate(getStringExt(basic, EXT_LAST_SERVICE));
        dto.setAssignee(getStringExt(basic, EXT_ASSIGNEE));
        dto.setVendor(getStringExt(basic, EXT_VENDOR));
        dto.setPriority(getStringExt(basic, EXT_PRIORITY));
        dto.setStatus(getStringExt(basic, EXT_STATUS));
        dto.setNotes(getStringExt(basic, EXT_NOTES));

        dto.setAudit(createAudit());
        return dto;
    }

    private boolean isMaintenance(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> MAINT_TYPE_SYSTEM.equals(c.getSystem()) && MAINT_TYPE_CODE.equals(c.getCode()));
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }

    private MaintenanceDto.Audit createAudit() {
        MaintenanceDto.Audit audit = new MaintenanceDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        return audit;
    }
}
