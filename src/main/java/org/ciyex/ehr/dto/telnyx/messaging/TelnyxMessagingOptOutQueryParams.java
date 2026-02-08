package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelnyxMessagingOptOutQueryParams {
    private String messagingProfileId;
    private String createdAfter;
    private String createdBefore;
    private String from;
    private Boolean redactionEnabled;
    private Integer pageNumber;
    private Integer pageSize;
}
