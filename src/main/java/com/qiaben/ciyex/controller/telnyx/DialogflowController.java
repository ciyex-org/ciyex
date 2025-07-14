package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.DialogflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/dialogflow")
@RequiredArgsConstructor
public class DialogflowController {

    private final DialogflowService sendDialogflowService;

    @PostMapping
    public DialogflowResponseDTO sendDialogflow(@RequestBody DialogflowRequestDTO dto) {
        return sendDialogflowService.createDialogflowConnection(dto);
    }

    @GetMapping("/{connectionId}")
    public DialogflowResponseDTO getDialogflow(@PathVariable String connectionId) {
        return sendDialogflowService.getDialogflowConnection(connectionId);
    }

    @PutMapping("/{connectionId}")
    public DialogflowResponseDTO updateDialogflow(@PathVariable String connectionId, @RequestBody DialogflowRequestDTO dto) {
        return sendDialogflowService.updateDialogflowConnection(connectionId, dto);
    }
    @DeleteMapping("/{connectionId}")
    public void deleteDialogflow(@PathVariable String connectionId) {
        sendDialogflowService.deleteDialogflowConnection(connectionId);
    }


}
