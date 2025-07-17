package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomRecordingViewResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomRecordingViewDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-recordings")
@RequiredArgsConstructor
public class TelnyxRoomRecordingViewDeleteController {

    private final TelnyxRoomRecordingViewDeleteService service;

    @GetMapping("/{id}")
    public ResponseEntity<TelnyxRoomRecordingViewResponseDto> getRoomRecording(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.viewRecording(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomRecording(@PathVariable("id") String id) {
        service.deleteRecording(id);
        return ResponseEntity.noContent().build();
    }
}
