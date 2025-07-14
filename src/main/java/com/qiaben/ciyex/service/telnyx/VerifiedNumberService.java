package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class VerifiedNumberService {

    private final TelnyxProperties telnyxProperties;

    public VerifiedNumberResponseDTO listVerifiedNumbers(int page, int size) {
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
                .body(VerifiedNumberResponseDTO.class);
    }

    public SingleVerifiedNumberResponseDTO retrieveVerifiedNumber(String phoneNumber) {
        String url = telnyxProperties.getApiBaseUrl()
                + "/v2/verified_numbers/" + phoneNumber;

        RestClient client = RestClient.create();

        return client.get()
                .uri(url)
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(SingleVerifiedNumberResponseDTO.class);
    }

    public String requestPhoneVerification(RequestVerificationDto dto) {
        RestClient client = RestClient.create();

        return client.post()
                .uri(telnyxProperties.getApiBaseUrl() + "/v2/verified_numbers")
                .header("Authorization", "Bearer " + telnyxProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(String.class);
    }

    public String submitVerificationCode(String phoneNumber, SubmitVerificationCodeDTO dto) {
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
