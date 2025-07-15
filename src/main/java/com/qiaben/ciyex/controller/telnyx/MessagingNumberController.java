
// src/main/java/com/qiaben/ciyex/controller/telnyx/MessagingNumberController.java
package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.MessagingNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging-numbers")
@RequiredArgsConstructor
public class MessagingNumberController {

    private final MessagingNumberService service;

    @GetMapping
    public ListPhoneNumbersResponseDTO list(
            @RequestParam(defaultValue = "1", name = "pageNumber") int pageNumber,
            @RequestParam(defaultValue = "20", name = "pageSize") int pageSize) {
        return service.list(pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public MessagingPhoneNumberDTO get(@PathVariable String id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public MessagingPhoneNumberDTO update(
            @PathVariable String id,
            @RequestBody @Validated UpdatePhoneNumberRequestDTO body) {
        return service.update(id, body);
    }

    @PostMapping("/bulk-update")
    public BulkUpdateResponseDTO bulkUpdate(
            @RequestBody @Validated BulkUpdateRequestDTO body) {
        return service.bulkUpdate(body);
    }

    @GetMapping("/bulk-update/{orderId}")
    public BulkUpdateResponseDTO bulkStatus(@PathVariable String orderId) {
        return service.bulkStatus(orderId);
    }
}

