package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.MessagingOptOutQueryParams;
import com.qiaben.ciyex.dto.telnyx.MessagingOptOutResponse;
import com.qiaben.ciyex.service.telnyx.MessagingOptOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging/opt-outs")
@RequiredArgsConstructor
public class MessagingOptOutController {

    private final MessagingOptOutService service;

    @GetMapping
    public ResponseEntity<MessagingOptOutResponse> listOptOuts(
            @RequestParam(required = false, name = "messaging_profile_id") String messagingProfileId,
            @RequestParam(required = false, name = "created_at[gte]") String createdAfter,
            @RequestParam(required = false, name = "created_at[lte]") String createdBefore,
            @RequestParam(required = false, name = "filter[from]") String from,
            @RequestParam(required = false, name = "redaction_enabled") Boolean redactionEnabled,
            @RequestParam(defaultValue = "1", name = "page[number]") Integer pageNumber,
            @RequestParam(defaultValue = "20", name = "page[size]") Integer pageSize
    ) {
        MessagingOptOutQueryParams params = MessagingOptOutQueryParams.builder()
                .messagingProfileId(messagingProfileId)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .from(from)
                .redactionEnabled(redactionEnabled)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();

        return ResponseEntity.ok(service.listOptOuts(params));
    }
}
