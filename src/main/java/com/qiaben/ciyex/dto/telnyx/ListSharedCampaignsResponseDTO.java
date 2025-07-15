package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class ListSharedCampaignsResponseDTO {
    private List<SharedCampaignDTO> records;
    private Integer page;
    private Integer totalRecords;
}
