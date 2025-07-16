package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RetrieveMessageDto;
import com.qiaben.ciyex.service.telnyx.RetrieveMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class RetrieveMessageController {

    private final RetrieveMessageService retrieveMessageService;

    @Autowired
    public RetrieveMessageController(RetrieveMessageService retrieveMessageService) {
        this.retrieveMessageService = retrieveMessageService;
    }

    @GetMapping("/messages/{id}")
    public RetrieveMessageDto getMessage(@PathVariable String id) {
        return retrieveMessageService.getMessageById(id);
    }
}
