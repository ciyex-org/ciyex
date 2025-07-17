package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxListSharedCampaignsResponseDTO {
    private List<TelnyxSharedCampaignDTO> records;
    private Integer page;
    private Integer totalRecords;
}
