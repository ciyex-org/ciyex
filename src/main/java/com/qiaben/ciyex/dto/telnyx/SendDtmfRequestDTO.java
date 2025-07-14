package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class SendDtmfRequestDTO {
    private String digits;           // required
    private Integer durationMillis; // optional (default 250)
    private String clientState;     // optional
    private String commandId;       // optional
}
