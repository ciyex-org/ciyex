package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTextToSpeechDto.GenerateSpeechRequest;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTextToSpeechDto.VoiceListResponse;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxTextToSpeechService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/text-to-speech")
@RequiredArgsConstructor
public class TelnyxTextToSpeechController {

    private final TelnyxTextToSpeechService telnyxTextToSpeechService;

    @GetMapping("/voices")
    public VoiceListResponse listVoices(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false, name = "elevenlabs_api_key_ref") String apiKeyRef) {
        return telnyxTextToSpeechService.listVoices(provider, apiKeyRef);
    }

    @PostMapping(value = "/speech", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] generateSpeech(@RequestBody GenerateSpeechRequest request) {
        return telnyxTextToSpeechService.generateSpeech(request);
    }
}
