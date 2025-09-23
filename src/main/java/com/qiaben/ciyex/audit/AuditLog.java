package com.qiaben.ciyex.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false, length = 255)
    private String userId;

    @Column(nullable = false, length = 50)
    private String userRole;

    @Column(nullable = false, length = 50)
    private String actionType;

    @Column(length = 50)
    private String entityType;

    @Column(length = 255)
    private String entityId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String endpoint;

    public AuditLog() {
    }

    public AuditLog(String userId, String userRole, String actionType,
                    String entityType, String entityId, String description,
                    String details, String ipAddress, String endpoint) {
        this.userId = userId;
        this.userRole = userRole;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.details = details;
        this.ipAddress = ipAddress;
        this.endpoint = endpoint;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", eventTime=" + eventTime +
                ", userId='" + userId + '\'' +
                ", userRole='" + userRole + '\'' +
                ", actionType='" + actionType + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", description='" + description + '\'' +
                ", details='" + details + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", endpoint='" + endpoint + '\'' +
                '}';
    }
}
