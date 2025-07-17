package com.qiaben.ciyex.controller.telnyx.messaging;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCreateMessagingProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCreateMessagingProfileResponseDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxCreateMessagingProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/messaging-profiles")
@RequiredArgsConstructor
public class TelnyxCreateMessagingProfileController {

    private final TelnyxCreateMessagingProfileService service;

    @PostMapping
    public ResponseEntity<TelnyxCreateMessagingProfileResponseDto> createProfile(
            @RequestBody TelnyxCreateMessagingProfileRequestDto requestDto) {
        TelnyxCreateMessagingProfileResponseDto response = service.createMessagingProfile(requestDto);
        return ResponseEntity.ok(response);
    }
}
