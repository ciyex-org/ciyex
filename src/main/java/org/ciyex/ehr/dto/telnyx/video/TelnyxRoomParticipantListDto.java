package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxRoomParticipantListDto {
    private List<ParticipantDto> data;
    private Meta meta;

    @Data
    public static class ParticipantDto {
        private String id;
        private String sessionId;
        private String context;
        private String joinedAt;
        private String updatedAt;
        private String leftAt;
        private String recordType;
    }

    @Data
    public static class Meta {
        private Integer pageNumber;
        private Integer pageSize;
        private Integer totalPages;
        private Integer totalResults;
    }
}
