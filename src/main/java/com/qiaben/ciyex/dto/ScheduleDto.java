package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO carries ALL schedule details to/from the API.
 * Only id/orgId/providerId/externalId are stored locally; the rest comes from External (FHIR) storage.
 */
@Data
public class ScheduleDto {
    // Locally stored identifiers
    private Long id; // DB id // tenant (from RequestContext typically)
    private Long providerId; // internal provider/practitioner id
    private String externalId; // FHIR Schedule id

    // one-time window
    private String start; // ISO-8601
    private String end; // ISO-8601

    // actors
    private List<String> actorReferences; // e.g., ["Practitioner/123", "Location/456"]

    // recurrence (non-native FHIR fields, persisted as extensions)
    private Recurrence recurrence; // null when one-time only
    private String timezone; // IANA tz for preview/slot generation

    // optional display metadata (pulled from external)
    private String serviceCategory;
    private String serviceType;
    private String specialty;
    private String status; // active/inactive
    private String comment;

    private Audit audit; // audit timestamps from local persistence

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }

    @Data
    public static class Recurrence {
        private String frequency; // DAILY | WEEKLY | MONTHLY
        private Integer interval; // Every n units
        private List<String> byWeekday; // ["MO","TU","WE",...]
        private String startDate; // yyyy-MM-dd
        private String endDate; // yyyy-MM-dd (optional)
        private String startTime; // HH:mm
        private String endTime; // HH:mm (optional)
        private Integer maxOccurrences; // for preview and optional slot generation
        private String locationId; // optional override (Location/{id})
    }
}