//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "physical_exam_section")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class PhysicalExamSection {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    // e.g., GENERAL, HEENT, NECK, ...
//    @Column(name = "section_key", length = 48, nullable = false)
//    private String sectionKey;
//
//    @Column(name = "all_normal")
//    private Boolean allNormal;
//
//    @Column(name = "normal_text", length = 2000)
//    private String normalText;
//
//    @Column(name = "findings", length = 4000)
//    private String findings;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "physical_exam_id", nullable = false)
//    private PhysicalExam physicalExam;
//}

package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "physical_exam_section")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class PhysicalExamSection extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "all_normal")
    private Boolean allNormal;

    @Column(name = "findings", length = 4000)
    private String findings;

    @Column(name = "normal_text", length = 2000)
    private String normalText;

    @Column(name = "section_key", length = 48, nullable = false)
    private String sectionKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_exam_id", nullable = false)
    private PhysicalExam physicalExam;
}
