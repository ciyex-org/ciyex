package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.Map;

@Data
public class GetCampaignMnoMetadataResponseDTO {
    private Map<String, MnoMetadata> data;

    @Data
    public static class MnoMetadata {
        private Boolean qualify;
        private String mno;
        private Boolean noEmbeddedLink;
        private Boolean reqSubscriberHelp;
        private Boolean reqSubscriberOptout;
        private Boolean mnoReview;
        private Boolean noEmbeddedPhone;
        private Boolean mnoSupport;
        private Boolean reqSubscriberOptin;
        private Integer minMsgSamples;
    }
}
