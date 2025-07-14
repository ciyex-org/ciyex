package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class FqdnConnectionListResponseDto {
    private List<FqdnConnectionDto> data;
}
