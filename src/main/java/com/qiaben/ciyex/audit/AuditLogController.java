package com.qiaben.ciyex.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<AuditLog> logs;

        if (userId != null && ipAddress != null) {
            logs = auditLogRepository.findByUserIdAndIpAddressOrderByEventTimeDesc(userId, ipAddress);
        } else if (userId != null) {
            logs = auditLogRepository.findByUserIdOrderByEventTimeDesc(userId);
        } else if (actionType != null) {
            logs = auditLogRepository.findByActionTypeOrderByEventTimeDesc(actionType);
        } else if (entityType != null) {
            logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimeDesc(entityType, null);
        } else if (userRole != null) {
            logs = auditLogRepository.findByUserRoleOrderByEventTimeDesc(userRole);
        } else if (ipAddress != null && startDate != null && endDate != null) {
            logs = auditLogRepository.findByIpAddressAndDateRange(ipAddress, startDate, endDate);
        } else if (ipAddress != null) {
            logs = auditLogRepository.findByIpAddressOrderByEventTimeDesc(ipAddress);
        } else if (endpoint != null) {
            logs = auditLogRepository.findByEndpointContainingOrderByEventTimeDesc(endpoint);
        } else if (startDate != null && endDate != null) {
            logs = auditLogRepository.findByDateRange(startDate, endDate);
        } else {
            logs = auditLogRepository.findAll();
        }

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable String userId) {
        List<AuditLog> logs = auditLogRepository.findByUserIdOrderByEventTimeDesc(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/action/{actionType}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAction(@PathVariable String actionType) {
        List<AuditLog> logs = auditLogRepository.findByActionTypeOrderByEventTimeDesc(actionType);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable(required = false) String entityId) {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByEventTimeDesc(entityType, entityId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByIpAddress(@PathVariable String ipAddress) {
        List<AuditLog> logs = auditLogRepository.findByIpAddressOrderByEventTimeDesc(ipAddress);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/endpoint/{endpoint}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEndpoint(@PathVariable String endpoint) {
        List<AuditLog> logs = auditLogRepository.findByEndpointContainingOrderByEventTimeDesc(endpoint);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}/ip/{ipAddress}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUserAndIp(
            @PathVariable String userId,
            @PathVariable String ipAddress) {
        List<AuditLog> logs = auditLogRepository.findByUserIdAndIpAddressOrderByEventTimeDesc(userId, ipAddress);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<AuditLog> logs = auditLogRepository.findByDateRange(startDate, endDate);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/ip/{ipAddress}/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByIpAddressAndDateRange(
            @PathVariable String ipAddress,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<AuditLog> logs = auditLogRepository.findByIpAddressAndDateRange(ipAddress, startDate, endDate);
        return ResponseEntity.ok(logs);
    }
}