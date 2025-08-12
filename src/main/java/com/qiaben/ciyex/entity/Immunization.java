//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import lombok.Data;
//
//@Entity
//@Data
//public class Immunization {
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getVaccineName() {
//        return vaccineName;
//    }
//
//    public void setVaccineName(String vaccineName) {
//        this.vaccineName = vaccineName;
//    }
//
//    public String getDateAdministered() {
//        return dateAdministered;
//    }
//
//    public void setDateAdministered(String dateAdministered) {
//        this.dateAdministered = dateAdministered;
//    }
//
//    public Long getPatientId() {
//        return patientId;
//    }
//
//    public void setPatientId(Long patientId) {
//        this.patientId = patientId;
//    }
//
//    public String getAdministeredBy() {
//        return administeredBy;
//    }
//
//    public void setAdministeredBy(String administeredBy) {
//        this.administeredBy = administeredBy;
//    }
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String vaccineName;
//
//    private String dateAdministered;
//
//    private Long patientId;
//
//    private String administeredBy;
//
//}


package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Immunization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vaccineName;
    private String dateAdministered;
    private Long patientId;
    private String administeredBy;
   private Long orgId;
   // private Long immuid;
    private Long externaleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter; // Relationship to Encounter
}