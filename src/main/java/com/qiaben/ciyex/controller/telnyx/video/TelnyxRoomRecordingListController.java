package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomRecordingListResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomRecordingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/room-recordings")
@RequiredArgsConstructor
public class TelnyxRoomRecordingListController {

    private final TelnyxRoomRecordingListService roomRecordingListService;

    @GetMapping
    public TelnyxRoomRecordingListResponseDto getRoomRecordings(@RequestParam Map<String, String> filters) {
        return roomRecordingListService.listRoomRecordings(filters);
    }
}
