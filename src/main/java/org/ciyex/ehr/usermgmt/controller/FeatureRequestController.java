package org.ciyex.ehr.usermgmt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.usermgmt.dto.FeatureRequestDto;
import org.ciyex.ehr.usermgmt.entity.FeatureRequest;
import org.ciyex.ehr.usermgmt.repository.FeatureRequestRepository;
import org.ciyex.ehr.usermgmt.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/feature-requests")
@RequiredArgsConstructor
@Slf4j
public class FeatureRequestController {

    private final FeatureRequestRepository repo;
    private final EmailService emailService;

    private static final String TARGET_EMAIL = "help@ciyex.org";

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(@RequestBody FeatureRequestDto dto) {
        try {
            String orgAlias = RequestContext.get().getOrgName();

            // Save to database
            var entity = FeatureRequest.builder()
                    .category(dto.getCategory() != null ? dto.getCategory() : "feature_request")
                    .subject(dto.getSubject())
                    .description(dto.getDescription())
                    .userEmail(dto.getUserEmail())
                    .userName(dto.getUserName())
                    .orgAlias(orgAlias)
                    .build();
            repo.save(entity);

            // Send email
            String categoryLabel = switch (dto.getCategory() != null ? dto.getCategory() : "") {
                case "bug_report" -> "Bug Report";
                case "improvement" -> "Improvement";
                default -> "Feature Request";
            };

            String html = """
                    <div style="font-family: sans-serif; max-width: 600px;">
                        <h2 style="color: #1e40af;">%s</h2>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr><td style="padding: 8px; font-weight: bold; width: 120px;">From:</td><td style="padding: 8px;">%s (%s)</td></tr>
                            <tr><td style="padding: 8px; font-weight: bold;">Organization:</td><td style="padding: 8px;">%s</td></tr>
                            <tr><td style="padding: 8px; font-weight: bold;">Subject:</td><td style="padding: 8px;">%s</td></tr>
                        </table>
                        <div style="margin-top: 16px; padding: 16px; background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0;">
                            <p style="white-space: pre-wrap;">%s</p>
                        </div>
                    </div>
                    """.formatted(
                    categoryLabel,
                    dto.getUserName() != null ? dto.getUserName() : "Unknown",
                    dto.getUserEmail() != null ? dto.getUserEmail() : "No email",
                    orgAlias,
                    dto.getSubject(),
                    dto.getDescription() != null ? dto.getDescription() : "No details provided"
            );

            emailService.sendEmail(TARGET_EMAIL,
                    "[Ciyex] " + categoryLabel + ": " + dto.getSubject(), html);

            return ResponseEntity.ok(ApiResponse.ok("Feature request submitted",
                    Map.of("id", entity.getId(), "status", "submitted")));
        } catch (Exception e) {
            log.error("Failed to submit feature request", e);
            // Still return success if saved but email failed
            return ResponseEntity.ok(ApiResponse.ok("Request saved (email delivery pending)",
                    Map.of("status", "saved")));
        }
    }
}
