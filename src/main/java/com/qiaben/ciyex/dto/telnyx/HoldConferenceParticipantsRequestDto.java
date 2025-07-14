package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class HoldConferenceParticipantsRequestDto {
    private List<String> callControlIds; // Optional: put all participants on hold if null/empty
    private String audioUrl;             // Optional
    private String mediaName;            // Optional (mutually exclusive with audioUrl)
}
