package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxPartnerCampaignSharedByMeResponseDTO {
    private Integer page;
    private Integer totalRecords;
    private List<TelnyxPartnerCampaignSharedByMeDTO> records;
}
