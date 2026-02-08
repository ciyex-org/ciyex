package org.ciyex.ehr.dto.telnyx.video;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class TelnyxRecordingDto {
    private String id;
    private String recordType;
    private String callControlId;
    private String callLegId;
    private String callSessionId;
    private String conferenceId;
    private String channels; // single | dual
    private String source;   // conference | call
    private String status;   // completed
    private Long   durationMillis;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime recordingStartedAt;
    private OffsetDateTime recordingEndedAt;
    private Map<String, String> downloadUrls; // mp3, wav
}