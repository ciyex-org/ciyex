package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxRoomRecordingDeleteResponseDto {
    private Data data;

    @lombok.Data
    public static class Data {
        private int room_recordings;
    }
}
