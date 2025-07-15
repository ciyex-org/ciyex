package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.MMSMessageDto;
import com.qiaben.ciyex.service.telnyx.MMSMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class MMSMessageController {

    private final MMSMessageService mmsMessageService;

    @Autowired
    public MMSMessageController(MMSMessageService mmsMessageService) {
        this.mmsMessageService = mmsMessageService;
    }

    @PostMapping("/messages/group_mms")
    public Object sendGroupMMS(@RequestBody MMSMessageDto mmsMessageDto) {
        return mmsMessageService.sendGroupMMS(mmsMessageDto);
    }
}
