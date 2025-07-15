package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SendLongCodeDto;
import com.qiaben.ciyex.service.telnyx.SendLongCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class SendLongCodeController {

    private final SendLongCodeService sendlongcodeService;

    @Autowired
    public SendLongCodeController(SendLongCodeService sendlongcodeService) {
        this.sendlongcodeService = sendlongcodeService;
    }

    @PostMapping("/messages/long_code")
    public Object sendLongCodeMessage(@RequestBody SendLongCodeDto sendlongcodeDto) {
        return sendlongcodeService.sendLongCodeMessage(sendlongcodeDto);
    }
}
