package org.ciyex.ehr.dto.telnyx.voice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelnyxFqdnRequestDto {
    @JsonProperty("connection_id")
    private String connectionId;
    private String fqdn;
    private Integer port;

    @JsonProperty("dns_record_type")
    private String dnsRecordType;
}
