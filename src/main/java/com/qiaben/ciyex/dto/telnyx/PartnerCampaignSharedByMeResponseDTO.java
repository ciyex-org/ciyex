package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class PartnerCampaignSharedByMeResponseDTO {
    private Integer page;
    private Integer totalRecords;
    private List<PartnerCampaignSharedByMeDTO> records;
}
