package org.ciyex.ehr.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Summary of app usage events for a given period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUsageSummary {
    private String appSlug;
    private int periodDays;
    private long totalEvents;
    /** Breakdown by event type: e.g., {"app_launch": 42, "cds_hook_invocation": 15} */
    private Map<String, Long> eventBreakdown;
}
