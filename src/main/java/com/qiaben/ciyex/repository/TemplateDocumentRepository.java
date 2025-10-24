package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.TemplateDocumentEntity;
import com.qiaben.ciyex.entity.TemplateContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateDocumentRepository extends JpaRepository<TemplateDocumentEntity, Long> {
    List<TemplateDocumentEntity> findByOrgId(Long orgId);
    List<TemplateDocumentEntity> findByOrgIdAndContext(Long orgId, TemplateContext context);
    List<TemplateDocumentEntity> findByOrgIdAndNameContainingIgnoreCase(Long orgId, String name);
    List<TemplateDocumentEntity> findByOrgIdAndContextAndNameContainingIgnoreCase(Long orgId, TemplateContext context, String name);
}