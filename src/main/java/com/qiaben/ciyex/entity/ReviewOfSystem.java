package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "review_of_systems")
public class ReviewOfSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long patientId;

    private Long orgId;



    private Long encounterId;

    private String systemName; // e.g. Cardiovascular, Musculoskeletal
    private boolean isNegative; // true if all negative, else false
    private String notes; // Additional notes for the system review

    @ElementCollection
    @CollectionTable(name = "system_review_details", joinColumns = @JoinColumn(name = "ros_id"))
    @Column(name = "details")
    private List<String> systemDetails; // List of symptoms or conditions in the review

    private String createdDate;
    private String lastModifiedDate;

    public void setIsNegative(boolean isNegative) {
        this.isNegative = isNegative;
    }
}
