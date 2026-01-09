package com.qiaben.ciyex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * FHIR-only Patient History Service.
 * Uses FHIR Basic resource for storing patient history JSON.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientHistoryService {
    
    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final ObjectMapper objectMapper;

    private static final String HISTORY_TYPE_SYSTEM = "http://ciyex.com/fhir/resource-type";
    private static final String HISTORY_TYPE_CODE = "patient-history";
    private static final String EXT_PATIENT_ID = "http://ciyex.com/fhir/StructureDefinition/patient-id";
    private static final String EXT_HISTORY_DATA = "http://ciyex.com/fhir/StructureDefinition/history-data";

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }
    
    public Object saveHistory(Long patientId, Object historyData) {
        try {
            Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
            List<Basic> histories = fhirClientService.extractResources(bundle, Basic.class).stream()
                    .filter(this::isPatientHistory)
                    .filter(b -> patientId.toString().equals(getStringExt(b, EXT_PATIENT_ID)))
                    .toList();

            String historyJson = objectMapper.writeValueAsString(historyData);

            if (histories.isEmpty()) {
                // Create new
                Basic basic = new Basic();
                CodeableConcept code = new CodeableConcept();
                code.addCoding().setSystem(HISTORY_TYPE_SYSTEM).setCode(HISTORY_TYPE_CODE).setDisplay("Patient History");
                basic.setCode(code);
                basic.addExtension(new Extension(EXT_PATIENT_ID, new StringType(patientId.toString())));
                basic.addExtension(new Extension(EXT_HISTORY_DATA, new StringType(historyJson)));
                fhirClientService.create(basic, getPracticeId());
                log.info("Created patient history in FHIR for patient: {}", patientId);
            } else {
                // Update existing
                Basic basic = histories.get(0);
                basic.getExtension().removeIf(e -> EXT_HISTORY_DATA.equals(e.getUrl()));
                basic.addExtension(new Extension(EXT_HISTORY_DATA, new StringType(historyJson)));
                fhirClientService.update(basic, getPracticeId());
                log.info("Updated patient history in FHIR for patient: {}", patientId);
            }

            return historyData;
        } catch (Exception e) {
            log.error("Failed to save history for patient {}", patientId, e);
            throw new RuntimeException("Failed to save patient history", e);
        }
    }
    
    public Object getHistory(Long patientId) {
        try {
            Bundle bundle = fhirClientService.search(Basic.class, getPracticeId());
            List<Basic> histories = fhirClientService.extractResources(bundle, Basic.class).stream()
                    .filter(this::isPatientHistory)
                    .filter(b -> patientId.toString().equals(getStringExt(b, EXT_PATIENT_ID)))
                    .toList();

            if (histories.isEmpty()) {
                return new java.util.HashMap<>();
            }

            String historyJson = getStringExt(histories.get(0), EXT_HISTORY_DATA);
            if (historyJson == null || historyJson.isEmpty()) {
                return new java.util.HashMap<>();
            }

            return objectMapper.readValue(historyJson, Object.class);
        } catch (Exception e) {
            log.error("Failed to retrieve history for patient {}", patientId, e);
            throw new RuntimeException("Failed to retrieve patient history", e);
        }
    }

    private boolean isPatientHistory(Basic basic) {
        if (!basic.hasCode()) return false;
        return basic.getCode().getCoding().stream()
                .anyMatch(c -> HISTORY_TYPE_SYSTEM.equals(c.getSystem()) && HISTORY_TYPE_CODE.equals(c.getCode()));
    }

    private String getStringExt(Basic basic, String url) {
        Extension ext = basic.getExtensionByUrl(url);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}