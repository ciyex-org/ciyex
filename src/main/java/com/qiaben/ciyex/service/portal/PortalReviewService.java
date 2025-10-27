package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.PortalPendingUpdateDto;
import com.qiaben.ciyex.dto.portal.PortalUpdateRequest;
import com.qiaben.ciyex.entity.portal.PortalPendingUpdate;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalPendingUpdateRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.TenantDataMergeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling portal patient updates that require EHR staff review
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalReviewService {

    private final PortalPendingUpdateRepository pendingUpdateRepository;
    private final PortalUserRepository portalUserRepository;
    private final TenantDataMergeService tenantDataMergeService;

    /**
     * Patient submits an update for EHR staff review
     */
    @Transactional
    public Long submitForReview(Long userId, PortalUpdateRequest request) {
        try {
            // Validate user exists
            PortalUser user = portalUserRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Create pending update record
            PortalPendingUpdate pendingUpdate = PortalPendingUpdate.builder()
                    .userId(userId)
                    .updateType(request.getUpdateType())
                    .payload(request.getChanges())
                    .hint(request.getHint())
                    .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                    .patientNotes(request.getPatientNotes())
                    .status("PENDING")
                    .build();

            PortalPendingUpdate saved = pendingUpdateRepository.save(pendingUpdate);
            
            log.info("✅ Update submitted for review - ID: {}, User: {}, Type: {}", 
                    saved.getId(), user.getEmail(), request.getUpdateType());

            // TODO: Optional - Send notification to EHR staff
            // notificationService.notifyEhrStaff(saved);

            return saved.getId();
            
        } catch (Exception e) {
            log.error("❌ Failed to submit update for review - User: {}, Type: {}", 
                    userId, request.getUpdateType(), e);
            throw new RuntimeException("Failed to submit update for review: " + e.getMessage());
        }
    }

    /**
     * Get all pending updates for EHR staff review
     */
    public List<PortalPendingUpdateDto> getAllPendingUpdates() {
        try {
            List<PortalPendingUpdate> pendingUpdates = pendingUpdateRepository.findByStatusOrderByCreatedDateDesc("PENDING");
            
            return pendingUpdates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("❌ Failed to get pending updates", e);
            throw new RuntimeException("Failed to retrieve pending updates: " + e.getMessage());
        }
    }

    /**
     * Get pending/approved/rejected updates for a specific patient
     */
    public List<PortalPendingUpdateDto> getPatientUpdates(Long userId) {
        try {
            List<PortalPendingUpdate> userUpdates = pendingUpdateRepository.findByUserIdOrderByCreatedDateDesc(userId);
            
            return userUpdates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("❌ Failed to get patient updates - User: {}", userId, e);
            throw new RuntimeException("Failed to retrieve patient updates: " + e.getMessage());
        }
    }

    /**
     * EHR staff approves an update and merges it into EHR system
     */
    @Transactional
    public void approveUpdate(Long updateId, String approverEmail, String approverNotes) {
        try {
            PortalPendingUpdate update = pendingUpdateRepository.findById(updateId)
                    .orElseThrow(() -> new IllegalArgumentException("Update not found: " + updateId));

            if (!"PENDING".equals(update.getStatus())) {
                throw new IllegalStateException("Update is not in PENDING status: " + update.getStatus());
            }

            // Get user info for tenant context
            PortalUser user = portalUserRepository.findById(update.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + update.getUserId()));

            // Merge data into appropriate EHR tenant schema
            tenantDataMergeService.mergeApprovedData(update);

            // Mark as approved
            update.approve(approverEmail, approverNotes);
            pendingUpdateRepository.save(update);

            log.info("✅ Update approved and merged - ID: {}, Type: {}, Approver: {}", 
                    updateId, update.getUpdateType(), approverEmail);

            // TODO: Optional - Notify patient of approval
            // notificationService.notifyPatientApproval(update, user);

        } catch (Exception e) {
            log.error("❌ Failed to approve update - ID: {}, Approver: {}", updateId, approverEmail, e);
            throw new RuntimeException("Failed to approve update: " + e.getMessage());
        }
    }

    /**
     * EHR staff rejects an update
     */
    @Transactional
    public void rejectUpdate(Long updateId, String approverEmail, String rejectionReason) {
        try {
            PortalPendingUpdate update = pendingUpdateRepository.findById(updateId)
                    .orElseThrow(() -> new IllegalArgumentException("Update not found: " + updateId));

            if (!"PENDING".equals(update.getStatus())) {
                throw new IllegalStateException("Update is not in PENDING status: " + update.getStatus());
            }

            // Mark as rejected
            update.reject(approverEmail, rejectionReason, null);
            pendingUpdateRepository.save(update);

            log.info("⚠️ Update rejected - ID: {}, Type: {}, Reason: {}, Approver: {}", 
                    updateId, update.getUpdateType(), rejectionReason, approverEmail);

            // TODO: Optional - Notify patient of rejection
            // notificationService.notifyPatientRejection(update, rejectionReason);

        } catch (Exception e) {
            log.error("❌ Failed to reject update - ID: {}, Approver: {}", updateId, approverEmail, e);
            throw new RuntimeException("Failed to reject update: " + e.getMessage());
        }
    }

    /**
     * Convert entity to DTO with patient info
     */
    private PortalPendingUpdateDto convertToDto(PortalPendingUpdate update) {
        PortalUser user = portalUserRepository.findById(update.getUserId()).orElse(null);
        
        return PortalPendingUpdateDto.builder()
                .id(update.getId())
                .userId(update.getUserId())
                .patientName(user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown")
                .patientEmail(user != null ? user.getEmail() : "Unknown")
                .updateType(update.getUpdateType())
                .payload(update.getPayload())
                .hint(update.getHint())
                .priority(update.getPriority())
                .status(update.getStatus())
                .patientNotes(update.getPatientNotes())
                .approverNotes(update.getApproverNotes())
                .approvedBy(update.getApprovedBy())
                .rejectionReason(update.getRejectionReason())
                .createdDate(update.getCreatedDate())
                .reviewedDate(update.getReviewedDate())
                .build();
    }
}