package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxShortCodeMessageDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxShortCodeMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxShortCodeMessageController {

    private final TelnyxShortCodeMessageService telnyxShortCodeMessageService;

    @Autowired
    public TelnyxShortCodeMessageController(TelnyxShortCodeMessageService telnyxShortCodeMessageService) {
        this.telnyxShortCodeMessageService = telnyxShortCodeMessageService;
    }

    @PostMapping("/messages/short_code")
    public Object sendShortCodeMessage(@RequestBody TelnyxShortCodeMessageDto payload) {
        return telnyxShortCodeMessageService.sendShortCodeMessage(payload);
    }
}
