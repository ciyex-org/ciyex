package org.ciyex.ehr.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated CDS Hooks response combining cards from all CDS services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsHookResponse {
    /** All cards from all CDS services, sorted by indicator severity */
    private List<CdsCard> cards;
    /** Number of services that were invoked */
    private int servicesInvoked;
    /** Number of services that failed (timeout/error) */
    private int servicesFailed;
}
