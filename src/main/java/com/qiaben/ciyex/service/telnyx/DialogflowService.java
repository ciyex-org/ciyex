package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.dto.telnyx.DialogflowRequestDTO;
import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.config.TelnyxProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DialogflowService {

    private final TelnyxProperties telnyxProperties;

    public DialogflowResponseDTO createDialogflowConnection(DialogflowRequestDTO dto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/dialogflow_connections/" + dto.getConnectionId();

        Map<String, Object> body = new HashMap<>();
        body.put("service_account", dto.getServiceAccount());
        body.put("dialogflow_api", dto.getDialogflowApi());
        body.put("conversation_profile_id", dto.getConversationProfileId());
        body.put("location", dto.getLocation());
        body.put("environment", dto.getEnvironment());

        return RestClient.create()
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(DialogflowResponseDTO.class);
    }

    public DialogflowResponseDTO getDialogflowConnection(String connectionId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/dialogflow_connections/" + connectionId;

        return RestClient.create()
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(DialogflowResponseDTO.class);
    }

    public DialogflowResponseDTO updateDialogflowConnection(String connectionId, DialogflowRequestDTO dto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/dialogflow_connections/" + connectionId;

        Map<String, Object> body = new HashMap<>();
        body.put("service_account", dto.getServiceAccount());
        body.put("dialogflow_api", dto.getDialogflowApi());
        body.put("conversation_profile_id", dto.getConversationProfileId());
        body.put("location", dto.getLocation());
        body.put("environment", dto.getEnvironment());

        return RestClient.create()
                .put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(DialogflowResponseDTO.class);
    }

    public void deleteDialogflowConnection(String connectionId) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/dialogflow_connections/" + connectionId;

        RestClient.create()
                .delete()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .toBodilessEntity(); // DELETE with 204 returns no body
    }

}

