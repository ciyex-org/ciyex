package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class ConferencePlayAudioRequestDto {
    private String audioUrl;               // Optional, mutually exclusive with mediaName
    private String mediaName;              // Optional, mutually exclusive with audioUrl
    private Object loop;                   // Integer (1-100) or "infinity"
    private List<String> callControlIds;   // Optional: target specific participants
}
