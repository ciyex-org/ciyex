package org.ciyex.ehr.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CDS Hooks Card per HL7 CDS Hooks specification.
 * Returned by CDS services as clinical decision support recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdsCard {
    private String uuid;
    private String summary;
    /** "info", "warning", "critical" */
    private String indicator;
    private String detail;
    private Source source;
    private List<Suggestion> suggestions;
    private List<Link> links;
    private boolean selectionBehavior;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        private String label;
        private String url;
        private String icon;
        private String topic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Suggestion {
        private String label;
        private String uuid;
        private boolean isRecommended;
        private List<Action> actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Action {
        private String type;
        private String description;
        private Object resource;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String label;
        private String url;
        private String type;
        private String appContext;
    }
}
