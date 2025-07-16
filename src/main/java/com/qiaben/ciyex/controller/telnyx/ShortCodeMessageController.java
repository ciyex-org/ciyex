package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ShortCodeMessageDto;
import com.qiaben.ciyex.service.telnyx.ShortCodeMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class ShortCodeMessageController {

    private final ShortCodeMessageService shortCodeMessageService;

    @Autowired
    public ShortCodeMessageController(ShortCodeMessageService shortCodeMessageService) {
        this.shortCodeMessageService = shortCodeMessageService;
    }

    @PostMapping("/messages/short_code")
    public Object sendShortCodeMessage(@RequestBody ShortCodeMessageDto payload) {
        return shortCodeMessageService.sendShortCodeMessage(payload);
    }
}
