package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSendLongCodeDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxSendLongCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxSendLongCodeController {

    private final TelnyxSendLongCodeService sendlongcodeServiceTelnyx;

    @Autowired
    public TelnyxSendLongCodeController(TelnyxSendLongCodeService sendlongcodeServiceTelnyx) {
        this.sendlongcodeServiceTelnyx = sendlongcodeServiceTelnyx;
    }

    @PostMapping("/messages/long_code")
    public Object sendLongCodeMessage(@RequestBody TelnyxSendLongCodeDto sendlongcodeDtoTelnyx) {
        return sendlongcodeServiceTelnyx.sendLongCodeMessage(sendlongcodeDtoTelnyx);
    }
}
