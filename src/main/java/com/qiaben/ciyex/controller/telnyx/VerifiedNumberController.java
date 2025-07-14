package com.qiaben.ciyex.controller.telnyx;
import com.qiaben.ciyex.dto.telnyx.RequestVerificationDto;
import com.qiaben.ciyex.dto.telnyx.SingleVerifiedNumberResponseDTO;
import com.qiaben.ciyex.dto.telnyx.SubmitVerificationCodeDTO;
import com.qiaben.ciyex.dto.telnyx.VerifiedNumberResponseDTO;
import com.qiaben.ciyex.service.telnyx.VerifiedNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verified-numbers")
@RequiredArgsConstructor
public class VerifiedNumberController {

    private final VerifiedNumberService service;

    @GetMapping
    public VerifiedNumberResponseDTO getVerifiedNumbers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return service.listVerifiedNumbers(page, size);
    }

    @PostMapping
    public String requestVerification(@RequestBody RequestVerificationDto dto) {
        return service.requestPhoneVerification(dto);
    }

    @PostMapping("/{phoneNumber}")
    public String verifyCode(
            @PathVariable String phoneNumber,
            @RequestBody SubmitVerificationCodeDTO dto
    ) {
        return service.submitVerificationCode(phoneNumber, dto);
    }

    @GetMapping("/{phoneNumber}")
    public SingleVerifiedNumberResponseDTO getVerifiedNumber(@PathVariable String phoneNumber) {
        return service.retrieveVerifiedNumber(phoneNumber);
    }

    @DeleteMapping("/{phoneNumber}")
    public String deleteVerifiedNumber(@PathVariable String phoneNumber) {
        return service.deleteVerifiedNumber(phoneNumber);
    }

}

