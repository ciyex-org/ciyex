//package com.qiaben.ciyex.entity;
//
//import com.qiaben.ciyex.dto.integration.RequestContext;
//import jakarta.persistence.PrePersist;
//import jakarta.persistence.PreUpdate;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * JPA Entity Listener that automatically populates the tenant name
// * from the RequestContext ThreadLocal before persisting or updating entities.
// */
//@Slf4j
//public class TenantAuditListener {
//
//    /**
//     * Called before an entity is persisted.
//     * Sets the tenant name from the current RequestContext.
//     */
//    @PrePersist
//    public void setTenantOnPersist(Object entity) {
//        if (entity instanceof AuditableEntity) {
//            AuditableEntity auditableEntity = (AuditableEntity) entity;
//
//            // Only set tenant name if it's not already set
//            if (auditableEntity.getTenantName() == null) {
//                String tenantName = getTenantNameFromContext();
//                if (tenantName != null) {
//                    auditableEntity.setTenantName(tenantName);
//                    log.debug("Set tenant name '{}' on entity {} before persist",
//                             tenantName, entity.getClass().getSimpleName());
//                } else {
//                    log.warn("No tenant name found in RequestContext for entity {} on persist",
//                            entity.getClass().getSimpleName());
//                }
//            }
//        }
//    }
//
//    /**
//     * Called before an entity is updated.
//     * Validates that the tenant name hasn't changed (it should be immutable).
//     */
//    @PreUpdate
//    public void validateTenantOnUpdate(Object entity) {
//        if (entity instanceof AuditableEntity) {
//            AuditableEntity auditableEntity = (AuditableEntity) entity;
//            String currentTenant = getTenantNameFromContext();
//            String entityTenant = auditableEntity.getTenantName();
//
//            if (currentTenant != null && entityTenant != null && !currentTenant.equals(entityTenant)) {
//                log.error("Tenant mismatch detected! Entity tenant: {}, Current context tenant: {}",
//                         entityTenant, currentTenant);
//                throw new IllegalStateException(
//                    String.format("Cannot update entity from different tenant. Entity tenant: %s, Current tenant: %s",
//                                entityTenant, currentTenant));
//            }
//        }
//    }
//
//    /**
//     * Retrieves the tenant name from the current RequestContext.
//     *
//     * @return the tenant name, or null if not available
//     */
//    private String getTenantNameFromContext() {
//        RequestContext context = RequestContext.get();
//        if (context != null) {
//            return context.getTenantName();
//        }
//        return null;
//    }
//}
