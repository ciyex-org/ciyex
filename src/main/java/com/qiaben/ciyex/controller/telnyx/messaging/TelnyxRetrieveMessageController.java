package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRetrieveMessageDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxRetrieveMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxRetrieveMessageController {

    private final TelnyxRetrieveMessageService telnyxRetrieveMessageService;

    @Autowired
    public TelnyxRetrieveMessageController(TelnyxRetrieveMessageService telnyxRetrieveMessageService) {
        this.telnyxRetrieveMessageService = telnyxRetrieveMessageService;
    }

    @GetMapping("/messages/{id}")
    public TelnyxRetrieveMessageDto getMessage(@PathVariable String id) {
        return telnyxRetrieveMessageService.getMessageById(id);
    }
}
