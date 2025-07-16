package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ScheduleMessageDto;
import com.qiaben.ciyex.service.telnyx.ScheduleMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class ScheduleMessageController {

    private final ScheduleMessageService scheduleMessageService;

    @Autowired
    public ScheduleMessageController(ScheduleMessageService scheduleMessageService) {
        this.scheduleMessageService = scheduleMessageService;
    }

    @PostMapping("/messages/schedule")
    public Object scheduleMessage(@RequestBody ScheduleMessageDto dto) {
        return scheduleMessageService.scheduleMessage(dto);
    }
}
