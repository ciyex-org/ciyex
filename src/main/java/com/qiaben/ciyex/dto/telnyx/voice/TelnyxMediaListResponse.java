package com.qiaben.ciyex.dto.telnyx.voice;
import lombok.Data;

import java.util.List;

@Data
public class TelnyxMediaListResponse {
    private List<TelnyxMediaDTO> data;
    private TelnyxMediaMetaDTO meta;
}
