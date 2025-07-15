package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.ListPhoneMessagingProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RetrievePhoneNumbersService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public RetrievePhoneNumbersService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public ListPhoneMessagingProfileDto listPhoneNumbers(String id, int pageNumber, int pageSize) {
        String url = String.format("%s/v2/messaging_profiles/%s/phone_numbers?page[number]=%d&page[size]=%d",
                telnyxProperties.getApiBaseUrl(), id, pageNumber, pageSize);
        return restTemplate.getForObject(url, ListPhoneMessagingProfileDto.class);
    }
}
