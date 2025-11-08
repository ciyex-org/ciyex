package com.qiaben.ciyex.entity;

import jakarta.persistence.*;


/**
 * Entity for storing organization configuration as simple key-value pairs.
 * Each row represents one configuration key-value pair.
 */
@Entity
@Table(name = "org_config")
public class OrgConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration key (e.g., "storage_type", "fhir_api_url", "stripe_api_key")
     */
    @Column(name = "config_key", unique = true, nullable = false, length = 500)
    private String key;

    /**
     * Configuration value (e.g., "fhir", "https://api.example.com")
     * Can be null or empty
     */
    @Column(name = "config_value", nullable = true, columnDefinition = "TEXT")
    private String value;

    // Constructors

    public OrgConfig() {
    }

    public OrgConfig(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "OrgConfig{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
