//package com.qiaben.ciyex.entity;
//
//
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//
//@Entity
//@Data
//public class Encounter {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String visitCategory;  // Visit category (e.g., Outpatient, Inpatient)
//    private String encounterProvider; // Provider for the encounter
//    private String type;            // Type of encounter (e.g., Routine, Emergency)
//    private String sensitivity;     // Sensitivity (e.g., normal, urgent)
//    private String dischargeDisposition; // Discharge disposition (e.g., Admitted, Discharged)
//    private String reasonForVisit;  // Reason for the visit
//    private Boolean inCollection;
//    @Column(nullable = false, updatable = false)
//    private Long createdAt;
//
//    @Column(nullable = false)
//    private Long updatedAt;
//}
//
package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Encounter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String visitCategory;
    private String encounterProvider;
    private String type;
    private String sensitivity;
    private String dischargeDisposition;
    private String reasonForVisit;
    private Boolean inCollection;
    private Long orgId;
    @Column(nullable = false, updatable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long updatedAt;

//    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "encounter_id")
//    private List<Immunization> immunizations; // Nested immunizations
//@OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
//private List<Immunization> immunizations;

}




