package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class UnmuteConferenceParticipantsRequestDto {
    private List<String> callControlIds; // Optional: unmute all if null or empty
}
