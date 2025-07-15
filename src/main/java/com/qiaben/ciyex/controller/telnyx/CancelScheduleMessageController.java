package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CancelScheduleMessageDto;
import com.qiaben.ciyex.service.telnyx.CancelScheduleMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class CancelScheduleMessageController {

    private final CancelScheduleMessageService cancelScheduleMessageService;

    @Autowired
    public CancelScheduleMessageController(CancelScheduleMessageService cancelScheduleMessageService) {
        this.cancelScheduleMessageService = cancelScheduleMessageService;
    }

    @DeleteMapping("/messages/{id}")
    public CancelScheduleMessageDto cancelScheduledMessage(@PathVariable("id") String id) {
        return cancelScheduleMessageService.cancelScheduledMessage(id);
    }
}
