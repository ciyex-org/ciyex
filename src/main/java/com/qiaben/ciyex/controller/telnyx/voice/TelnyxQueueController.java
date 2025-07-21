package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxQueueDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*; // ✅ Required for @RestController, @GetMapping, etc.

@RestController
@RequestMapping("/api/telnyx/queues")
@RequiredArgsConstructor
public class TelnyxQueueController {

    private final TelnyxQueueService telnyxQueueService;

    @GetMapping("/{queueName}")
    public TelnyxQueueDto.QueueWrapper getQueue(@PathVariable String queueName) {
        return telnyxQueueService.getQueue(queueName);
    }

    @GetMapping("/{queueName}/calls/{callControlId}")
    public TelnyxQueueDto.QueueCallWrapper getQueueCall(
            @PathVariable String queueName,
            @PathVariable String callControlId) {
        return telnyxQueueService.getQueueCall(queueName, callControlId);
    }

    @GetMapping("/{queueName}/calls")
    public TelnyxQueueDto.QueueCallListWrapper listQueueCalls(
            @PathVariable String queueName,
            @RequestParam(name = "page[number]", defaultValue = "1") Integer page,
            @RequestParam(name = "page[size]", defaultValue = "20") Integer size) {
        return telnyxQueueService.listQueueCalls(queueName, page, size);
    }
}
