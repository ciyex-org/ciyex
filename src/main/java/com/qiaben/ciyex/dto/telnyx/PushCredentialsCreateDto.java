package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class PushCredentialsCreateDto {
    private String type;
    private String certificate;
    private String private_key;
    private String alias;
}
