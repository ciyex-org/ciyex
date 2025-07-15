package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class FqdnConnectionDto {
    private String id;
    private String record_type;
    private String connection_id;
    private String ip_address;
    private Integer port;
    private String created_at;
    private String updated_at;
}
