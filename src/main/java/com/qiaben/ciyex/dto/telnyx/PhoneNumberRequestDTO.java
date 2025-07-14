package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class PhoneNumberRequestDTO {
    private String location_id; // Required in PATCH request body
}
