package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomRecordingDeleteResponseDto;
import com.qiaben.ciyex.service.telnyx.RoomRecordingDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/room-recordings")
@RequiredArgsConstructor
public class RoomRecordingDeleteController {

    private final RoomRecordingDeleteService roomRecordingDeleteService;

    @DeleteMapping
    public RoomRecordingDeleteResponseDto deleteRoomRecordings(@RequestParam Map<String, String> filters) {
        return roomRecordingDeleteService.deleteRoomRecordings(filters);
    }
}
