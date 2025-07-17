package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlSecretRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlSecretResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxTeXmlSecretService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/texml/secrets")
@RequiredArgsConstructor
public class TelnyxTeXmlSecretController {

    private final TelnyxTeXmlSecretService secretService;

    @PostMapping
    public ResponseEntity<TelnyxTeXmlSecretResponseDto> createSecret(@RequestBody TelnyxTeXmlSecretRequestDto requestDto) {
        TelnyxTeXmlSecretResponseDto response = secretService.createSecret(requestDto);
        return ResponseEntity.status(201).body(response);
    }
}
