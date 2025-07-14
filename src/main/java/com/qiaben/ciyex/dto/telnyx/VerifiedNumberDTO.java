package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class VerifiedNumberDTO {
    private String phone_number;
    private String record_type;
    private String verified_at;
}
