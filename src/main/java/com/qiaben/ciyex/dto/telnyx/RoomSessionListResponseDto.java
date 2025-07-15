package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class RoomSessionListResponseDto {

    private List<SessionDto> data;
    private Meta meta;

    @Data
    public static class SessionDto {
        private String id;
        private String roomId;
        private Boolean active;
        private String createdAt;
        private String updatedAt;
        private String endedAt;
        private String recordType;
        private List<ParticipantDto> participants;
    }

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
