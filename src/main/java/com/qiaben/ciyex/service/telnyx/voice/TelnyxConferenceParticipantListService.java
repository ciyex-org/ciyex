package com.qiaben.ciyex.service.telnyx.voice;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceParticipantListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TelnyxConferenceParticipantListService {

    private final TelnyxProperties telnyxProperties;
    private final RestClient restClient = RestClient.create();

    public TelnyxConferenceParticipantListDto listParticipants(
            String conferenceId,
            Boolean muted,
            Boolean onHold,
            Boolean whispering,
            Integer pageNumber,
            Integer pageSize) {

        String url = UriComponentsBuilder
                .fromHttpUrl(telnyxProperties.getApiBaseUrl())
                .path("/v2/conferences/{conferenceId}/participants")
                .queryParamIfPresent("filter[muted]", java.util.Optional.ofNullable(muted))
                .queryParamIfPresent("filter[on_hold]", java.util.Optional.ofNullable(onHold))
                .queryParamIfPresent("filter[whispering]", java.util.Optional.ofNullable(whispering))
                .queryParamIfPresent("page[number]", java.util.Optional.ofNullable(pageNumber))
                .queryParamIfPresent("page[size]", java.util.Optional.ofNullable(pageSize))
                .build(conferenceId)
                .toString();

        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + telnyxProperties.getApiKey())
                .retrieve()
                .body(TelnyxConferenceParticipantListDto.class);
    }
}
