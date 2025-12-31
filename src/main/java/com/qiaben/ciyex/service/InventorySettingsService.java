package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InventorySettingsDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FHIR-only Inventory Settings Service.
 * Uses FHIR Basic resource for storing inventory settings.
 * No local database storage - all data stored in FHIR server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventorySettingsService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;

    private static final String SETTINGS_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String SETTINGS_TYPE_CODE = "inventory-settings";
    private static final String EXT_LOW_STOCK_ALERTS = "http://ciyex.com/fhir/StructureDefinition/low-stock-alerts";
    private static final String EXT_AUTO_REORDER = "http://ciyex.com/fhir/StructureDefinition/auto-reorder-suggestions";
    private static final String EXT_CRITICAL_LOW_PCT = "http://ciyex.com/fhir/StructureDefinition/critical-low-percentage";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    // GET SETTINGS (creates default if not exists)
    public InventorySettingsDto getSettings() {
        log.debug("Getting inventory settings");

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> settings = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isInventorySettings)
                .toList();

        if (!settings.isEmpty()) {
            return fromFhirBasic(settings.get(0));
        }

        // Create default settings
        log.info("No inventory settings found, creating defaults");
        InventorySettingsDto defaults = InventorySettingsDto.builder()
                .lowStockAlerts(true)
                .autoReorderSuggestions(false)
                .criticalLowPercentage(10)
                .build();

        return createSettings(defaults);
    }

    // UPDATE SETTINGS
    public InventorySettingsDto updateSettings(InventorySettingsDto dto) {
        log.debug("Updating inventory settings");

        Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
        List<Basic> settings = fhirClientService.extractResources(bundle, Basic.class).stream()
                .filter(this::isInventorySettings)
                .toList();

        if (settings.isEmpty()) {
            // Create new settings
            return createSettings(dto);
        }

        // Update existing
        Basic existing = settings.get(0);
        String fhirId = existing.getIdElement().getIdPart();

        Basic basic = toFhirBasic(dto);
        basic.setId(fhirId);
        fhirClientService.update(basic, getPracticeId());

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setAudit(createAudit());

        log.info("Updated inventory settings with FHIR ID: {}", fhirId);
        return dto;
    }

    private InventorySettingsDto createSettings(InventorySettingsDto dto) {
        Basic basic = toFhirBasic(dto);
        var outcome = fhirClientService.create(basic, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setId((long) Math.abs(fhirId.hashCode()));
        dto.setFhirId(fhirId);
        dto.setAudit(createAudit());

        log.info("Created inventory settings with FHIR ID: {}", fhirId);
        return dto;
    }

    // -------- FHIR Mapping --------

    private Basic toFhirBasic(InventorySettingsDto dto) {
        Basic basic = new Basic();

        // Code to identify as inventory settings
        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(SETTINGS_TYPE_SYSTEM).setCode(SETTINGS_TYPE_CODE).setDisplay("Inventory Settings");
        basic.setCode(code);

        // Low stock alerts
        basic.addExtension(new Extension(EXT_LOW_STOCK_ALERTS, new BooleanType(dto.isLowStockAlerts())));

        // Auto reorder suggestions
        basic.addExtension(new Extension(EXT_AUTO_REORDER, new BooleanType(dto.isAutoReorderSuggestions())));

        // Critical low percentage
        basic.addExtension(new Extension(EXT_CRITICAL_LOW_PCT, new IntegerType(dto.getCriticalLowPercentage())));

        return basic;
    }

    private InventorySettingsDto fromFhirBasic(Basic basic) {
        String fhirId = basic.getIdElement().getIdPart();

        boolean lowStockAlerts = true;
        Extension lsaExt = basic.getExtensionByUrl(EXT_LOW_STOCK_ALERTS);
        if (lsaExt != null && lsaExt.getValue() instanceof BooleanType) {
            lowStockAlerts = ((BooleanType) lsaExt.getValue()).booleanValue();
        }

        boolean autoReorder = false;
        Extension arExt = basic.getExtensionByUrl(EXT_AUTO_REORDER);
        if (arExt != null && arExt.getValue() instanceof BooleanType) {
            autoReorder = ((BooleanType) arExt.getValue()).booleanValue();
        }

        int criticalLowPct = 10;
        Extension clpExt = basic.getExtensionByUrl(EXT_CRITICAL_LOW_PCT);
        if (clpExt != null && clpExt.getValue() instanceof IntegerType) {
            criticalLowPct = ((IntegerType) clpExt.getValue()).getValue();
        }

        return InventorySettingsDto.builder()
                .id((long) Math.abs(fhirId.hashCode()))
                .fhirId(fhirId)
                .lowStockAlerts(lowStockAlerts)
                .autoReorderSuggestions(autoReorder)
                .criticalLowPercentage(criticalLowPct)
                .audit(createAudit())
                .build();
    }

    private boolean isInventorySettings(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> SETTINGS_TYPE_SYSTEM.equals(c.getSystem()) && SETTINGS_TYPE_CODE.equals(c.getCode()));
    }

    private InventorySettingsDto.Audit createAudit() {
        InventorySettingsDto.Audit audit = new InventorySettingsDto.Audit();
        audit.setCreatedDate(LocalDate.now().format(DAY));
        audit.setLastModifiedDate(LocalDate.now().format(DAY));
        return audit;
    }
}
