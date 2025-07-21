package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSendMessageDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxSendMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxSendMessageController {

    private final TelnyxSendMessageService telnyxSendMessageService;

    @Autowired
    public TelnyxSendMessageController(TelnyxSendMessageService telnyxSendMessageService) {
        this.telnyxSendMessageService = telnyxSendMessageService;
    }

    @PostMapping("/messages")
    public Object sendMessage(@RequestBody TelnyxSendMessageDto sendMessageDto) {
        return telnyxSendMessageService.sendMessage(sendMessageDto);
    }
}
