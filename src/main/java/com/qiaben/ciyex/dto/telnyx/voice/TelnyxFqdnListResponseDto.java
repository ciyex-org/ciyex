package com.qiaben.ciyex.dto.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMetaDto;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelnyxFqdnListResponseDto {
    private List<TelnyxFqdnDto> data;
    private TelnyxMetaDto meta;
}
