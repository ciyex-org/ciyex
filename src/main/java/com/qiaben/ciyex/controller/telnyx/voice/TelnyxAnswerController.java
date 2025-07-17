package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAnswerRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAnswerResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/answer")
@RequiredArgsConstructor
public class TelnyxAnswerController {

    private final TelnyxAnswerService answerService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxAnswerResponseDTO> answer(
            @PathVariable String callControlId,
            @RequestBody TelnyxAnswerRequestDTO body
    ) {
        return ResponseEntity.ok(answerService.answerCall(callControlId, body));
    }
}
