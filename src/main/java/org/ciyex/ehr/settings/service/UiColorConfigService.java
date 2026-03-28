package org.ciyex.ehr.settings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.settings.entity.UiColorConfig;
import org.ciyex.ehr.settings.repository.UiColorConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UiColorConfigService {

    private final UiColorConfigRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 20 curated, distinct colors — bg + darker border.
     * Round-robin assignment ensures variety within each category.
     */
    private static final String[][] COLOR_PALETTE = {
            {"#4F46E5", "#4338CA"},  // Indigo
            {"#2563EB", "#1D4ED8"},  // Blue
            {"#0891B2", "#0E7490"},  // Cyan
            {"#059669", "#047857"},  // Emerald
            {"#16A34A", "#15803D"},  // Green
            {"#CA8A04", "#A16207"},  // Yellow
            {"#EA580C", "#C2410C"},  // Orange
            {"#DC2626", "#B91C1C"},  // Red
            {"#DB2777", "#BE185D"},  // Pink
            {"#9333EA", "#7E22CE"},  // Purple
            {"#7C3AED", "#6D28D9"},  // Violet
            {"#0D9488", "#0F766E"},  // Teal
            {"#D97706", "#B45309"},  // Amber
            {"#E11D48", "#BE123C"},  // Rose
            {"#8B5CF6", "#7C3AED"},  // Lavender
            {"#06B6D4", "#0891B2"},  // Sky
            {"#F59E0B", "#D97706"},  // Gold
            {"#10B981", "#059669"},  // Mint
            {"#6366F1", "#4F46E5"},  // Slate Blue
            {"#EC4899", "#DB2777"},  // Hot Pink
    };

    // ---- Query delegates ----

    public List<UiColorConfig> findAll(String orgId) {
        return repository.findByOrgId(orgId);
    }

    public List<UiColorConfig> findByCategory(String orgId, String category) {
        return repository.findByOrgIdAndCategory(orgId, category);
    }

    public Optional<UiColorConfig> findByEntity(String orgId, String category, String entityKey) {
        return repository.findByOrgIdAndCategoryAndEntityKey(orgId, category, entityKey);
    }

    // ---- Bulk save (used by CalendarColorSettings UI) ----

    @Transactional
    public List<UiColorConfig> bulkSave(String orgId, List<ColorConfigRequest> requests) {
        for (ColorConfigRequest req : requests) {
            UiColorConfig config = repository
                    .findByOrgIdAndCategoryAndEntityKey(orgId, req.category(), req.entityKey())
                    .orElse(UiColorConfig.builder()
                            .orgId(orgId)
                            .category(req.category())
                            .entityKey(req.entityKey())
                            .build());

            config.setEntityLabel(req.entityLabel());
            config.setBgColor(req.bgColor());
            config.setBorderColor(req.borderColor());
            config.setTextColor(req.textColor());
            repository.save(config);
        }
        return repository.findByOrgId(orgId);
    }

    // ---- Auto-assign ----

    /**
     * Auto-assign a color from the palette if the entity doesn't have one yet.
     * Uses round-robin within the category so consecutive entities get different colors.
     */
    public void autoAssignColorIfMissing(String orgId, String category, String entityKey, String entityLabel) {
        if (repository.findByOrgIdAndCategoryAndEntityKey(orgId, category, entityKey).isPresent()) {
            return;
        }

        int count = repository.findByOrgIdAndCategory(orgId, category).size();
        int idx = count % COLOR_PALETTE.length;
        String[] colors = COLOR_PALETTE[idx];

        repository.save(UiColorConfig.builder()
                .orgId(orgId)
                .category(category)
                .entityKey(entityKey)
                .entityLabel(entityLabel)
                .bgColor(colors[0])
                .borderColor(colors[1])
                .textColor("#FFFFFF")
                .build());

        log.info("Auto-assigned color {} to {}:{} for org {}", colors[0], category, entityKey, orgId);
    }

    /**
     * Parse the fieldConfig JSON for the "appointments" tab, find the appointmentType
     * field's options array, and auto-assign a color for each option value.
     */
    public void autoAssignColorsForAppointmentOptions(String orgId, Object fieldConfigObj) {
        try {
            JsonNode root;
            if (fieldConfigObj instanceof String s) {
                root = objectMapper.readTree(s);
            } else {
                root = objectMapper.valueToTree(fieldConfigObj);
            }

            JsonNode sections = root.path("sections");
            if (!sections.isArray()) return;

            for (JsonNode section : sections) {
                JsonNode fields = section.path("fields");
                if (!fields.isArray()) continue;

                for (JsonNode field : fields) {
                    String key = field.path("key").asText("");
                    if (!"appointmentType".equals(key)) continue;

                    JsonNode options = field.path("options");
                    if (!options.isArray()) continue;

                    for (JsonNode opt : options) {
                        String value;
                        String label;
                        if (opt.isTextual()) {
                            // Plain string option: "Consultation"
                            value = opt.asText("");
                            label = value;
                        } else {
                            // Object option: {"value": "ROUTINE", "label": "Routine"}
                            value = opt.path("value").asText("");
                            label = opt.path("label").asText(value);
                        }
                        if (!value.isBlank()) {
                            autoAssignColorIfMissing(orgId, "visit-type", value, label);
                        }
                    }
                    return; // found the field, done
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse appointment type options for auto-color assignment", e);
        }
    }

    // ---- DTO ----

    public record ColorConfigRequest(
            String category,
            String entityKey,
            String entityLabel,
            String bgColor,
            String borderColor,
            String textColor
    ) {}
}
