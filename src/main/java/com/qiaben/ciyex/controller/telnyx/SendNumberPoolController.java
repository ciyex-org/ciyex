package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SendNumberPoolDto;
import com.qiaben.ciyex.service.telnyx.SendNumberPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class SendNumberPoolController {

    private final SendNumberPoolService sendNumberPoolService;

    @Autowired
    public SendNumberPoolController(SendNumberPoolService sendNumberPoolService) {
        this.sendNumberPoolService = sendNumberPoolService;
    }

    @PostMapping("/messages/number_pool")
    public Object sendNumberPoolMessage(@RequestBody SendNumberPoolDto sendNumberPoolDto) {
        return sendNumberPoolService.sendNumberPoolMessage(sendNumberPoolDto);
    }
}
