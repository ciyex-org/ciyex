package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxFqdnConnectionListResponseDto {
    private List<TelnyxFqdnConnectionDto> data;
}
