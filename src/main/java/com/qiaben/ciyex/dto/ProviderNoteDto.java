package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.converter.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import lombok.Data;

import java.util.Map;

@Data
public class ProviderNoteDto {
    private Long id;
    private String externalId;     // optional FHIR Composition.id
    private Long orgId;
    private Long patientId;
    private Long encounterId;
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "sections_json", columnDefinition = "TEXT")
    private Map<String, Object> sections;
// instead of String sectionsJson

    // FHIR Composition-ish fields (see vv.txt)
    private String noteTitle;      // e.g., "SOAP Note":contentReference[oaicite:1]{index=1}
    private String noteTypeCode;   // e.g., LOINC 11488-4 (Consultation note):contentReference[oaicite:2]{index=2}
    private String noteStatus;     // preliminary|final|amended:contentReference[oaicite:3]{index=3}
    private String authorPractitionerId; // internal id (maps to Practitioner):contentReference[oaicite:4]{index=4}
    private String noteDateTime;   // ISO string when created

    // Human-readable body (can be whole note narrative)
    private String narrative;      // free text narrative:contentReference[oaicite:5]{index=5}

    // Sections as JSON string (Subjective/Objective/Assessment/Plan, etc.)
    // Example:
    // {
    //   "sections":[
    //     {"title":"Subjective","code":"61150-9","text":"...", "entries":[]},
    //     {"title":"Objective","code":"61149-1","text":"...", "entries":[{"type":"Observation","id":"201"}]},
    //     {"title":"Assessment","code":"51848-0","text":"..."},
    //     {"title":"Plan","code":"18776-5","text":"..."}
    //   ]
    // }
    private String sectionsJson;   // persisted as TEXT/JSONB:contentReference[oaicite:6]{index=6}

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }


}
