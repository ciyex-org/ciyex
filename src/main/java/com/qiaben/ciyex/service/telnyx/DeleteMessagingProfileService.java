package com.qiaben.ciyex.service.telnyx;

import com.qiaben.ciyex.config.TelnyxProperties;
import com.qiaben.ciyex.dto.telnyx.DeleteMessagingProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DeleteMessagingProfileService {

    private final TelnyxProperties telnyxProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public DeleteMessagingProfileService(TelnyxProperties telnyxProperties, RestTemplate restTemplate) {
        this.telnyxProperties = telnyxProperties;
        this.restTemplate = restTemplate;
    }

    public DeleteMessagingProfileDto deleteMessagingProfile(String id) {
        String url = String.format("%s/v2/messaging_profiles/%s", telnyxProperties.getApiBaseUrl(), id);
        restTemplate.delete(url); // Send DELETE request
        return new DeleteMessagingProfileDto(id, "Deleted successfully");
    }
}
