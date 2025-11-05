package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * LabResult entity captures the outcome of a laboratory test.
 * Dates are stored as String (YYYY-MM-DD or timestamp) to match existing schema conventions.
 */
@Entity
@Table(name = "lab_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Patient linkage
    @Column(name = "patient_id")
    private Long patientId;

    // Order / procedural metadata
    @Column(name = "order_date")
    private String orderDate;           // e.g. 2025-01-03

    @Column(name = "procedure_name")
    private String procedureName;       // Hemoglobin Test etc.

    @Column(name = "reported_date")
    private String reportedDate;        // Reported (result available) date

    @Column(name = "collected_date")
    private String collectedDate;       // External time collected

    // Specimen info
    @Column(name = "specimen")
    private String specimen;            // blood / urine etc.

    // Status (Final, Preliminary, Corrected, etc.)
    private String status;

    // Coding (LOINC / internal code)
    @Column(name = "code")
    private String code;

    @Column(name = "test_name")
    private String testName;            // Display name of result item

    @Column(name = "result_date")
    private String resultDate;          // Date associated with measurement

    @Column(name = "end_date")
    private String endDate;             // Optional end date (range tests / panels)

    @Column(name = "abnormal_flag")
    private String abnormalFlag;        // Low / High / Normal / Critical etc.

    @Column(name = "value")
    private String value;               // Numeric or textual value kept as string for flexibility

    @Column(name = "units")
    private String units;               // g/dL, mg/dL, etc.

    @Column(name = "reference_range")
    private String referenceRange;      // e.g. "12.1 - 16.0"

    @Column(name = "recommendations", length = 2048)
    private String recommendations;     // Clinical recommendations / interpretation

    // Audit fields (string date/time consistent with existing tables)
    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "last_modified_date")
    private String lastModifiedDate;

    // FHIR Observation id linkage
    @Column(name = "fhir_external_id")
    private String externalId;
}
