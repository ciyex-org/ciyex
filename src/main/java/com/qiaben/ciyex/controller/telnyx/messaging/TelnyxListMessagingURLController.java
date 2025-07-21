package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListMessagingURLDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxListMessagingURLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxListMessagingURLController {

    private final TelnyxListMessagingURLService telnyxListMessagingURLService;

    @Autowired
    public TelnyxListMessagingURLController(TelnyxListMessagingURLService telnyxListMessagingURLService) {
        this.telnyxListMessagingURLService = telnyxListMessagingURLService;
    }

    @GetMapping("/messaging_url_domains")
    public TelnyxListMessagingURLDto getMessagingUrlDomains(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return telnyxListMessagingURLService.listMessagingUrlDomains(page, size);
    }
}
