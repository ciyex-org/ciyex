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
//@RequestMapping("/api/chief-complaints")  // The correct dynamic encounterId path
//public class ChiefComplaintController {
//
//    private final ChiefComplaintService chiefComplaintService;
//
//    public ChiefComplaintController(ChiefComplaintService chiefComplaintService) {
//        this.chiefComplaintService = chiefComplaintService;
//    }
//
//    @PostMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<ChiefComplaintDto>> create(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @RequestBody ChiefComplaintDto dto,
//            @RequestHeader("orgid") Long orgId
//    ) {
//        dto.setOrgId(orgId);  // Set orgId from headers
//        dto.setPatientId(patientId); // Set patientId
//        dto.setEncounterId(encounterId);  // Set encounterId
//        ChiefComplaintDto createdComplaint = chiefComplaintService.create(dto);
//        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
//                .success(true)
//                .message("Chief Complaint created successfully")
//                .data(createdComplaint)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{patientId}/{encounterId}")
//    public ResponseEntity<ApiResponse<List<ChiefComplaintDto>>> getAll(@PathVariable Long patientId, @PathVariable Long encounterId) {
//        List<ChiefComplaintDto> complaints = chiefComplaintService.getByPatientIdAndEncounterId(patientId, encounterId);
//        ApiResponse<List<ChiefComplaintDto>> response = new ApiResponse.Builder<List<ChiefComplaintDto>>()
//                .success(true)
//                .message("Chief Complaints fetched successfully")
//                .data(complaints)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ChiefComplaintDto>> getExactItem(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
//        ChiefComplaintDto complaint = chiefComplaintService.getById(patientId, encounterId, id);
//        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
//                .success(true)
//                .message("Chief Complaint fetched successfully")
//                .data(complaint)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<ChiefComplaintDto>> update(
//            @PathVariable Long patientId,
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            @RequestBody ChiefComplaintDto dto,
//            @RequestHeader("orgid") Long orgId
//    ) {
//        dto.setOrgId(orgId);  // Ensure the orgId is set in the DTO
//        ChiefComplaintDto updatedComplaint = chiefComplaintService.update(patientId, encounterId, id, dto);
//        ApiResponse<ChiefComplaintDto> response = new ApiResponse.Builder<ChiefComplaintDto>()
//                .success(true)
//                .message("Chief Complaint updated successfully")
//                .data(updatedComplaint)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{patientId}/{encounterId}/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id) {
//        chiefComplaintService.delete(patientId, encounterId, id);
//        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
//                .success(true)
//                .message("Chief Complaint deleted successfully")
//                .data(null)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//}
//
//



package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ChiefComplaintDto;
import com.qiaben.ciyex.service.ChiefComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chief-complaints")
@RequiredArgsConstructor
@Slf4j
public class ChiefComplaintController {

    private final ChiefComplaintService service;

    // LIST (used by ChiefComplaintList)  :contentReference[oaicite:1]{index=1}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<ChiefComplaintDto>>> list(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var items = service.list(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<ChiefComplaintDto>>builder()
                .success(true).message("Chief complaints fetched").data(items).build());
    }

    // GET ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ChiefComplaintDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        try {
            var dto = service.getOne(orgId, patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<ChiefComplaintDto>builder()
                    .success(true).message("Chief complaint fetched").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ChiefComplaintDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<ChiefComplaintDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody ChiefComplaintDto dto) {
        var saved = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<ChiefComplaintDto>builder()
                .success(true).message("Chief complaint created").data(saved).build());
    }

    // UPDATE (423 if signed)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<ChiefComplaintDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody ChiefComplaintDto dto) {
        try {
            var saved = service.update(orgId, patientId, encounterId, id, dto);
            return ResponseEntity.ok(ApiResponse.<ChiefComplaintDto>builder()
                    .success(true).message("Chief complaint updated").data(saved).build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<ChiefComplaintDto>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ChiefComplaintDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // DELETE (UI expects JSON, not 204)  :contentReference[oaicite:2]{index=2}
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        try {
            service.delete(orgId, patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Chief complaint deleted").build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(423)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // ESIGN (POST, no body; string-only)
    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ResponseEntity<ApiResponse<ChiefComplaintDto>> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            Principal principal) {
        try {
            String user = (principal != null) ? principal.getName() : "system";
            var dto = service.eSign(orgId, patientId, encounterId, id, user);
            return ResponseEntity.ok(ApiResponse.<ChiefComplaintDto>builder()
                    .success(true).message("Chief complaint e-signed").data(dto).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ChiefComplaintDto>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("eSign failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ChiefComplaintDto>builder().success(false).message(ex.getMessage()).build());
        }
    }

    // PRINT (PDF)
    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        byte[] pdf = service.renderPdf(orgId, patientId, encounterId, id);
        String filename = "chief-complaint-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
