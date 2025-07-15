package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class VerifiedNumberResponseDTO {
    private List<VerifiedNumberDTO> data;
}
