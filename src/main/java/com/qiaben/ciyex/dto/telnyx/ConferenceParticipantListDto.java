package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ConferenceParticipantListDto {

    private List<DataItem> data;
    private Meta meta;

    @Data
    public static class DataItem {
        private String recordType;
        private String id;
        private String callLegId;
        private String callControlId;
        private Conference conference;
        private List<String> whisperCallControlIds;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private Boolean endConferenceOnExit;
        private Boolean softEndConferenceOnExit;
        private String status;
        private Boolean muted;
        private Boolean onHold;

        @Data
        public static class Conference {
            private String id;
            private String callControlId;
            private OffsetDateTime createdAt;
        }
    }

    @Data
    public static class Meta {
        private Integer totalPages;
        private Integer totalResults;
        private Integer pageNumber;
        private Integer pageSize;
    }
}
