package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomRecordingListResponseDto;
import com.qiaben.ciyex.service.telnyx.RoomRecordingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/room-recordings")
@RequiredArgsConstructor
public class RoomRecordingListController {

    private final RoomRecordingListService roomRecordingListService;

    @GetMapping
    public RoomRecordingListResponseDto getRoomRecordings(@RequestParam Map<String, String> filters) {
        return roomRecordingListService.listRoomRecordings(filters);
    }
}
