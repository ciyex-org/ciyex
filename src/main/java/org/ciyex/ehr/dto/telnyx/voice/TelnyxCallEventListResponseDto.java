package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TelnyxCallEventListResponseDto {
    private List<CallEvent> data;
    private Meta meta;

    @Data
    public static class CallEvent {
        private String recordType;
        private String callLegId;
        private String callSessionId;
        private String eventTimestamp;
        private String name;
        private String type;
        private Map<String, Object> metadata;
    }

    @Data
    public static class Meta {
        private Integer totalPages;
        private Integer totalResults;
        private Integer pageNumber;
        private Integer pageSize;
    }
}
