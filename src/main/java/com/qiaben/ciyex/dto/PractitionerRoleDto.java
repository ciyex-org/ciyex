//package com.qiaben.ciyex.dto;
//
//import com.qiaben.ciyex.entity.Location;
//import com.qiaben.ciyex.entity.Org;
//import lombok.Data;
//
//@Data
//public class PractitionerRoleDto {
//
//    private Long id;
//    private String roleName;
//    private String specialty;
//
//    private Org org; // Organization entity
//    private Location location; // Location entity
//
//    private Long providerId;
//    private Long orgId;
//    private Long locationId;
//}

package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.entity.Org;
import lombok.Data;

@Data
public class PractitionerRoleDto {
    private Long id;
    private String role;
    private String specialty;
    private Long providerId;
    private Long orgId; // Organization ID for multi-tenancy
    private Long updatedAt; // Timestamp of the last update

    private Org organization;  // Organization DTO
    private Location location;  // Location DTO
}
