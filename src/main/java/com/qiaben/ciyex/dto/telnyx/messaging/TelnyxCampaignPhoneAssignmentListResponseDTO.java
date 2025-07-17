package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TelnyxCampaignPhoneAssignmentListResponseDTO {
    private List<TelnyxCampaignPhoneAssignmentRecordDTO> records;
    private Integer page;
    private Integer totalRecords;
}
