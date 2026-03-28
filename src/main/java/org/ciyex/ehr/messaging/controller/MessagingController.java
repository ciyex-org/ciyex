package org.ciyex.ehr.messaging.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.messaging.service.MessagingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.read', 'SCOPE_patient/Communication.read')")
@RestController
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService messagingService;

    private String userId(Jwt jwt) {
        return jwt.getSubject();
    }

    private String userName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        if (name == null || name.isBlank()) {
            String given = jwt.getClaimAsString("given_name");
            String family = jwt.getClaimAsString("family_name");
            if (given != null || family != null) {
                name = ((given != null ? given : "") + " " + (family != null ? family : "")).trim();
            }
        }
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("email");
        }
        return name != null && !name.isBlank() ? name : "Unknown";
    }

    // ==================== Channels ====================

    @GetMapping("/api/channels")
    public ResponseEntity<Map<String, Object>> getChannels(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        List<Map<String, Object>> channels = messagingService.getChannels(userId(jwt), email);
        return ResponseEntity.ok(Map.of("success", true, "data", channels));
    }

    public record CreateChannelRequest(String name, String type, String topic, List<String> memberIds, Map<String, String> memberNames) {}

    @PostMapping("/api/channels")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Map<String, Object>> createChannel(
            @RequestBody CreateChannelRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> channel = messagingService.createChannel(
                req.name(), req.type(), req.topic(), req.memberIds(),
                req.memberNames(), userId(jwt), userName(jwt));
        return ResponseEntity.ok(Map.of("success", true, "data", channel));
    }

    public record UpdateChannelRequest(String name, String topic, String description) {}

    @PutMapping("/api/channels/{channelId}")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Map<String, Object>> updateChannel(
            @PathVariable UUID channelId,
            @RequestBody UpdateChannelRequest req) {
        Map<String, Object> channel = messagingService.updateChannel(channelId, req.name(), req.topic(), req.description());
        return ResponseEntity.ok(Map.of("success", true, "data", channel));
    }

    @DeleteMapping("/api/channels/{channelId}")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> deleteChannel(@PathVariable UUID channelId) {
        messagingService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

    public record StartDmRequest(String targetUserId, String targetUserName) {}

    @PostMapping("/api/channels/dm")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Map<String, Object>> startDm(
            @RequestBody StartDmRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> channel = messagingService.findOrCreateDm(
                userId(jwt), userName(jwt), req.targetUserId(), req.targetUserName());
        return ResponseEntity.ok(Map.of("success", true, "data", channel));
    }

    // ==================== Messages ====================

    @GetMapping("/api/channels/{channelId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        List<Map<String, Object>> messages = messagingService.getMessages(channelId, limit, userId(jwt));
        return ResponseEntity.ok(Map.of("success", true, "data", messages));
    }

    public record SendMessageRequest(String content, String parentId, List<String> mentions) {}

    @PostMapping("/api/channels/{channelId}/messages")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable UUID channelId,
            @RequestBody SendMessageRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        UUID parentId = req.parentId() != null && !req.parentId().isBlank()
                ? UUID.fromString(req.parentId()) : null;
        Map<String, Object> msg = messagingService.sendMessage(
                channelId, userId(jwt), userName(jwt), req.content(), parentId, req.mentions());
        return ResponseEntity.ok(Map.of("success", true, "data", msg));
    }

    public record EditMessageRequest(String content) {}

    @PutMapping("/api/messages/{messageId}")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Map<String, Object>> editMessage(
            @PathVariable UUID messageId,
            @RequestBody EditMessageRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> msg = messagingService.editMessage(messageId, req.content(), userId(jwt));
        return ResponseEntity.ok(Map.of("success", true, "data", msg));
    }

    @DeleteMapping("/api/messages/{messageId}")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal Jwt jwt) {
        messagingService.deleteMessage(messageId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    // ==================== Threads ====================

    @GetMapping("/api/messages/{messageId}/thread")
    public ResponseEntity<Map<String, Object>> getThreadReplies(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal Jwt jwt) {
        List<Map<String, Object>> replies = messagingService.getThreadReplies(messageId, userId(jwt));
        return ResponseEntity.ok(Map.of("success", true, "data", replies));
    }

    // ==================== Reactions ====================

    public record ReactionRequest(String emoji) {}

    @PostMapping("/api/messages/{messageId}/reactions")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> addReaction(
            @PathVariable UUID messageId,
            @RequestBody ReactionRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        messagingService.addReaction(messageId, userId(jwt), req.emoji());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/messages/{messageId}/reactions")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> removeReaction(
            @PathVariable UUID messageId,
            @RequestBody ReactionRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        messagingService.removeReaction(messageId, userId(jwt), req.emoji());
        return ResponseEntity.ok().build();
    }

    // ==================== Pins ====================

    @PostMapping("/api/messages/{messageId}/pin")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> pinMessage(@PathVariable UUID messageId) {
        messagingService.pinMessage(messageId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/messages/{messageId}/pin")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> unpinMessage(@PathVariable UUID messageId) {
        messagingService.unpinMessage(messageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/channels/{channelId}/pinned")
    public ResponseEntity<Map<String, Object>> getPinnedMessages(
            @PathVariable UUID channelId,
            @AuthenticationPrincipal Jwt jwt) {
        List<Map<String, Object>> pinned = messagingService.getPinnedMessages(channelId, userId(jwt));
        return ResponseEntity.ok(Map.of("success", true, "data", pinned));
    }

    // ==================== Members ====================

    @GetMapping("/api/channels/{channelId}/members")
    public ResponseEntity<Map<String, Object>> getChannelMembers(@PathVariable UUID channelId) {
        List<Map<String, Object>> members = messagingService.getChannelMembers(channelId);
        return ResponseEntity.ok(Map.of("success", true, "data", members));
    }

    public record AddMemberRequest(String userId, String displayName) {}

    @PostMapping("/api/channels/{channelId}/members")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> addMember(
            @PathVariable UUID channelId,
            @RequestBody AddMemberRequest req) {
        String name = (req.displayName() != null && !req.displayName().isBlank())
                ? req.displayName() : req.userId();
        messagingService.addMember(channelId, req.userId(), name, "member");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/channels/{channelId}/members/{userId}")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID channelId,
            @PathVariable String userId) {
        messagingService.removeMember(channelId, userId);
        return ResponseEntity.ok().build();
    }

    // ==================== Search ====================

    @GetMapping("/api/messages/search")
    public ResponseEntity<Map<String, Object>> searchMessages(
            @RequestParam String q,
            @RequestParam(required = false) String channelId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID chId = channelId != null && !channelId.isBlank() ? UUID.fromString(channelId) : null;
        List<Map<String, Object>> results = messagingService.searchMessages(q, chId, userId(jwt));
        return ResponseEntity.ok(Map.of("success", true, "data", results));
    }

    // ==================== Mark Read ====================

    @PostMapping("/api/channels/{channelId}/read")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID channelId,
            @AuthenticationPrincipal Jwt jwt) {
        messagingService.markChannelRead(channelId, userId(jwt));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/channels/{channelId}/unread")
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Void> markUnread(
            @PathVariable UUID channelId,
            @AuthenticationPrincipal Jwt jwt) {
        messagingService.markChannelUnread(channelId, userId(jwt));
        return ResponseEntity.ok().build();
    }

    // ==================== Attachments ====================

    @PostMapping(value = "/api/messages/{messageId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('SCOPE_user/Communication.write', 'SCOPE_patient/Communication.write')")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable UUID messageId,
            @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> att = messagingService.uploadAttachment(messageId, file);
            return ResponseEntity.ok(Map.of("success", true, "data", att));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/messages/{messageId}/attachments/{attachmentId}/download")
    public ResponseEntity<Map<String, Object>> getAttachmentUrl(
            @PathVariable UUID messageId,
            @PathVariable UUID attachmentId) {
        try {
            Map<String, Object> att = messagingService.getAttachment(messageId, attachmentId);
            return ResponseEntity.ok(Map.of("success", true, "data", att));
        } catch (Exception e) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
