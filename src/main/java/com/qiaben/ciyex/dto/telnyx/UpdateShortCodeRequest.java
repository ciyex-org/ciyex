package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

// DTO for PATCH body
@Data
public class UpdateShortCodeRequest {
    private String messaging_profile_id;
}
