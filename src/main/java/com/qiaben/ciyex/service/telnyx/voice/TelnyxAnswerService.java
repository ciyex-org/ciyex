package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAnswerRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAnswerResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelnyxAnswerService {
    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public void handle() {
        try {
            // Your actual Telnyx API call logic here
        } catch (RestClientException ex) {
            log.error("Answer call failed: {}", ex.getMessage(), ex);
        }
    }

    public TelnyxAnswerResponseDTO answerCall(String callControlId, TelnyxAnswerRequestDTO body) {
        return null;
    }
}
