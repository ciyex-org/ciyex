package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTeXmlCallRecordingResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxTeXmlAccountRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/texml/Accounts/{accountSid}")
@RequiredArgsConstructor
public class TelnyxTeXmlAccountRecordingController {

    private final TelnyxTeXmlAccountRecordingService recordingService;

    // Fetch all recordings for the account
    @GetMapping("/Recordings")
    public ResponseEntity<TelnyxTeXmlCallRecordingListResponseDto> getAllRecordings(
            @PathVariable String accountSid,
            @RequestParam Map<String, String> queryParams) {
        TelnyxTeXmlCallRecordingListResponseDto response = recordingService.getAllRecordings(accountSid, queryParams);
        return ResponseEntity.ok(response);
    }

    // Fetch a single recording by recording ID
    @GetMapping("/Recordings/{recordingSid}")
    public ResponseEntity<TelnyxTeXmlCallRecordingResponseDto> getRecordingById(
            @PathVariable String accountSid,
            @PathVariable String recordingSid) {
        TelnyxTeXmlCallRecordingResponseDto response = recordingService.getRecordingById(accountSid, recordingSid);
        return ResponseEntity.ok(response);
    }

    // Delete a recording by ID
    @DeleteMapping("/Recordings/{recordingSid}")
    public ResponseEntity<Void> deleteRecordingById(
            @PathVariable String accountSid,
            @PathVariable String recordingSid) {
        recordingService.deleteRecordingById(accountSid, recordingSid);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    // ✅ New: Fetch recordings for a conference
    @GetMapping("/Conferences/{conferenceSid}/Recordings")
    public ResponseEntity<TelnyxTeXmlCallRecordingListResponseDto> getConferenceRecordings(
            @PathVariable String accountSid,
            @PathVariable String conferenceSid) {
        TelnyxTeXmlCallRecordingListResponseDto response = recordingService.getConferenceRecordings(accountSid, conferenceSid);
        return ResponseEntity.ok(response);
    }
}
