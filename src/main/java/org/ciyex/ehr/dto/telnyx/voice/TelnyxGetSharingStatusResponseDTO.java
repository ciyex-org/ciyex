package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxGetSharingStatusResponseDTO {

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
