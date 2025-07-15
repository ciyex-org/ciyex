package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ListMessagingURLDto;
import com.qiaben.ciyex.service.telnyx.ListMessagingURLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class ListMessagingURLController {

    private final ListMessagingURLService listMessagingURLService;

    @Autowired
    public ListMessagingURLController(ListMessagingURLService listMessagingURLService) {
        this.listMessagingURLService = listMessagingURLService;
    }

    @GetMapping("/messaging_url_domains")
    public ListMessagingURLDto getMessagingUrlDomains(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return listMessagingURLService.listMessagingUrlDomains(page, size);
    }
}
