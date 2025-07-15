package com.qiaben.ciyex.service.telnyx;


import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.RoomDto;
import com.qiaben.ciyex.dto.telnyx.RoomRequestDto;
import com.qiaben.ciyex.dto.telnyx.RoomResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.Collections;
import java.util.Map;

@Service
public class TelnyxRoomService {

    @Autowired
    private TelnyxProperties telnyxProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    public RoomDto getRooms(Map<String, String> queryParams) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/rooms";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + telnyxProperties.getApiKey());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        queryParams.forEach(builder::queryParam);

        ResponseEntity<RoomDto> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                RoomDto.class
        );

        return response.getBody();
    }

    public RoomResponseDto createRoom(RoomRequestDto roomRequestDto) {
        String url = telnyxProperties.getApiBaseUrl() + "/v2/rooms";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + telnyxProperties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<RoomRequestDto> entity = new HttpEntity<>(roomRequestDto, headers);

        ResponseEntity<RoomResponseDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, RoomResponseDto.class);

        return response.getBody();
    }

}
