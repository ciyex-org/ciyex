package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.TemplateContext;
import java.time.Instant;
import java.util.Map;

public class TemplateDocumentResponse {
    public Long id;
    public Long orgId;
    public TemplateContext context;
    public String name;
    public String content;
    public Map<String, Object> options;
    public Instant createdAt;
    public Instant updatedAt;
}