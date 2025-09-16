//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "family_history_entry")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class FamilyHistoryEntry {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    // FATHER | MOTHER | SIBLING | SPOUSE | OFFSPRING
//    @Column(name = "relation", length = 24, nullable = false)
//    private String relation;
//
//    @Column(name = "diagnosis_code", length = 64)
//    private String diagnosisCode;
//
//    @Column(name = "diagnosis_text")
//    private String diagnosisText;
//
//    @Column(name = "notes", length = 1000)
//    private String notes;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "family_history_id", nullable = false)
//    private FamilyHistory familyHistory;
//}






package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "family_history_entry")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FamilyHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> family_history.id (on delete cascade handled by DB + orphanRemoval)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_history_id", nullable = false)
    private FamilyHistory familyHistory;

    @Column(name = "relation", length = 24, nullable = false)
    private String relation;

    @Column(name = "diagnosis_code", length = 64)
    private String diagnosisCode;

    @Column(name = "diagnosis_text", length = 255)
    private String diagnosisText;

    @Column(name = "notes", length = 1000)
    private String notes;
}
