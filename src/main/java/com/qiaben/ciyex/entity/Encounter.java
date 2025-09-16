
package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
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

    @Enumerated(EnumType.STRING)
    private EncounterStatus status = EncounterStatus.UNSIGNED; // default

    @Column(nullable = false, updatable = false)
    private Long createdAt;
    @Column(name = "encounter_date")
    private Instant encounterDate;   // or LocalDateTime if you prefer


    @Column(nullable = false)
    private Long updatedAt;

    @Column(nullable = false)
    private Long patientId;
//    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "encounter_id")
//    private List<Immunization> immunizations; // Nested immunizations
//@OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
//private List<Immunization> immunizations;

}





//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import java.time.Instant;
//
//@Entity
//@Table(name = "encounters")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class Encounter {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false)  // org scoping
//    private Long orgId;
//
//    @Column(nullable = false)
//    private Long patientId;
//
//    @Column(length = 50)
//    private String visitCategory;          // OPD / ER / IPD …
//
//    @Column(length = 200)
//    private String encounterProvider;      // physician name or id string
//
//    @Column(length = 100)
//    private String type;                   // Consultation / Follow-up / Telehealth …
//
//    @Column(length = 50)
//    private String sensitivity;            // Normal / Restricted
//
//    @Column(length = 200)
//    private String dischargeDisposition;   // Home, …
//
//    @Column(length = 2000)
//    private String reasonForVisit;
//
//    private Instant encounterDate;         // nullable if not set
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private EncounterStatus status = EncounterStatus.UNSIGNED;
//}
