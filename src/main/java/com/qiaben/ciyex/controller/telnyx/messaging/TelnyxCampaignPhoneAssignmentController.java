package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCampaignPhoneAssignmentListResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCampaignPhoneAssignmentRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCampaignPhoneAssignmentResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxCampaignPhoneAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign-phone-assignment")
@RequiredArgsConstructor
public class TelnyxCampaignPhoneAssignmentController {

    private final TelnyxCampaignPhoneAssignmentService service;

    @PostMapping
    public ResponseEntity<TelnyxCampaignPhoneAssignmentResponseDTO> assign(@RequestBody TelnyxCampaignPhoneAssignmentRequestDTO request) {
        return ResponseEntity.ok(service.assignPhoneToCampaign(request));
    }

    @GetMapping
    public ResponseEntity<TelnyxCampaignPhoneAssignmentListResponseDTO> getAssignments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer recordsPerPage,
            @RequestParam(required = false, name = "filter[telnyx_campaign_id]") String telnyxCampaignId,
            @RequestParam(required = false, name = "filter[telnyx_brand_id]") String telnyxBrandId,
            @RequestParam(required = false, name = "filter[tcr_campaign_id]") String tcrCampaignId,
            @RequestParam(required = false, name = "filter[tcr_brand_id]") String tcrBrandId,
            @RequestParam(required = false, name = "sort") String sort
    ) {
        return ResponseEntity.ok(service.getAssignments(page, recordsPerPage, telnyxCampaignId, telnyxBrandId, tcrCampaignId, tcrBrandId, sort));
    }

    @GetMapping("/{phoneNumber}")
    public ResponseEntity<TelnyxCampaignPhoneAssignmentResponseDTO> getByPhone(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(service.getAssignmentByPhoneNumber(phoneNumber));
    }

    @PutMapping("/{phoneNumber}")
    public ResponseEntity<TelnyxCampaignPhoneAssignmentResponseDTO> updateByPhone(
            @PathVariable String phoneNumber,
            @RequestBody TelnyxCampaignPhoneAssignmentRequestDTO request) {
        return ResponseEntity.ok(service.updateAssignmentByPhoneNumber(phoneNumber, request));
    }
}
