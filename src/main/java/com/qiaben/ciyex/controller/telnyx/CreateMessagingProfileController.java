package com.qiaben.ciyex.controller.telnyx;
import com.qiaben.ciyex.dto.telnyx.CreateMessagingProfileRequestDto;
import com.qiaben.ciyex.dto.telnyx.CreateMessagingProfileResponseDto;
import com.qiaben.ciyex.service.telnyx.CreateMessagingProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/messaging-profiles")
@RequiredArgsConstructor
public class CreateMessagingProfileController {

    private final CreateMessagingProfileService service;

    @PostMapping
    public ResponseEntity<CreateMessagingProfileResponseDto> createProfile(
            @RequestBody CreateMessagingProfileRequestDto requestDto) {
        CreateMessagingProfileResponseDto response = service.createMessagingProfile(requestDto);
        return ResponseEntity.ok(response);
    }
}
