package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class RecordingListResponseDto {
    private List<RecordingDto> data;
    private RecordingListMetaDto meta;
}