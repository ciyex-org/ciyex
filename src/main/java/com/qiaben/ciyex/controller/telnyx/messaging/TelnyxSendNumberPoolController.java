package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSendNumberPoolDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxSendNumberPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxSendNumberPoolController {

    private final TelnyxSendNumberPoolService telnyxSendNumberPoolService;

    @Autowired
    public TelnyxSendNumberPoolController(TelnyxSendNumberPoolService telnyxSendNumberPoolService) {
        this.telnyxSendNumberPoolService = telnyxSendNumberPoolService;
    }

    @PostMapping("/messages/number_pool")
    public Object sendNumberPoolMessage(@RequestBody TelnyxSendNumberPoolDto telnyxSendNumberPoolDto) {
        return telnyxSendNumberPoolService.sendNumberPoolMessage(telnyxSendNumberPoolDto);
    }
}
