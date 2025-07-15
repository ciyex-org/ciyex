package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SendMessageDto;
import com.qiaben.ciyex.service.telnyx.SendMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class SendMessageController {

    private final SendMessageService sendMessageService;

    @Autowired
    public SendMessageController(SendMessageService sendMessageService) {
        this.sendMessageService = sendMessageService;
    }

    @PostMapping("/messages")
    public Object sendMessage(@RequestBody SendMessageDto sendMessageDto) {
        return sendMessageService.sendMessage(sendMessageDto);
    }
}
