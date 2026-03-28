package org.ciyex.ehr.fhir;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "fhir_resource_form_data",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resourceType", "resourceId", "orgAlias"}))
@Getter
@Setter
public class FhirFormDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String resourceType;

    @Column(nullable = false, length = 128)
    private String resourceId;

    @Column(nullable = false, length = 128)
    private String orgAlias;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String formData;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
