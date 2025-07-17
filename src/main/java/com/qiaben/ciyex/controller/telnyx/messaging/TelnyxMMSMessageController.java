package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMMSMessageDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxMMSMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxMMSMessageController {

    private final TelnyxMMSMessageService telnyxMmsMessageService;

    @Autowired
    public TelnyxMMSMessageController(TelnyxMMSMessageService telnyxMmsMessageService) {
        this.telnyxMmsMessageService = telnyxMmsMessageService;
    }

    @PostMapping("/messages/group_mms")
    public Object sendGroupMMS(@RequestBody TelnyxMMSMessageDto mmsMessageDto) {
        return telnyxMmsMessageService.sendGroupMMS(mmsMessageDto);
    }
}
