package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class RoomRecordingDeleteResponseDto {
    private Data data;

    @lombok.Data
    public static class Data {
        private int room_recordings;
    }
}
