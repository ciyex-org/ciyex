package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

@Data
public class TelnyxPhoneNumberRequestDTO {
    private String location_id; // Required in PATCH request body
}
