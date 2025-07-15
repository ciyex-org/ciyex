// src/main/java/com/qiaben/ciyex/service/telnyx/TelnyxRcsService.java
package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RcsCapabilitiesDTO;
import com.qiaben.ciyex.dto.telnyx.RcsTestNumberInviteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelnyxRcsService {

    private final TelnyxProperties props;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    // GET capabilities
    public RcsCapabilitiesDTO listCapabilities(String agentId, String phoneE164) {
        String path = String.format("/rcs/agents/%s/phone_numbers/%s/capabilities", agentId, phoneE164);
        return client()
                .get()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RcsCapabilitiesDTO.class);
    }

    // POST invite
    public RcsTestNumberInviteDTO inviteTestNumber(String agentId, String phoneE164) {
        String path = String.format("/rcs/agents/%s/test_numbers/%s", agentId, phoneE164);
        return client()
                .post()
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(RcsTestNumberInviteDTO.class);
    }
}
