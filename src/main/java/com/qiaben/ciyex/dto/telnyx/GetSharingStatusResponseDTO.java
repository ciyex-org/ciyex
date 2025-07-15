package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class GetSharingStatusResponseDTO {

    private SharingDetail sharedByMe;
    private SharingDetail sharedWithMe;

    @Data
    public static class SharingDetail {
        private String downstreamCnpId;
        private String sharedDate;
        private String sharingStatus;
        private String statusDate;
        private String upstreamCnpId;
    }
}
