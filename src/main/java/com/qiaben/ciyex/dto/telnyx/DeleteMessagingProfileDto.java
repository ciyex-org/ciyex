package com.qiaben.ciyex.dto.telnyx;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeleteMessagingProfileDto {
    private String id;
    private String message;
}
