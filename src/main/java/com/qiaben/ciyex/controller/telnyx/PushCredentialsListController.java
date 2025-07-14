package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.PushCredentialsListResponseDto;
import com.qiaben.ciyex.service.telnyx.PushCredentialsListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/push-credentials")
@RequiredArgsConstructor
public class PushCredentialsListController {

    private final PushCredentialsListService service;

    @GetMapping
    public PushCredentialsListResponseDto list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String alias,
            @RequestParam(name = "page[size]", required = false) Integer pageSize,
            @RequestParam(name = "page[number]", required = false) Integer pageNumber
    ) {
        return service.listPushCredentials(type, alias, pageSize, pageNumber);
    }
}
