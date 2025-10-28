package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxMuteRequestDto {

    private Object participants;
    private List<String> exclude;
}
