package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    List<MessageAttachment> findAllByOrgIdAndMessageId(Long orgId, Long messageId);
    Optional<MessageAttachment> findByIdAndOrgId(Long id, Long orgId);
    List<MessageAttachment> findAllByMessageId(Long messageId);
}