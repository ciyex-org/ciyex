package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRequestVerificationDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSingleVerifiedNumberResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitVerificationCodeDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxVerifiedNumberResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxVerifiedNumberService {

    private final TelnyxProperties telnyxProperties;

    public TelnyxVerifiedNumberResponseDTO listVerifiedNumbers(int page, int size) {
        String url = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl() + "/v2/verified_numbers")
                .queryParam("page[number]", page)
                .queryParam("page[size]", size)
                .toUriString();

        RestClient client = RestClient.create();

        return client.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxVerifiedNumberResponseDTO.class);
    }

    public TelnyxSingleVerifiedNumberResponseDTO retrieveVerifiedNumber(String phoneNumber) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/verified_numbers/" + phoneNumber;

        RestClient client = RestClient.create();

        return client.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxSingleVerifiedNumberResponseDTO.class);
    }

    public String requestPhoneVerification(TelnyxRequestVerificationDto dto) {
        RestClient client = RestClient.create();

        return client.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/verified_numbers")
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String submitVerificationCode(String phoneNumber, TelnyxSubmitVerificationCodeDTO dto) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/verified_numbers/" + phoneNumber + "/actions/verify";

        RestClient client = RestClient.create();

        return client.post()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String deleteVerifiedNumber(String phoneNumber) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/verified_numbers/" + phoneNumber;

        RestClient client = RestClient.create();

        return client.delete()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(String.class); // You can map it to a DTO if needed
    }

}
