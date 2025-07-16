package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomRecordingViewResponseDto;
import com.qiaben.ciyex.service.telnyx.RoomRecordingViewDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-recordings")
@RequiredArgsConstructor
public class RoomRecordingViewDeleteController {

    private final RoomRecordingViewDeleteService service;

    @GetMapping("/{id}")
    public ResponseEntity<RoomRecordingViewResponseDto> getRoomRecording(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.viewRecording(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomRecording(@PathVariable("id") String id) {
        service.deleteRecording(id);
        return ResponseEntity.noContent().build();
    }
}
