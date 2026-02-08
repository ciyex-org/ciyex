package org.ciyex.ehr.dto.telnyx.voice;

import org.ciyex.ehr.dto.telnyx.messaging.TelnyxMetaDto;
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
