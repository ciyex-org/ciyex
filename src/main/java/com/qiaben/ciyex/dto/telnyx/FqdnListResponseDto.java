package com.qiaben.ciyex.dto.telnyx;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FqdnListResponseDto {
    private List<FqdnDto> data;
    private MetaDto meta;
}
