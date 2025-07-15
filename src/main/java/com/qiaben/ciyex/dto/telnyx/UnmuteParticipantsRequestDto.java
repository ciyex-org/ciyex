package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class UnmuteParticipantsRequestDto {
    private Object participants;
    private List<String> exclude;
}