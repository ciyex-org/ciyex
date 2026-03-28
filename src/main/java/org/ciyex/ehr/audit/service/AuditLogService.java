package org.ciyex.ehr.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.audit.dto.AuditLogDto;
import org.ciyex.ehr.audit.entity.AuditLog;
import org.ciyex.ehr.audit.repository.AuditLogRepository;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional
    public AuditLogDto log(AuditLogDto dto) {
        // Auto-populate userName / userId / userRole from JWT when caller did not provide them
        String resolvedUserName = dto.getUserName();
        String resolvedUserId   = dto.getUserId();
        String resolvedUserRole = dto.getUserRole();

        if (isBlank(resolvedUserName) || isBlank(resolvedUserId)) {
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth instanceof JwtAuthenticationToken jwtAuth) {
                    var jwt = jwtAuth.getToken();
                    if (isBlank(resolvedUserId)) {
                        resolvedUserId = firstNonBlank(
                                jwt.getClaimAsString("sub"),
                                jwt.getClaimAsString("userId"),
                                jwt.getClaimAsString("user_id"));
                    }
                    if (isBlank(resolvedUserName)) {
                        resolvedUserName = firstNonBlank(
                                jwt.getClaimAsString("preferred_username"),
                                jwt.getClaimAsString("name"),
                                jwt.getClaimAsString("email"),
                                resolvedUserId);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract user info from security context for audit log: {}", e.getMessage());
            }
        }
        if (isBlank(resolvedUserRole)) {
            resolvedUserRole = RequestContext.get().getUserRole();
        }

        var entry = AuditLog.builder()
                .action(dto.getAction())
                .resourceType(dto.getResourceType())
                .resourceId(dto.getResourceId())
                .resourceName(dto.getResourceName())
                .userId(resolvedUserId)
                .userName(resolvedUserName)
                .userRole(resolvedUserRole)
                .ipAddress(dto.getIpAddress())
                .details(dto.getDetails())
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .orgAlias(orgAlias())
                .build();
        entry = repo.save(entry);
        return toDto(entry);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> list(Pageable pageable) {
        return repo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getByUser(String userId, Pageable pageable) {
        return repo.findByOrgAliasAndUserIdOrderByCreatedAtDesc(orgAlias(), userId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getByPatient(Long patientId, Pageable pageable) {
        return repo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getByResourceType(String resourceType, Pageable pageable) {
        return repo.findByOrgAliasAndResourceTypeOrderByCreatedAtDesc(orgAlias(), resourceType, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getByAction(String action, Pageable pageable) {
        return repo.findByOrgAliasAndActionOrderByCreatedAtDesc(orgAlias(), action, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return list(pageable);
        }
        return repo.search(orgAlias(), query.trim(), pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        String org = orgAlias();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime last7d = now.minusDays(7);
        LocalDateTime last30d = now.minusDays(30);

        String[] actions = {"VIEW", "CREATE", "UPDATE", "DELETE", "SIGN", "PRINT", "EXPORT"};

        Map<String, Object> stats = new LinkedHashMap<>();

        // Counts by action for each time window
        Map<String, Long> last24hCounts = new LinkedHashMap<>();
        Map<String, Long> last7dCounts = new LinkedHashMap<>();
        Map<String, Long> last30dCounts = new LinkedHashMap<>();

        for (String action : actions) {
            last24hCounts.put(action, repo.countByOrgAliasAndActionAndCreatedAtAfter(org, action, last24h));
            last7dCounts.put(action, repo.countByOrgAliasAndActionAndCreatedAtAfter(org, action, last7d));
            last30dCounts.put(action, repo.countByOrgAliasAndActionAndCreatedAtAfter(org, action, last30d));
        }

        stats.put("last24h", last24hCounts);
        stats.put("last7d", last7dCounts);
        stats.put("last30d", last30dCounts);

        // Totals per window
        stats.put("total24h", repo.countByOrgAliasAndCreatedAtAfter(org, last24h));
        stats.put("total7d", repo.countByOrgAliasAndCreatedAtAfter(org, last7d));
        stats.put("total30d", repo.countByOrgAliasAndCreatedAtAfter(org, last30d));

        return stats;
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctResourceTypes() {
        return repo.findDistinctResourceTypesByOrgAlias(orgAlias());
    }

    private AuditLogDto toDto(AuditLog e) {
        return AuditLogDto.builder()
                .id(e.getId())
                .action(e.getAction())
                .resourceType(e.getResourceType())
                .resourceId(e.getResourceId())
                .resourceName(e.getResourceName())
                .userId(e.getUserId())
                .userName(e.getUserName())
                .userRole(e.getUserRole())
                .ipAddress(e.getIpAddress())
                .details(e.getDetails())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
