package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class KickParticipantsRequestDto {
    private Object participants;
    private List<String> exclude;
}
