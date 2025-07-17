package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMessagingOptOutQueryParams;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMessagingOptOutResponse;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxMessagingOptOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging/opt-outs")
@RequiredArgsConstructor
public class TelnyxMessagingOptOutController {

    private final TelnyxMessagingOptOutService service;

    @GetMapping
    public ResponseEntity<TelnyxMessagingOptOutResponse> listOptOuts(
            @RequestParam(required = false, name = "messaging_profile_id") String messagingProfileId,
            @RequestParam(required = false, name = "created_at[gte]") String createdAfter,
            @RequestParam(required = false, name = "created_at[lte]") String createdBefore,
            @RequestParam(required = false, name = "filter[from]") String from,
            @RequestParam(required = false, name = "redaction_enabled") Boolean redactionEnabled,
            @RequestParam(defaultValue = "1", name = "page[number]") Integer pageNumber,
            @RequestParam(defaultValue = "20", name = "page[size]") Integer pageSize
    ) {
        TelnyxMessagingOptOutQueryParams params = TelnyxMessagingOptOutQueryParams.builder()
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
