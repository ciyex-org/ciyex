package org.ciyex.ehr.dto;

import org.ciyex.ehr.dto.TemplateContext;
import java.time.Instant;
import java.util.Map;

public class TemplateDocumentResponse {
    public String id;  // FHIR ID
    public TemplateContext context;
    public String name;
    public String content;
    public Map<String, Object> options;
    public Instant createdAt;
    public Instant updatedAt;
}
