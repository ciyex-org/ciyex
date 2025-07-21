package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomRecordingDeleteResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomRecordingDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/room-recordings")
@RequiredArgsConstructor
public class TelnyxRoomRecordingDeleteController {

    private final TelnyxRoomRecordingDeleteService roomRecordingDeleteService;

    @DeleteMapping
    public TelnyxRoomRecordingDeleteResponseDto deleteRoomRecordings(@RequestParam Map<String, String> filters) {
        return roomRecordingDeleteService.deleteRoomRecordings(filters);
    }
}
