package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SubscriptionDto;
import com.qiaben.ciyex.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @PostMapping
    public ApiResponse<SubscriptionDto> create(@RequestBody SubscriptionDto dto,
                                               @RequestHeader(value = "X-Org-Id", required = false) Long orgIdHeader,
                                               @RequestHeader(value = "orgId", required = false) Long orgIdAlt,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        Long orgId = orgIdHeader != null ? orgIdHeader : orgIdAlt;
        if (orgId == null) {
            throw new RuntimeException("OrgId header is required (X-Org-Id or orgId)");
        }

        dto.setOrgId(orgId);
        dto.setUserId(userId);

        return ApiResponse.<SubscriptionDto>builder()
                .success(true)
                .message("Subscription created successfully")
                .data(service.create(dto))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<SubscriptionDto> update(@PathVariable Long id,
                                               @RequestBody SubscriptionDto dto,
                                               @RequestHeader(value = "X-Org-Id", required = false) Long orgIdHeader,
                                               @RequestHeader(value = "orgId", required = false) Long orgIdAlt,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        Long orgId = orgIdHeader != null ? orgIdHeader : orgIdAlt;
        if (orgId == null) {
            throw new RuntimeException("OrgId header is required (X-Org-Id or orgId)");
        }

        dto.setOrgId(orgId);
        dto.setUserId(userId);

        return ApiResponse.<SubscriptionDto>builder()
                .success(true)
                .message("Subscription updated successfully")
                .data(service.update(id, dto))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Deleted successfully")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionDto> getById(@PathVariable Long id,
                                                @RequestHeader(value = "X-Org-Id", required = false) Long orgIdHeader,
                                                @RequestHeader(value = "orgId", required = false) Long orgIdAlt) {
        Long orgId = orgIdHeader != null ? orgIdHeader : orgIdAlt;
        if (orgId == null) {
            throw new RuntimeException("OrgId header is required (X-Org-Id or orgId)");
        }

        return service.getByIdAndOrg(id, orgId)
                .map(dto -> ApiResponse.<SubscriptionDto>builder()
                        .success(true)
                        .message("Subscription retrieved successfully")
                        .data(dto)
                        .build())
                .orElse(ApiResponse.<SubscriptionDto>builder()
                        .success(false)
                        .message("Not found")
                        .build());
    }

    @GetMapping
    public ApiResponse<List<SubscriptionDto>> getAll(@RequestHeader(value = "X-Org-Id", required = false) Long orgIdHeader,
                                                     @RequestHeader(value = "orgId", required = false) Long orgIdAlt) {
        Long orgId = orgIdHeader != null ? orgIdHeader : orgIdAlt;
        if (orgId == null) {
            throw new RuntimeException("OrgId header is required (X-Org-Id or orgId)");
        }

        return ApiResponse.<List<SubscriptionDto>>builder()
                .success(true)
                .message("Subscriptions retrieved successfully")
                .data(service.getAllByOrg(orgId))
                .build();
    }
}
