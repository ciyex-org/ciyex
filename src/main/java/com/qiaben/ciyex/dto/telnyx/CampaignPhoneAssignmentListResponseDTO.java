package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignPhoneAssignmentListResponseDTO {
    private List<CampaignPhoneAssignmentRecordDTO> records;
    private Integer page;
    private Integer totalRecords;
}
