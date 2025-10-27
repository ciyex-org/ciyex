package com.qiaben.ciyex.controller.portal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qiaben.ciyex.dto.ListOptionDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.service.ListOptionService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/portal/list-options")
@RequiredArgsConstructor
@CrossOrigin(
        origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
        allowedHeaders = "*",
        methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
        allowCredentials = "true"
)
public class PortalListOptionsController {

    private final ListOptionService listOptionService;

    /**
     * GET /api/portal/list-options/list/{listId}
     * Proxy to EHR list-options for the requested list id in tenant context.
     */
    @GetMapping("/list/{listId}")
    public ResponseEntity<ApiResponse<List<ListOptionDto>>> getListOptionsByListId(
            @PathVariable(value = "listId", required = true) String listId,
            HttpServletRequest request) {
        try {
            log.info("Portal fetching list-options for listId: {}", listId);
            List<ListOptionDto> rows = listOptionService.getListOptionsByListId(listId);

            log.info("Retrieved {} list options for listId: {}", rows.size(), listId);
            return ResponseEntity.ok(ApiResponse.<List<ListOptionDto>>builder()
                    .success(true)
                    .message("List options retrieved")
                    .data(rows)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching list-options for listId {}", listId, e);
            return ResponseEntity.ok(ApiResponse.<List<ListOptionDto>>builder()
                    .success(false)
                    .message("Failed to fetch list options: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Alternative query param form: /api/portal/list-options?list_id=...
     * When no list_id provided, return both visit_types and appointment_priorities
     */
    @GetMapping
    public ResponseEntity<?> getByQuery(
            @RequestParam(value = "list_id", required = false) String listId,
            HttpServletRequest request) {
        if (listId == null || listId.trim().isEmpty()) {
            // Return both visit_types and appointment_priorities in unified response
            try {
                log.info("Portal fetching unified list-options (visit_types + appointment_priorities)");

                List<ListOptionDto> visitTypes =listOptionService.getListOptionsByListId("visit_types");
                List<ListOptionDto> priorities = listOptionService.getListOptionsByListId("appointment_priorities");

                log.info("Retrieved {} visit types and {} priorities", visitTypes.size(), priorities.size());

                // Return unified response with both lists
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Unified list options retrieved")
                        .data(java.util.Map.of(
                            "visit_types", visitTypes,
                            "appointment_priorities", priorities
                        ))
                        .build());

            } catch (Exception e) {
                log.error("Error fetching unified list-options", e);
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Failed to fetch list options: " + e.getMessage())
                        .data(java.util.Map.of(
                            "visit_types", java.util.List.of(),
                            "appointment_priorities", java.util.List.of()
                        ))
                        .build());
            }
        }

        return getListOptionsByListId(listId, request);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(value.toString());
    }

    private void setRequestContextOrg(HttpServletRequest request) {
        // RequestContext is now set by TenantContextInterceptor
        // This method is kept for backward compatibility but does nothing
    }

}
