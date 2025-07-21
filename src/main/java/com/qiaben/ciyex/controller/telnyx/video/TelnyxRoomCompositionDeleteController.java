package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomCompositionDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/room-compositions")
@RequiredArgsConstructor
public class TelnyxRoomCompositionDeleteController {

    private final TelnyxRoomCompositionDeleteService deleteService;

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteComposition(@PathVariable("id") String id) {
        boolean deleted = deleteService.deleteRoomComposition(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // 204
        } else {
            return ResponseEntity.notFound().build();  // 404
        }
    }
}
