package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class LeaveConferenceRequestDto {
    private String callControlId;
    private String commandId;
    private String beepEnabled; // always | never | on_enter | on_exit
}
