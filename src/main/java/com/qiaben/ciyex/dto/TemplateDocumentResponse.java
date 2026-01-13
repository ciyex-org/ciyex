package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.dto.TemplateContext;
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
