package org.ciyex.ehr.dto;

import org.ciyex.ehr.dto.TemplateContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class TemplateDocumentUpsertRequest {
    @NotNull public TemplateContext context;
    @NotBlank @Size(max = 300) public String name;
    @NotBlank public String content;               // full HTML
    @NotNull public Map<String, Object> options;   // JSONB
}


