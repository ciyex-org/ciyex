
// src/main/java/com/qiaben/ciyex/controller/telnyx/MessagingNumberController.java
package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListPhoneNumbersResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMessagingPhoneNumberDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdatePhoneNumberRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxBulkUpdateRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxBulkUpdateResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxMessagingNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging-numbers")
@RequiredArgsConstructor
public class TelnyxMessagingNumberController {

    private final TelnyxMessagingNumberService service;

    @GetMapping
    public TelnyxListPhoneNumbersResponseDTO list(
            @RequestParam(defaultValue = "1", name = "pageNumber") int pageNumber,
            @RequestParam(defaultValue = "20", name = "pageSize") int pageSize) {
        return service.list(pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public TelnyxMessagingPhoneNumberDTO get(@PathVariable String id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public TelnyxMessagingPhoneNumberDTO update(
            @PathVariable String id,
            @RequestBody @Validated TelnyxUpdatePhoneNumberRequestDTO body) {
        return service.update(id, body);
    }

    @PostMapping("/bulk-update")
    public TelnyxBulkUpdateResponseDTO bulkUpdate(
            @RequestBody @Validated TelnyxBulkUpdateRequestDTO body) {
        return service.bulkUpdate(body);
    }

    @GetMapping("/bulk-update/{orderId}")
    public TelnyxBulkUpdateResponseDTO bulkStatus(@PathVariable String orderId) {
        return service.bulkStatus(orderId);
    }
}

