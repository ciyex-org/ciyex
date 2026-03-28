package org.ciyex.ehr.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_installations")
public class AppInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "app_id", nullable = false)
    private UUID appId;

    @Column(name = "app_slug", nullable = false)
    private String appSlug;

    @Column(name = "app_name", nullable = false)
    private String appName;

    @Column(name = "app_icon_url")
    private String appIconUrl;

    @Column(name = "app_category")
    private String appCategory;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "active";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> config = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extension_points", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> extensionPoints = List.of();

    @Column(name = "smart_launch_url")
    private String smartLaunchUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "smart_redirect_uris", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> smartRedirectUris = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_scopes", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> fhirScopes = List.of();

    @Column(name = "keycloak_client_id")
    private String keycloakClientId;

    @Column(name = "cds_hooks_discovery_url")
    private String cdsHooksDiscoveryUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_hooks", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> supportedHooks = List.of();

    @Column(name = "installed_by")
    private String installedBy;

    @Column(name = "installed_at", nullable = false, updatable = false)
    private LocalDateTime installedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "uninstalled_at")
    private LocalDateTime uninstalledAt;

    @PrePersist
    void prePersist() {
        installedAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
