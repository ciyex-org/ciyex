package com.qiaben.ciyex.controller.telnyx.voice;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRequestVerificationDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSingleVerifiedNumberResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitVerificationCodeDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxVerifiedNumberResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxVerifiedNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verified-numbers")
@RequiredArgsConstructor
public class TelnyxVerifiedNumberController {

    private final TelnyxVerifiedNumberService service;

    @GetMapping
    public TelnyxVerifiedNumberResponseDTO getVerifiedNumbers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return service.listVerifiedNumbers(page, size);
    }

    @PostMapping
    public String requestVerification(@RequestBody TelnyxRequestVerificationDto dto) {
        return service.requestPhoneVerification(dto);
    }

    @PostMapping("/{phoneNumber}")
    public String verifyCode(
            @PathVariable String phoneNumber,
            @RequestBody TelnyxSubmitVerificationCodeDTO dto
    ) {
        return service.submitVerificationCode(phoneNumber, dto);
    }

    @GetMapping("/{phoneNumber}")
    public TelnyxSingleVerifiedNumberResponseDTO getVerifiedNumber(@PathVariable String phoneNumber) {
        return service.retrieveVerifiedNumber(phoneNumber);
    }

    @DeleteMapping("/{phoneNumber}")
    public String deleteVerifiedNumber(@PathVariable String phoneNumber) {
        return service.deleteVerifiedNumber(phoneNumber);
    }

}

