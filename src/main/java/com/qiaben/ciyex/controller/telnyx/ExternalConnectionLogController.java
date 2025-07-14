package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ExternalConnectionLogMessageDTO;
import com.qiaben.ciyex.service.telnyx.ExternalConnectionLogService;
import com.qiaben.ciyex.service.telnyx.ExternalConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/external-connections/logs")
@RequiredArgsConstructor
public class ExternalConnectionLogController {

    private final ExternalConnectionLogService logService;

    @GetMapping
    public ExternalConnectionLogMessageDTO getLogs(
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "250") Integer pageSize,
            @RequestParam(required = false) String externalConnectionId,
            @RequestParam(required = false) String telephoneNumberContains,
            @RequestParam(required = false) String telephoneNumberEq
    ) {
        return logService.getLogMessages(
                pageNumber,
                pageSize,
                externalConnectionId,
                telephoneNumberContains,
                telephoneNumberEq
        );
    }

    @GetMapping("/{id}")
    public ExternalConnectionLogMessageDTO getLogMessageById(@PathVariable String id) {
        return logService.getLogMessageById(id);
    }

    @DeleteMapping("/{id}")
    public boolean dismissLogMessage(@PathVariable String id) {
        return logService.dismissLogMessageById(id);
    }
}
