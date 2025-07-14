package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class MuteConferenceParticipantsRequestDto {
    private List<String> callControlIds; // Optional: mute all if null or empty
}
