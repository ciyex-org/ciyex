package org.ciyex.ehr.messaging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.messaging.entity.*;
import org.ciyex.ehr.messaging.repository.*;
import org.ciyex.ehr.service.KeycloakAdminService;
import org.ciyex.ehr.service.KeycloakUserService;
import org.ciyex.ehr.service.storage.FileStorageStrategyResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {

    private final ChannelRepository channelRepo;
    private final ChannelMemberRepository memberRepo;
    private final MessageRepository messageRepo;
    private final MessageReactionRepository reactionRepo;
    private final MessageAttachmentRepository attachmentRepo;
    private final FileStorageStrategyResolver storageResolver;
    private final SecureMessageNotificationService secureMessageNotifier;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakUserService keycloakUserService;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ==================== Channels ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChannels(String userId, String email) {
        // Search by both Keycloak UUID and email to find channels created with either identifier
        List<String> userIds = new ArrayList<>();
        userIds.add(userId);
        if (email != null && !email.isBlank() && !email.equals(userId)) {
            userIds.add(email);
        }
        List<Channel> channels = channelRepo.findAccessibleChannels(orgAlias(), userIds);

        // Migrate any channel_member records that use email to use the Keycloak UUID
        for (Channel ch : channels) {
            if (email != null && !email.isBlank()) {
                memberRepo.findByChannelIdAndUserId(ch.getId(), email).ifPresent(m -> {
                    m.setUserId(userId);
                    memberRepo.save(m);
                    log.debug("Migrated channel member from email {} to Keycloak UUID {} in channel {}", email, userId, ch.getId());
                });
            }
        }

        return channels.stream().map(c -> toChannelDto(c, userId)).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createChannel(String name, String type, String topic,
                                              List<String> memberIds, Map<String, String> memberNames,
                                              String creatorId, String creatorName) {
        Channel ch = Channel.builder()
                .name(name)
                .type(type)
                .topic(topic)
                .createdBy(creatorId)
                .orgAlias(orgAlias())
                .build();
        ch = channelRepo.save(ch);

        // Add creator as owner
        addMember(ch.getId(), creatorId, creatorName, "owner");

        // Add specified members
        if (memberIds != null) {
            for (String memberId : memberIds) {
                if (!memberId.equals(creatorId)) {
                    String displayName = (memberNames != null && memberNames.containsKey(memberId))
                            ? memberNames.get(memberId) : memberId;
                    addMember(ch.getId(), memberId, displayName, "member");
                }
            }
        }

        // System message
        createSystemMessage(ch.getId(), creatorName + " created this channel", "channel_created", creatorId);

        return toChannelDto(ch, creatorId);
    }

    @Transactional
    public Map<String, Object> updateChannel(UUID channelId, String name, String topic, String description) {
        Channel ch = channelRepo.findById(channelId)
                .orElseThrow(() -> new NoSuchElementException("Channel not found"));
        if (name != null) ch.setName(name);
        if (topic != null) ch.setTopic(topic);
        if (description != null) ch.setDescription(description);
        ch = channelRepo.save(ch);
        return toChannelDto(ch, null);
    }

    @Transactional
    public void deleteChannel(UUID channelId) {
        channelRepo.deleteById(channelId);
    }

    @Transactional
    public Map<String, Object> findOrCreateDm(String currentUserId, String currentUserName,
                                               String targetUserId, String targetUserName) {
        // Resolve targetUserId to Keycloak UUID if it's an email or non-UUID identifier
        String resolvedTargetId = resolveToKeycloakId(targetUserId);

        // Check if DM already exists
        var existing = channelRepo.findDmBetweenUsers(orgAlias(), currentUserId, resolvedTargetId);
        if (existing.isPresent()) {
            return toChannelDto(existing.get(), currentUserId);
        }
        // Also check with original targetUserId in case old channels used the unresolved ID
        if (!resolvedTargetId.equals(targetUserId)) {
            var existingOld = channelRepo.findDmBetweenUsers(orgAlias(), currentUserId, targetUserId);
            if (existingOld.isPresent()) {
                // Migrate old member to use the resolved Keycloak UUID
                memberRepo.findByChannelIdAndUserId(existingOld.get().getId(), targetUserId)
                        .ifPresent(m -> {
                            m.setUserId(resolvedTargetId);
                            memberRepo.save(m);
                        });
                return toChannelDto(existingOld.get(), currentUserId);
            }
        }
        // Create new DM channel - name is the other person's name
        Channel ch = Channel.builder()
                .name(targetUserName)
                .type("dm")
                .createdBy(currentUserId)
                .orgAlias(orgAlias())
                .build();
        ch = channelRepo.save(ch);
        // Add both users — always use resolved Keycloak UUID
        addMember(ch.getId(), currentUserId, currentUserName, "owner");
        addMember(ch.getId(), resolvedTargetId, targetUserName, "member");
        return toChannelDto(ch, currentUserId);
    }

    /**
     * Resolve a user identifier (email, FHIR ID, or provider-{id}) to a Keycloak UUID.
     * If the identifier is already a valid UUID, returns it as-is.
     * Falls back to the original identifier if Keycloak lookup fails.
     */
    private String resolveToKeycloakId(String userId) {
        if (userId == null || userId.isBlank()) return userId;

        // Already a UUID — return as-is
        try {
            UUID.fromString(userId);
            return userId;
        } catch (IllegalArgumentException ignored) {
            // Not a UUID — try to resolve
        }

        // If it looks like an email, look up in Keycloak
        if (userId.contains("@")) {
            try {
                var users = keycloakAdminService.searchUserByEmail(userId);
                if (!users.isEmpty()) {
                    String kcId = String.valueOf(users.get(0).get("id"));
                    log.debug("Resolved email {} to Keycloak UUID {}", userId, kcId);
                    return kcId;
                }
            } catch (Exception e) {
                log.warn("Failed to resolve email {} to Keycloak UUID: {}", userId, e.getMessage());
            }
        }

        // Try resolving as a FHIR patient or practitioner ID via Keycloak user attributes
        try {
            // Try patient_fhir_id first, then practitioner_fhir_id
            Map<String, Object> user = keycloakUserService.findUserByAttribute("patient_fhir_id", userId);
            if (user == null) {
                user = keycloakUserService.findUserByAttribute("practitioner_fhir_id", userId);
            }
            if (user != null && user.get("id") != null) {
                String kcId = String.valueOf(user.get("id"));
                log.debug("Resolved FHIR ID {} to Keycloak UUID {}", userId, kcId);
                return kcId;
            }
        } catch (Exception e) {
            log.warn("Failed to resolve FHIR ID {} to Keycloak UUID: {}", userId, e.getMessage());
        }

        log.debug("Could not resolve userId {} to Keycloak UUID, using as-is", userId);
        return userId;
    }

    // ==================== Messages ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMessages(UUID channelId, int limit, String currentUserId) {
        List<Message> messages;
        if (limit > 0) {
            messages = messageRepo.findRecentMessages(channelId, PageRequest.of(0, limit));
            Collections.reverse(messages); // chronological order
        } else {
            messages = messageRepo.findByChannelIdAndDeletedFalseOrderByCreatedAtAsc(channelId);
        }
        return messages.stream().map(m -> toMessageDto(m, currentUserId)).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> sendMessage(UUID channelId, String senderId, String senderName,
                                            String content, UUID parentId, List<String> mentions) {
        Message msg = Message.builder()
                .channelId(channelId)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .parentId(parentId)
                .mentions(mentions != null ? mentions : List.of())
                .orgAlias(orgAlias())
                .build();
        msg = messageRepo.save(msg);

        // Mark channel as read for the sender so their own message doesn't appear as unread
        markChannelRead(channelId, senderId);

        // Fire async email/SMS notification to channel recipients
        try {
            String org = orgAlias();
            Channel channel = channelRepo.findById(channelId).orElse(null);
            String channelName = channel != null ? channel.getName() : null;
            secureMessageNotifier.onMessageSent(org, channelId, senderId, senderName, content, channelName);
        } catch (Exception e) {
            log.warn("Failed to trigger secure message notification: {}", e.getMessage());
        }

        return toMessageDto(msg, senderId);
    }

    @Transactional
    public Map<String, Object> editMessage(UUID messageId, String content, String userId) {
        Message msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));
        if (!msg.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("Cannot edit another user's message");
        }
        msg.setContent(content);
        msg.setEdited(true);
        msg = messageRepo.save(msg);
        return toMessageDto(msg, userId);
    }

    @Transactional
    public void deleteMessage(UUID messageId, String userId) {
        Message msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));
        msg.setDeleted(true);
        messageRepo.save(msg);
    }

    // ==================== Threads ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getThreadReplies(UUID parentId, String currentUserId) {
        List<Message> replies = messageRepo.findByParentIdAndDeletedFalseOrderByCreatedAtAsc(parentId);
        return replies.stream().map(m -> toMessageDto(m, currentUserId)).collect(Collectors.toList());
    }

    // ==================== Reactions ====================

    @Transactional
    public void addReaction(UUID messageId, String userId, String emoji) {
        if (reactionRepo.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji).isPresent()) {
            return; // already reacted
        }
        MessageReaction r = MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .orgAlias(orgAlias())
                .build();
        reactionRepo.save(r);
    }

    @Transactional
    public void removeReaction(UUID messageId, String userId, String emoji) {
        reactionRepo.deleteByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);
    }

    // ==================== Pins ====================

    @Transactional
    public void pinMessage(UUID messageId) {
        Message msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));
        msg.setPinned(true);
        messageRepo.save(msg);
    }

    @Transactional
    public void unpinMessage(UUID messageId) {
        Message msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));
        msg.setPinned(false);
        messageRepo.save(msg);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPinnedMessages(UUID channelId, String currentUserId) {
        List<Message> pinned = messageRepo.findPinnedMessages(channelId);
        return pinned.stream().map(m -> toMessageDto(m, currentUserId)).collect(Collectors.toList());
    }

    // ==================== Members ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChannelMembers(UUID channelId) {
        return memberRepo.findByChannelIdOrderByDisplayNameAsc(channelId).stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addMember(UUID channelId, String userId, String displayName, String role) {
        if (memberRepo.existsByChannelIdAndUserId(channelId, userId)) return;
        ChannelMember m = ChannelMember.builder()
                .channelId(channelId)
                .userId(userId)
                .displayName(displayName)
                .role(role)
                .orgAlias(orgAlias())
                .build();
        memberRepo.save(m);
    }

    @Transactional
    public void removeMember(UUID channelId, String userId) {
        memberRepo.deleteByChannelIdAndUserId(channelId, userId);
    }

    // ==================== Search ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchMessages(String query, UUID channelId, String currentUserId) {
        List<Message> results;
        if (channelId != null) {
            results = messageRepo.searchMessagesInChannel(orgAlias(), channelId, query, 50);
        } else {
            results = messageRepo.searchMessages(orgAlias(), query, 50);
        }
        return results.stream().map(m -> toMessageDto(m, currentUserId)).collect(Collectors.toList());
    }

    // ==================== Mark Read ====================

    @Transactional
    public void markChannelRead(UUID channelId, String userId) {
        memberRepo.findByChannelIdAndUserId(channelId, userId).ifPresent(m -> {
            m.setLastReadAt(Instant.now());
            memberRepo.save(m);
        });
    }

    @Transactional
    public void markChannelUnread(UUID channelId, String userId) {
        memberRepo.findByChannelIdAndUserId(channelId, userId).ifPresent(m -> {
            m.setLastReadAt(null);
            memberRepo.save(m);
        });
    }

    // ==================== Helpers ====================

    private void createSystemMessage(UUID channelId, String content, String systemType, String senderId) {
        Message msg = Message.builder()
                .channelId(channelId)
                .senderId(senderId)
                .senderName("System")
                .content(content)
                .system(true)
                .systemType(systemType)
                .orgAlias(orgAlias())
                .build();
        messageRepo.save(msg);
    }

    private Map<String, Object> toChannelDto(Channel c, String currentUserId) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", c.getId().toString());
        dto.put("name", c.getName());
        dto.put("type", c.getType());
        dto.put("topic", c.getTopic());
        dto.put("description", c.getDescription());
        dto.put("createdBy", c.getCreatedBy());
        dto.put("createdAt", c.getCreatedAt().toString());
        List<ChannelMember> members = memberRepo.findByChannelIdOrderByDisplayNameAsc(c.getId());
        dto.put("memberCount", members.size());

        // Compute unreadCount from lastReadAt
        long unreadCount = 0;
        if (currentUserId != null) {
            ChannelMember currentMember = members.stream()
                    .filter(m -> m.getUserId().equals(currentUserId)).findFirst().orElse(null);
            if (currentMember != null && currentMember.getLastReadAt() != null) {
                unreadCount = messageRepo.countMessagesAfter(c.getId(), currentMember.getLastReadAt());
            } else if (currentMember != null) {
                // Never marked read — all messages are unread
                unreadCount = messageRepo.countMessages(c.getId());
            }
        }
        dto.put("unreadCount", unreadCount);

        // Include last message for preview & sorting
        List<Message> recent = messageRepo.findRecentMessages(c.getId(), org.springframework.data.domain.PageRequest.of(0, 1));
        if (!recent.isEmpty()) {
            Message last = recent.get(0);
            Map<String, Object> lastMsg = new LinkedHashMap<>();
            lastMsg.put("id", last.getId().toString());
            lastMsg.put("senderId", last.getSenderId());
            lastMsg.put("senderName", last.getSenderName());
            lastMsg.put("content", last.getContent());
            lastMsg.put("createdAt", last.getCreatedAt().toString());
            lastMsg.put("isSystem", last.isSystem());
            dto.put("lastMessage", lastMsg);
        } else {
            dto.put("lastMessage", null);
        }

        // Include members array so frontends can identify participants (e.g., DM partner)
        dto.put("members", members.stream().map(this::toMemberDto).collect(Collectors.toList()));
        return dto;
    }

    private Map<String, Object> toMessageDto(Message m, String currentUserId) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", m.getId().toString());
        dto.put("channelId", m.getChannelId().toString());
        dto.put("senderId", m.getSenderId());
        dto.put("senderName", m.getSenderName());
        dto.put("content", m.getContent());
        dto.put("contentHtml", m.getContentHtml());
        dto.put("createdAt", m.getCreatedAt().toString());
        dto.put("updatedAt", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
        dto.put("parentId", m.getParentId() != null ? m.getParentId().toString() : null);
        dto.put("isPinned", m.isPinned());
        dto.put("isEdited", m.isEdited());
        dto.put("isDeleted", m.isDeleted());
        dto.put("isSystem", m.isSystem());
        dto.put("systemType", m.getSystemType());
        dto.put("notificationType", m.getNotificationType());
        dto.put("metadata", m.getMetadata());
        dto.put("mentions", m.getMentions());

        // Avatar from sender name
        String name = m.getSenderName();
        String initials = Arrays.stream(name.split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> String.valueOf(w.charAt(0)))
                .collect(Collectors.joining())
                .toUpperCase();
        if (initials.length() > 2) initials = initials.substring(0, 2);
        String[] colors = {
            "bg-gradient-to-br from-blue-500 to-blue-600",
            "bg-gradient-to-br from-pink-500 to-pink-600",
            "bg-gradient-to-br from-green-500 to-green-600",
            "bg-gradient-to-br from-purple-500 to-purple-600",
            "bg-gradient-to-br from-orange-500 to-orange-600",
            "bg-gradient-to-br from-indigo-500 to-indigo-600",
        };
        int colorIdx = Math.abs(m.getSenderId().hashCode()) % colors.length;
        dto.put("senderAvatar", Map.of("initials", initials, "color", colors[colorIdx]));

        // Thread reply count
        if (m.getParentId() == null) {
            long replyCount = messageRepo.countByParentIdAndDeletedFalse(m.getId());
            dto.put("threadReplyCount", replyCount);
        }

        // Reactions (aggregate)
        List<MessageReaction> reactions = reactionRepo.findByMessageId(m.getId());
        Map<String, List<String>> emojiUsers = new LinkedHashMap<>();
        for (MessageReaction r : reactions) {
            emojiUsers.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>()).add(r.getUserId());
        }
        List<Map<String, Object>> reactionDtos = emojiUsers.entrySet().stream().map(e -> {
            Map<String, Object> rd = new LinkedHashMap<>();
            rd.put("emoji", e.getKey());
            rd.put("count", e.getValue().size());
            rd.put("users", e.getValue());
            rd.put("hasReacted", currentUserId != null && e.getValue().contains(currentUserId));
            return rd;
        }).collect(Collectors.toList());
        dto.put("reactions", reactionDtos);

        // Attachments
        List<MessageAttachment> atts = attachmentRepo.findByMessageId(m.getId());
        dto.put("attachments", atts.stream().map(a -> {
            Map<String, Object> ad = new LinkedHashMap<>();
            ad.put("id", a.getId().toString());
            ad.put("fileName", a.getFileName());
            ad.put("fileUrl", a.getFileUrl());
            ad.put("fileType", a.getFileType());
            ad.put("fileSize", a.getFileSize());
            ad.put("thumbnailUrl", a.getThumbnailUrl());
            return ad;
        }).collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public Map<String, Object> uploadAttachment(UUID messageId, MultipartFile file) {
        String org = orgAlias();
        String storageKey = org + "/messaging/" + messageId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            byte[] bytes = file.getBytes();
            storageResolver.resolve(org).uploadByKey(bytes, storageKey, file.getContentType(), org,
                    "messaging", messageId.toString(), file.getOriginalFilename());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload attachment: " + e.getMessage(), e);
        }
        String fileUrl;
        try {
            fileUrl = storageResolver.resolve(org).getPresignedUrlByKey(storageKey, 3600 * 24);
        } catch (Exception e) {
            fileUrl = storageKey;
        }
        MessageAttachment att = MessageAttachment.builder()
                .messageId(messageId)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .orgAlias(org)
                .build();
        att = attachmentRepo.save(att);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", att.getId().toString());
        result.put("fileName", att.getFileName());
        result.put("fileUrl", att.getFileUrl());
        result.put("fileType", att.getFileType());
        result.put("fileSize", att.getFileSize());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAttachment(UUID messageId, UUID attachmentId) {
        return attachmentRepo.findById(attachmentId)
                .filter(a -> a.getMessageId().equals(messageId))
                .map(a -> {
                    String org = orgAlias();
                    String url = a.getFileUrl();
                    try {
                        url = storageResolver.resolve(org).getPresignedUrlByKey(
                                org + "/messaging/" + messageId + "/" + a.getFileName(), 3600);
                    } catch (Exception ignored) {}
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId().toString());
                    m.put("fileName", a.getFileName());
                    m.put("fileUrl", url);
                    m.put("fileType", a.getFileType());
                    m.put("fileSize", a.getFileSize());
                    return m;
                })
                .orElseThrow(() -> new NoSuchElementException("Attachment not found"));
    }

    private Map<String, Object> toMemberDto(ChannelMember m) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("userId", m.getUserId());
        dto.put("displayName", m.getDisplayName());
        dto.put("role", m.getRole());
        dto.put("joinedAt", m.getJoinedAt().toString());
        dto.put("lastReadAt", m.getLastReadAt() != null ? m.getLastReadAt().toString() : null);

        String initials = Arrays.stream(m.getDisplayName().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> String.valueOf(w.charAt(0)))
                .collect(Collectors.joining())
                .toUpperCase();
        if (initials.length() > 2) initials = initials.substring(0, 2);
        String[] colors = {
            "bg-gradient-to-br from-blue-500 to-blue-600",
            "bg-gradient-to-br from-pink-500 to-pink-600",
            "bg-gradient-to-br from-green-500 to-green-600",
            "bg-gradient-to-br from-purple-500 to-purple-600",
            "bg-gradient-to-br from-orange-500 to-orange-600",
        };
        int colorIdx = Math.abs(m.getUserId().hashCode()) % colors.length;
        dto.put("avatar", Map.of("initials", initials, "color", colors[colorIdx]));

        return dto;
    }
}
