package org.ciyex.ehr.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to record an app usage event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordUsageRequest {
    @NotBlank
    private String appSlug;
    @NotBlank
    private String eventType;
    private String eventDetail;
    private String userId;
    private Integer quantity;
}
