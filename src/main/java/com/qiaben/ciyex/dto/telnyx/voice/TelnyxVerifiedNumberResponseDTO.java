package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxVerifiedNumberResponseDTO {
    private List<TelnyxVerifiedNumberDTO> data;
}
