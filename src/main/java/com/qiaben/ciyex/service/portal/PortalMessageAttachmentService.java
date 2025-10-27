package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.MessageAttachmentDto;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalMessageAttachmentDto;
import com.qiaben.ciyex.entity.MessageAttachment;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.MessageAttachmentRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.MessageAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalMessageAttachmentService {

    private final MessageAttachmentRepository messageAttachmentRepository;
    private final PortalUserRepository portalUserRepository;
    private final MessageAttachmentService messageAttachmentService;

    /**
     * Get all attachments for a specific message (portal user access)
     */
    public ApiResponse<List<PortalMessageAttachmentDto>> getMessageAttachments(Long portalUserId, Long messageId) {
        try {
            PortalUser portalUser = portalUserRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<PortalMessageAttachmentDto>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<PortalMessageAttachmentDto>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<PortalMessageAttachmentDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();
            Long orgId = portalUser.getOrgId();

            // Get message attachments for this message
            List<MessageAttachment> attachments = messageAttachmentRepository.findAllByOrgIdAndMessageId(orgId, messageId);

            // Filter to only show attachments for messages involving this patient
            // (You might want to add additional validation here to ensure the message belongs to this patient)

            List<PortalMessageAttachmentDto> portalAttachments = attachments.stream()
                    .map(attachment -> toPortalMessageAttachmentDto(attachment, portalUserId))
                    .collect(Collectors.toList());

            return ApiResponse.<List<PortalMessageAttachmentDto>>builder()
                    .success(true)
                    .message("Message attachments retrieved successfully")
                    .data(portalAttachments)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving message attachments for portal user: {}, message: {}", portalUserId, messageId, e);
            return ApiResponse.<List<PortalMessageAttachmentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve message attachments")
                    .build();
        }
    }

    /**
     * Upload attachment to a message (portal user access)
     */
    public ApiResponse<PortalMessageAttachmentDto> uploadMessageAttachment(
            Long portalUserId,
            Long messageId,
            MessageAttachmentDto dto,
            MultipartFile file) {

        try {
            PortalUser portalUser = portalUserRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<PortalMessageAttachmentDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<PortalMessageAttachmentDto>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            Long orgId = portalUser.getOrgId();

            // Upload the attachment using the regular service
            MessageAttachmentDto createdAttachment = messageAttachmentService.create(orgId, messageId, dto, file);

            // Convert to portal DTO
            PortalMessageAttachmentDto portalAttachment = toPortalMessageAttachmentDto(
                messageAttachmentRepository.findById(createdAttachment.getId()).orElse(null),
                portalUserId
            );

            return ApiResponse.<PortalMessageAttachmentDto>builder()
                    .success(true)
                    .message("Attachment uploaded successfully")
                    .data(portalAttachment)
                    .build();

        } catch (Exception e) {
            log.error("Error uploading message attachment for portal user: {}, message: {}", portalUserId, messageId, e);
            return ApiResponse.<PortalMessageAttachmentDto>builder()
                    .success(false)
                    .message("Failed to upload attachment")
                    .build();
        }
    }

    /**
     * Delete message attachment (portal user access)
     */
    public ApiResponse<Void> deleteMessageAttachment(Long portalUserId, Long messageId, Long attachmentId) {
        try {
            PortalUser portalUser = portalUserRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<Void>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<Void>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            Long orgId = portalUser.getOrgId();

            // Delete the attachment using the regular service
            messageAttachmentService.delete(orgId, attachmentId);

            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Attachment deleted successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error deleting message attachment for portal user: {}, attachment: {}", portalUserId, attachmentId, e);
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete attachment")
                    .build();
        }
    }

    /**
     * Convert MessageAttachment entity to PortalMessageAttachmentDto
     */
    private PortalMessageAttachmentDto toPortalMessageAttachmentDto(MessageAttachment attachment, Long portalUserId) {
        if (attachment == null) return null;

        // Calculate file size from stored value or S3
        String fileSize = "Unknown";
        if (attachment.getFileSize() != null && attachment.getFileSize() > 0) {
            fileSize = formatFileSize(attachment.getFileSize());
        }

        return PortalMessageAttachmentDto.builder()
                .id(attachment.getId())
                .messageId(attachment.getMessageId())
                .orgId(attachment.getOrgId())
                .category(attachment.getCategory())
                .type(attachment.getType())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .description(attachment.getDescription())
                .createdDate(attachment.getCreatedDate() != null ? attachment.getCreatedDate().toString() : null)
                .lastModifiedDate(attachment.getLastModifiedDate() != null ? attachment.getLastModifiedDate().toString() : null)
                .encrypted(attachment.getEncryptionKey() != null && attachment.getIv() != null)
                .downloadUrl("/api/portal/messages/" + attachment.getMessageId() + "/attachments/" + attachment.getId() + "/download")
                .fileSize(fileSize)
                .uploadedBy("Patient") // For now, assume patient uploaded - you might want to track this in DB
                .build();
    }

    /**
     * Format file size in human readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    public MessageAttachmentService.DownloadResult getAttachmentDownload(Long portalUserId, Long messageId, Long attachmentId) {
        try {
            PortalUser portalUser = portalUserRepository.findById(portalUserId)
                    .orElseThrow(() -> new RuntimeException("Portal user not found"));

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                throw new RuntimeException("User not approved");
            }

            Long orgId = portalUser.getOrgId();

            // Get the download result using the regular service
            return messageAttachmentService.download(orgId, attachmentId);

        } catch (Exception e) {
            log.error("Error getting attachment download for portal user: {}, attachment: {}", portalUserId, attachmentId, e);
            throw new RuntimeException("Failed to download attachment", e);
        }
    }
}