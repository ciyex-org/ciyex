



package com.qiaben.ciyex.dto;

import java.time.LocalDateTime;

import com.qiaben.ciyex.entity.EncounterStatus;

import lombok.Data;

@Data
public class EncounterDto {

    private Long id;
    private Long patientId;
    private String visitCategory;
    private String encounterProvider;
    private String type;
    private String sensitivity;
    private String dischargeDisposition;
    private String reasonForVisit;
    private Long createdAt;
    private Long orgId;
    private Long updatedAt;
    private Boolean inCollection;
  private LocalDateTime encounterDate;

    private EncounterStatus status;


}

//package com.qiaben.ciyex.dto;
//
//import com.qiaben.ciyex.entity.EncounterStatus;
//import lombok.*;
//import java.time.Instant;
//
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class EncounterDto {
//    private Long id;
//    private Long orgId;
//    private Long patientId;
//    private String visitCategory;
//    private String encounterProvider;
//    private String type;
//    private String sensitivity;
//    private String dischargeDisposition;
//    private String reasonForVisit;
//    private Instant encounterDate;
//    private EncounterStatus status;
//}
