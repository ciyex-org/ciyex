//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//
//@Entity
//public class PractitionerRole {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String roleName;
//    private String specialty;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
//    private Org organization;  // Relationship with Org
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "location_id", referencedColumnName = "id", nullable = false)
//    private Location location;  // Relationship with Location
//
//    private Long providerId;
//    private Long orgId;
//
//    // Getters and setters
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getRoleName() {
//        return roleName;
//    }
//
//    public void setRoleName(String roleName) {
//        this.roleName = roleName;
//    }
//
//    public String getSpecialty() {
//        return specialty;
//    }
//
//    public void setSpecialty(String specialty) {
//        this.specialty = specialty;
//    }
//
//    public Org getOrganization() {
//        return organization;
//    }
//
//    public void setOrganization(Org organization) {
//        this.organization = organization;
//    }
//
//    public Location getLocation() {
//        return location;
//    }
//
//    public void setLocation(Location location) {
//        this.location = location;
//    }
//
//    public Long getProviderId() {
//        return providerId;
//    }
//
//    public void setProviderId(Long providerId) {
//        this.providerId = providerId;
//    }
//
//    public Long getOrgId() {
//        return orgId;
//    }
//
//    public void setOrgId(Long orgId) {
//        this.orgId = orgId;
//    }
//}

package com.qiaben.ciyex.entity;

import jakarta.persistence.*;

@Entity
public class PractitionerRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String role;
    private String specialty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    private Org organization;  // Relationship with Org

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", referencedColumnName = "id", nullable = false)
    private Location location;  // Relationship with Location

    private Long providerId;
    private Long orgId; // Organization ID for multi-tenancy
    private Long updatedAt; // To track the last update timestamp

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public Org getOrganization() {
        return organization;
    }

    public void setOrganization(Org organization) {
        this.organization = organization;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

