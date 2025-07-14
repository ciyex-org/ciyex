package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TeXmlSecretRequestDto;
import com.qiaben.ciyex.dto.telnyx.TeXmlSecretResponseDto;
import com.qiaben.ciyex.service.telnyx.TeXmlSecretService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/texml/secrets")
@RequiredArgsConstructor
public class TeXmlSecretController {

    private final TeXmlSecretService secretService;

    @PostMapping
    public ResponseEntity<TeXmlSecretResponseDto> createSecret(@RequestBody TeXmlSecretRequestDto requestDto) {
        TeXmlSecretResponseDto response = secretService.createSecret(requestDto);
        return ResponseEntity.status(201).body(response);
    }
}
