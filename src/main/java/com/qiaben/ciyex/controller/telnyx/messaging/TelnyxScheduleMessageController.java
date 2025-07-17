package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxScheduleMessageDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxScheduleMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxScheduleMessageController {

    private final TelnyxScheduleMessageService telnyxScheduleMessageService;

    @Autowired
    public TelnyxScheduleMessageController(TelnyxScheduleMessageService telnyxScheduleMessageService) {
        this.telnyxScheduleMessageService = telnyxScheduleMessageService;
    }

    @PostMapping("/messages/schedule")
    public Object scheduleMessage(@RequestBody TelnyxScheduleMessageDto dto) {
        return telnyxScheduleMessageService.scheduleMessage(dto);
    }
}
