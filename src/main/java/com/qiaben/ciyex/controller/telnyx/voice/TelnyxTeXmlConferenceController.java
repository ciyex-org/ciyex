package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.*;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxTeXmlConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/texml")
@RequiredArgsConstructor
public class TelnyxTeXmlConferenceController {

    private final TelnyxTeXmlConferenceService svc;

    @GetMapping("/accounts/{acc}/conferences/{conf}")
    public ResponseEntity<TelnyxTeXmlConferenceDto> fetchConference(
            @PathVariable String acc, @PathVariable String conf) {
        return ResponseEntity.ok(svc.fetchConference(acc, conf));
    }

    @PostMapping("/accounts/{acc}/conferences/{conf}")
    public ResponseEntity<TelnyxTeXmlConferenceDto> updateConference(
            @PathVariable String acc,
            @PathVariable String conf,
            @RequestBody TelnyxTeXmlConferenceUpdateRequestDto body) {
        return ResponseEntity.ok(svc.updateConference(acc, conf, body));
    }

    @GetMapping("/accounts/{acc}/conferences")
    public ResponseEntity<TelnyxTeXmlConferenceListResponseDto> listConferences(
            @PathVariable String acc, @RequestParam Map<String, String> qp) {
        return ResponseEntity.ok(svc.listConferences(acc, qp));
    }

    @GetMapping("/accounts/{acc}/conferences/{conf}/recordings")
    public ResponseEntity<TelnyxTeXmlRecordingListResponseDto> listRecordings(
            @PathVariable String acc, @PathVariable String conf) {
        return ResponseEntity.ok(svc.listRecordings(acc, conf));
    }

    @GetMapping("/accounts/{acc}/conferences/{conf}/participants")
    public ResponseEntity<TelnyxTeXmlParticipantListResponseDto> listParticipants(
            @PathVariable String acc, @PathVariable String conf) {
        return ResponseEntity.ok(svc.listParticipants(acc, conf));
    }

    @GetMapping("/accounts/{acc}/conferences/{conf}/participants/{id}")
    public ResponseEntity<TelnyxTeXmlParticipantResponseDto> getParticipant(
            @PathVariable String acc,
            @PathVariable String conf,
            @PathVariable String id) {
        return ResponseEntity.ok(svc.getParticipant(acc, conf, id));
    }

    @PostMapping("/accounts/{acc}/conferences/{conf}/participants")
    public ResponseEntity<TelnyxTeXmlParticipantResponseDto> dialParticipant(
            @PathVariable String acc,
            @PathVariable String conf,
            @RequestBody TelnyxTeXmlDialParticipantRequestDto body) {
        return ResponseEntity.ok(svc.dialParticipant(acc, conf, body));
    }

    @PostMapping("/accounts/{acc}/conferences/{conf}/participants/{id}")
    public ResponseEntity<TelnyxTeXmlParticipantResponseDto> updateParticipant(
            @PathVariable String acc,
            @PathVariable String conf,
            @PathVariable String id,
            @RequestBody TelnyxTeXmlParticipantUpdateRequestDto body) {
        return ResponseEntity.ok(svc.updateParticipant(acc, conf, id, body));
    }

    @DeleteMapping("/accounts/{acc}/conferences/{conf}/participants/{id}")
    public ResponseEntity<Void> deleteParticipant(
            @PathVariable String acc,
            @PathVariable String conf,
            @PathVariable String id) {
        svc.deleteParticipant(acc, conf, id);
        return ResponseEntity.noContent().build();
    }
}
