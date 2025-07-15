package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.QueueDto;
import com.qiaben.ciyex.service.telnyx.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*; // ✅ Required for @RestController, @GetMapping, etc.

@RestController
@RequestMapping("/api/telnyx/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @GetMapping("/{queueName}")
    public QueueDto.QueueWrapper getQueue(@PathVariable String queueName) {
        return queueService.getQueue(queueName);
    }

    @GetMapping("/{queueName}/calls/{callControlId}")
    public QueueDto.QueueCallWrapper getQueueCall(
            @PathVariable String queueName,
            @PathVariable String callControlId) {
        return queueService.getQueueCall(queueName, callControlId);
    }

    @GetMapping("/{queueName}/calls")
    public QueueDto.QueueCallListWrapper listQueueCalls(
            @PathVariable String queueName,
            @RequestParam(name = "page[number]", defaultValue = "1") Integer page,
            @RequestParam(name = "page[size]", defaultValue = "20") Integer size) {
        return queueService.listQueueCalls(queueName, page, size);
    }
}
