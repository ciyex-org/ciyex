package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class UnholdConferenceParticipantsRequestDto {
    private List<String> callControlIds;
}
