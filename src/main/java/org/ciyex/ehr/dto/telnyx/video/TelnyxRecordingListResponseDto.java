package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxRecordingListResponseDto {
    private List<TelnyxRecordingDto> data;
    private TelnyxRecordingListMetaDto meta;
}