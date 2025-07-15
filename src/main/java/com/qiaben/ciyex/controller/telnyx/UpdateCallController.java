package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CallResponseDto;
import com.qiaben.ciyex.dto.telnyx.UpdateCallRequestDto;
import com.qiaben.ciyex.service.telnyx.UpdateCallService;  // ← corrected package
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/texml")
@RequiredArgsConstructor
public class UpdateCallController {

    private final UpdateCallService updateCallService;

    @PostMapping("/Accounts/{accountSid}/Calls/{callSid}")
    public CallResponseDto updateCall(@PathVariable String accountSid,
                                      @PathVariable String callSid,
                                      @ModelAttribute UpdateCallRequestDto request) {
        return updateCallService.updateCall(accountSid, callSid, request);
    }
}
