//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "social_history_entry")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class SocialHistoryEntry {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    // SMOKING | ALCOHOL | DRUGS | OCCUPATION | MARITAL_STATUS | EXERCISE | DIET | HOUSING | EDUCATION | SEXUAL_HISTORY | OTHER
//    @Column(name = "category", length = 48, nullable = false)
//    private String category;
//
//    @Column(name = "value", length = 255)
//    private String value;
//
//    @Column(name = "details", length = 2000)
//    private String details;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "social_history_id", nullable = false)
//    private SocialHistory socialHistory;
//}


package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "social_history_entry")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SocialHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", nullable = false, length = 48)
    private String category;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "value", length = 255)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_history_id", nullable = false)
    private SocialHistory socialHistory;
}

