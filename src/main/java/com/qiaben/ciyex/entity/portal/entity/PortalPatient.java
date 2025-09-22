package com.qiaben.ciyex.entity.portal.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portal_patient")
@Data                       // generates getters/setters/toString/hashCode/equals
@NoArgsConstructor          // no-args constructor
@AllArgsConstructor         // all-args constructor
@Builder                    // enables PortalPatient.builder()
public class PortalPatient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private PortalUser user;   // ✅ make sure PortalUser is also in com.qiaben.ciyex.entity.portal

    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String email;
    private String address;

    @Column(name = "insurance_id")
    private Long insuranceId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
