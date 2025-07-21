package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCancelScheduleMessageDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxCancelScheduleMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxCancelScheduleMessageController {

    private final TelnyxCancelScheduleMessageService telnyxCancelScheduleMessageService;

    @Autowired
    public TelnyxCancelScheduleMessageController(TelnyxCancelScheduleMessageService telnyxCancelScheduleMessageService) {
        this.telnyxCancelScheduleMessageService = telnyxCancelScheduleMessageService;
    }

    @DeleteMapping("/messages/{id}")
    public TelnyxCancelScheduleMessageDto cancelScheduledMessage(@PathVariable("id") String id) {
        return telnyxCancelScheduleMessageService.cancelScheduledMessage(id);
    }
}
