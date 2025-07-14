package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class UpdateConferenceParticipantResponseDto {
    private Data data;

    @lombok.Data
    public static class Data {
        private String result;
    }
}
