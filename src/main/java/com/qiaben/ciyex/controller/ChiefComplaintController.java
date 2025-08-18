//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ChiefComplaintDto;
//import com.qiaben.ciyex.service.ChiefComplaintService;
//import com.qiaben.ciyex.dto.ApiResponse;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/{encounterId}/chief-complaints")  // The correct dynamic encounterId path
//public class ChiefComplaintController {
//
//    private final ChiefComplaintService chiefComplaintService;
//
//    public ChiefComplaintController(ChiefComplaintService chiefComplaintService) {
//        this.chiefComplaintService = chiefComplaintService;
//    }
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<ChiefComplaintDto>> create(
//            @PathVariable Long encounterId,
//            @RequestBody ChiefComplaintDto dto,
//            @RequestHeader("orgid") Long orgId
//    ) {
//        dto.setOrgId(orgId);  // Set orgId from headers
//        dto.setEncounterId(encounterId);  // Set encounterId from URL
//        ChiefComplaintDto createdComplaint = chiefComplaintService.create(dto);
//        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
//                .success(true)
//                .message("Chief Complaint created successfully")
//                .data(createdComplaint)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<ChiefComplaintDto>>> getAll(@PathVariable Long encounterId) {
//        List<ChiefComplaintDto> complaints = chiefComplaintService.getByEncounterId(encounterId);
//        ApiResponse<List<ChiefComplaintDto>> response = new ApiResponse.Builder<List<ChiefComplaintDto>>()
//                .success(true)
//                .message("Chief Complaints fetched successfully")
//                .data(complaints)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<ChiefComplaintDto>> update(
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            @RequestBody ChiefComplaintDto dto,
//            @RequestHeader("orgid") Long orgId  // Extract orgId from headers
//    ) {
//        // Set the orgId in the DTO before calling the service
//        dto.setOrgId(orgId);  // Ensure the orgId is set in the DTO
//
//        // Call the service to update the chief complaint
//        ChiefComplaintDto updatedComplaint = chiefComplaintService.update(encounterId, id, dto);
//
//        // Build and return the response
//        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
//                .success(true)
//                .message("Chief Complaint updated successfully")
//                .data(updatedComplaint)
//                .build();
//
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(
//            @PathVariable Long encounterId,
//            @PathVariable Long id
//    ) {
//        chiefComplaintService.delete(encounterId, id);
//        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
//                .success(true)
//                .message("Chief Complaint deleted successfully")
//                .data(null)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//}


package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ChiefComplaintDto;
import com.qiaben.ciyex.service.ChiefComplaintService;
import com.qiaben.ciyex.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chief-complaints")  // The correct dynamic encounterId path
public class ChiefComplaintController {

    private final ChiefComplaintService chiefComplaintService;

    public ChiefComplaintController(ChiefComplaintService chiefComplaintService) {
        this.chiefComplaintService = chiefComplaintService;
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ChiefComplaintDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody ChiefComplaintDto dto,
            @RequestHeader("orgid") Long orgId
    ) {
        dto.setOrgId(orgId);  // Set orgId from headers
        dto.setPatientId(patientId); // Set patientId
        dto.setEncounterId(encounterId);  // Set encounterId
        ChiefComplaintDto createdComplaint = chiefComplaintService.create(dto);
        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
                .success(true)
                .message("Chief Complaint created successfully")
                .data(createdComplaint)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ChiefComplaintDto>>> getAll(@PathVariable Long patientId, @PathVariable Long encounterId) {
        List<ChiefComplaintDto> complaints = chiefComplaintService.getByPatientIdAndEncounterId(patientId, encounterId);
        ApiResponse<List<ChiefComplaintDto>> response = new ApiResponse.Builder<List<ChiefComplaintDto>>()
                .success(true)
                .message("Chief Complaints fetched successfully")
                .data(complaints)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ChiefComplaintDto>> getExactItem(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        ChiefComplaintDto complaint = chiefComplaintService.getById(patientId, encounterId, id);
        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
                .success(true)
                .message("Chief Complaint fetched successfully")
                .data(complaint)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ChiefComplaintDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody ChiefComplaintDto dto,
            @RequestHeader("orgid") Long orgId
    ) {
        dto.setOrgId(orgId);  // Ensure the orgId is set in the DTO
        ChiefComplaintDto updatedComplaint = chiefComplaintService.update(patientId, encounterId, id, dto);
        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
                .success(true)
                .message("Chief Complaint updated successfully")
                .data(updatedComplaint)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
        chiefComplaintService.delete(patientId, encounterId, id);
        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                .success(true)
                .message("Chief Complaint deleted successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}

