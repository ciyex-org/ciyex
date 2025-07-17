package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxHoldConferenceParticipantsRequestDto {
    private List<String> callControlIds; // Optional: put all participants on hold if null/empty
    private String audioUrl;             // Optional
    private String mediaName;            // Optional (mutually exclusive with audioUrl)
}
