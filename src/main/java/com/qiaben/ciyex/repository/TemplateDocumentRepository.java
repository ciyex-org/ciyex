package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.TemplateDocumentEntity;
import com.qiaben.ciyex.entity.TemplateContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateDocumentRepository extends JpaRepository<TemplateDocumentEntity, Long> {
    List<TemplateDocumentEntity> findAll();
    List<TemplateDocumentEntity> findByContext(TemplateContext context);
    List<TemplateDocumentEntity> findByNameContainingIgnoreCase(String name);
    List<TemplateDocumentEntity> findByContextAndNameContainingIgnoreCase(TemplateContext context, String name);
}
