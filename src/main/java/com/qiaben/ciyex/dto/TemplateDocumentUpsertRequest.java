package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.TemplateContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class TemplateDocumentUpsertRequest {
    @NotNull public Long orgId;
    @NotNull public TemplateContext context;
    @NotBlank @Size(max = 300) public String name;
    @NotBlank public String content;               // full HTML
    @NotNull public Map<String, Object> options;   // JSONB
}


