//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.PractitionerRoleDto;
//import com.qiaben.ciyex.service.PractitionerRoleService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/practitionerRoles")
//public class PractitionerRoleController {
//
//    @Autowired
//    private PractitionerRoleService service;
//
//    // Endpoint to create a new PractitionerRole
//    @PostMapping
//    public PractitionerRoleDto createPractitionerRole(@RequestBody PractitionerRoleDto dto) {
//        return service.createPractitionerRole(dto);
//    }
//
//    // Endpoint to get all PractitionerRoles
//    @GetMapping
//    public List<PractitionerRoleDto> getAllPractitionerRoles() {
//        return service.getAllPractitionerRoles();
//    }
//}

package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PractitionerRoleDto;
import com.qiaben.ciyex.service.PractitionerRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practitioner-roles")
public class PractitionerRoleController {

    private final PractitionerRoleService practitionerRoleService;

    @Autowired
    public PractitionerRoleController(PractitionerRoleService practitionerRoleService) {
        this.practitionerRoleService = practitionerRoleService;
    }

    // Create PractitionerRole
    @PostMapping
    public ResponseEntity<ApiResponse<PractitionerRoleDto>> createPractitionerRole(@RequestBody PractitionerRoleDto practitionerRoleDto,
                                                                                   @RequestHeader(value = "orgId") Long orgId) {
        try {
            PractitionerRoleDto createdPractitionerRole = practitionerRoleService.createPractitionerRole(practitionerRoleDto, orgId);
            return ResponseEntity.ok(ApiResponse.<PractitionerRoleDto>builder()
                    .success(true)
                    .message("PractitionerRole created successfully")
                    .data(createdPractitionerRole)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<PractitionerRoleDto>builder()
                    .success(false)
                    .message("Failed to create PractitionerRole: " + e.getMessage())
                    .build());
        }
    }

    // Get PractitionerRole by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PractitionerRoleDto>> getPractitionerRoleById(@PathVariable Long id,
                                                                                    @RequestHeader(value = "orgId") Long orgId) {
        try {
            PractitionerRoleDto practitionerRole = practitionerRoleService.getPractitionerRoleById(id, orgId);
            return ResponseEntity.ok(ApiResponse.<PractitionerRoleDto>builder()
                    .success(true)
                    .message("PractitionerRole fetched successfully")
                    .data(practitionerRole)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ApiResponse.<PractitionerRoleDto>builder()
                    .success(false)
                    .message("PractitionerRole not found: " + e.getMessage())
                    .build());
        }
    }

    // Update PractitionerRole by ID
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PractitionerRoleDto>> updatePractitionerRole(@PathVariable Long id,
                                                                                   @RequestBody PractitionerRoleDto practitionerRoleDto,
                                                                                   @RequestHeader(value = "orgId") Long orgId) {
        try {
            PractitionerRoleDto updatedPractitionerRole = practitionerRoleService.updatePractitionerRole(id, practitionerRoleDto, orgId);
            return ResponseEntity.ok(ApiResponse.<PractitionerRoleDto>builder()
                    .success(true)
                    .message("PractitionerRole updated successfully")
                    .data(updatedPractitionerRole)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<PractitionerRoleDto>builder()
                    .success(false)
                    .message("Failed to update PractitionerRole: " + e.getMessage())
                    .build());
        }
    }
}
