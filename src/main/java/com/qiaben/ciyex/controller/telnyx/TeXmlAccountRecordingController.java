package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TeXmlCallRecordingListResponseDto;
import com.qiaben.ciyex.dto.telnyx.TeXmlCallRecordingResponseDto;
import com.qiaben.ciyex.service.telnyx.TeXmlAccountRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/texml/Accounts/{accountSid}")
@RequiredArgsConstructor
public class TeXmlAccountRecordingController {

    private final TeXmlAccountRecordingService recordingService;

    // Fetch all recordings for the account
    @GetMapping("/Recordings")
    public ResponseEntity<TeXmlCallRecordingListResponseDto> getAllRecordings(
            @PathVariable String accountSid,
            @RequestParam Map<String, String> queryParams) {
        TeXmlCallRecordingListResponseDto response = recordingService.getAllRecordings(accountSid, queryParams);
        return ResponseEntity.ok(response);
    }

    // Fetch a single recording by recording ID
    @GetMapping("/Recordings/{recordingSid}")
    public ResponseEntity<TeXmlCallRecordingResponseDto> getRecordingById(
            @PathVariable String accountSid,
            @PathVariable String recordingSid) {
        TeXmlCallRecordingResponseDto response = recordingService.getRecordingById(accountSid, recordingSid);
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
    public ResponseEntity<TeXmlCallRecordingListResponseDto> getConferenceRecordings(
            @PathVariable String accountSid,
            @PathVariable String conferenceSid) {
        TeXmlCallRecordingListResponseDto response = recordingService.getConferenceRecordings(accountSid, conferenceSid);
        return ResponseEntity.ok(response);
    }
}
