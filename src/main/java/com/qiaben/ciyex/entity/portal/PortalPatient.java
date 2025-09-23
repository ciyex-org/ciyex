package com.qiaben.ciyex.entity.portal;

import java.time.LocalDate;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portal_patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalPatient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private PortalUser user;

    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String insuranceId;
    public void setInsuranceId(Long insuranceId2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInsuranceId'");
    }
}
