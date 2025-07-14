package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TextToSpeechDto.GenerateSpeechRequest;
import com.qiaben.ciyex.dto.telnyx.TextToSpeechDto.VoiceListResponse;
import com.qiaben.ciyex.service.telnyx.TextToSpeechService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/text-to-speech")
@RequiredArgsConstructor
public class TextToSpeechController {

    private final TextToSpeechService textToSpeechService;

    @GetMapping("/voices")
    public VoiceListResponse listVoices(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false, name = "elevenlabs_api_key_ref") String apiKeyRef) {
        return textToSpeechService.listVoices(provider, apiKeyRef);
    }

    @PostMapping(value = "/speech", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] generateSpeech(@RequestBody GenerateSpeechRequest request) {
        return textToSpeechService.generateSpeech(request);
    }
}
