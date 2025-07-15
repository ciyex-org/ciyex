package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FqdnDto {
    private String id;

    @JsonProperty("record_type")
    private String recordType;

    @JsonProperty("connection_id")
    private String connectionId;

    private String fqdn;
    private Integer port;

    @JsonProperty("dns_record_type")
    private String dnsRecordType;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}
