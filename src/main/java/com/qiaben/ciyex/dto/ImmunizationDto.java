//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//import java.time.LocalDate;
//
//import lombok.Data;
//
//@Data
//public class ImmunizationDto {
//    private Long id;
//    private String vaccineName;
//    private String dateAdministered; // Date as String, or you can use LocalDate
//    private Long patientId;
//    private String administeredBy;
//}
package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ImmunizationDto {
    private Long id;
    private String vaccineName;
    private String dateAdministered;
    private Long patientId;
    private String administeredBy;
    private Long encounterId; // Used to set the encounter relationship
    private Long orgId;
    //private Long immuid;
    private Long externaleId;
}