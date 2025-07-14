package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.TeXmlConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/texml")
@RequiredArgsConstructor
public class TeXmlConferenceController {

    private final TeXmlConferenceService svc;

    @GetMapping("/accounts/{acc}/conferences/{conf}")
    public ResponseEntity<TeXmlConferenceDto> fetchConference(
            @PathVariable String acc, @PathVariable String conf) {
        return ResponseEntity.ok(svc.fetchConference(acc, conf));
    }

    @PostMapping("/accounts/{acc}/conferences/{conf}")
    public ResponseEntity<TeXmlConferenceDto> updateConference(
            @PathVariable String acc,
            @PathVariable String conf,
            @RequestBody TeXmlConferenceUpdateRequestDto body) {
        return ResponseEntity.ok(svc.updateConference(acc, conf, body));
    }

    @GetMapping("/accounts/{acc}/conferences")
    public ResponseEntity<TeXmlConferenceListResponseDto> listConferences(
            @PathVariable String acc, @RequestParam Map<String, String> qp) {
        return ResponseEntity.ok(svc.listConferences(acc, qp));
    }

    @GetMapping("/accounts/{acc}/conferences/{conf}/recordings")
    public ResponseEntity<TeXmlRecordingListResponseDto> listRecordings(
            @PathVariable String acc, @PathVariable String conf) {
        return ResponseEntity.ok(svc.listRecordings(acc, conf));
    }

    @GetMapping("/accounts/{acc}/conferences/{conf}/participants")
    public ResponseEntity<TeXmlParticipantListResponseDto> listParticipants(
            @PathVariable String acc, @PathVariable String conf) {
        return ResponseEntity.ok(svc.listParticipants(acc, conf));
    }

    @GetMapping("/accounts/{acc}/conferences/{conf}/participants/{id}")
    public ResponseEntity<TeXmlParticipantResponseDto> getParticipant(
            @PathVariable String acc,
            @PathVariable String conf,
            @PathVariable String id) {
        return ResponseEntity.ok(svc.getParticipant(acc, conf, id));
    }

    @PostMapping("/accounts/{acc}/conferences/{conf}/participants")
    public ResponseEntity<TeXmlParticipantResponseDto> dialParticipant(
            @PathVariable String acc,
            @PathVariable String conf,
            @RequestBody TeXmlDialParticipantRequestDto body) {
        return ResponseEntity.ok(svc.dialParticipant(acc, conf, body));
    }

    @PostMapping("/accounts/{acc}/conferences/{conf}/participants/{id}")
    public ResponseEntity<TeXmlParticipantResponseDto> updateParticipant(
            @PathVariable String acc,
            @PathVariable String conf,
            @PathVariable String id,
            @RequestBody TeXmlParticipantUpdateRequestDto body) {
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
