package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.CampaignPhoneAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign-phone-assignment")
@RequiredArgsConstructor
public class CampaignPhoneAssignmentController {

    private final CampaignPhoneAssignmentService service;

    @PostMapping
    public ResponseEntity<CampaignPhoneAssignmentResponseDTO> assign(@RequestBody CampaignPhoneAssignmentRequestDTO request) {
        return ResponseEntity.ok(service.assignPhoneToCampaign(request));
    }

    @GetMapping
    public ResponseEntity<CampaignPhoneAssignmentListResponseDTO> getAssignments(
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
    public ResponseEntity<CampaignPhoneAssignmentResponseDTO> getByPhone(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(service.getAssignmentByPhoneNumber(phoneNumber));
    }

    @PutMapping("/{phoneNumber}")
    public ResponseEntity<CampaignPhoneAssignmentResponseDTO> updateByPhone(
            @PathVariable String phoneNumber,
            @RequestBody CampaignPhoneAssignmentRequestDTO request) {
        return ResponseEntity.ok(service.updateAssignmentByPhoneNumber(phoneNumber, request));
    }
}
