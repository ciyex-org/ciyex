// package com.qiaben.ciyex.controller.portal;

// import com.qiaben.ciyex.dto.*;
// import com.qiaben.ciyex.entity.portal.PortalUser;
// import com.qiaben.ciyex.repository.portal.PortalUserRepository;
// import com.qiaben.ciyex.service.*;
// import com.qiaben.ciyex.util.JwtTokenUtil;
// import com.qiaben.ciyex.dto.integration.RequestContext;
// import jakarta.servlet.http.HttpServletRequest;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api/portal")
// @RequiredArgsConstructor
// @Slf4j
// public class PortalHealthController {

//     private final JwtTokenUtil jwtTokenUtil;
//     private final PortalUserRepository portalUserRepository;

//     private final VitalsService vitalsService;
//     private final BillingService billingService;
//     private final MedicationService medicationService;
//     private final AllergyService allergyService;
//     private final HistoryService historyService;

//     // 🔹 Extract patientId from JWT or PortalUser
//     private Long extractPatientIdFromToken(HttpServletRequest request) {
//         String authHeader = request.getHeader("Authorization");
//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             throw new IllegalStateException("Missing or invalid Authorization header");
//         }
//         String token = authHeader.substring(7);
//         try {
//             Long userId = jwtTokenUtil.getUserIdFromToken(token);
//             if (userId != null) return userId;

//             String email = jwtTokenUtil.getEmailFromToken(token);
//             return portalUserRepository.findByEmail(email)
//                     .map(PortalUser::getId)
//                     .orElseThrow(() -> new IllegalStateException("No user found for email: " + email));
//         } catch (Exception e) {
//             log.error("❌ Token validation failed", e);
//             throw new IllegalStateException("Invalid or expired token");
//         }
//     }

//     private Long toLong(Object value) {
//         if (value == null) return null;
//         if (value instanceof Number) return ((Number) value).longValue();
//         return Long.valueOf(value.toString());
//     }

//     private void setRequestContextOrg(HttpServletRequest request) {
//         String token = request.getHeader("Authorization").substring(7);
//         List<?> orgIds = jwtTokenUtil.getOrgIdsFromToken(token);
//         if (orgIds == null || orgIds.isEmpty()) {
//             throw new IllegalStateException("No orgId found in patient token");
//         }
//         Long orgId = toLong(orgIds.get(0));
//         RequestContext ctx = new RequestContext();
//         ctx.setOrgId(orgId);
//         RequestContext.set(ctx);
//     }

//     // ------------------- VITALS -------------------
//     @PreAuthorize("hasRole('PATIENT')")
//     @GetMapping("/vitals")
//     public ResponseEntity<ApiResponse<List<VitalDto>>> getMyVitals(HttpServletRequest request) {
//         try {
//             Long patientId = extractPatientIdFromToken(request);
//             setRequestContextOrg(request);
//             return ResponseEntity.ok(ApiResponse.success("Vitals retrieved successfully", vitalsService.getByPatientId(patientId)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Failed to retrieve vitals"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PostMapping("/vitals")
//     public ResponseEntity<ApiResponse<VitalDto>> addVital(HttpServletRequest request, @RequestBody VitalDto dto) {
//         try {
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Vital added successfully", vitalsService.create(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not save vital"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PutMapping("/vitals/{id}")
//     public ResponseEntity<ApiResponse<VitalDto>> updateVital(HttpServletRequest request, @PathVariable Long id, @RequestBody VitalDto dto) {
//         try {
//             dto.setId(id);
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.ok(ApiResponse.success("Vital updated successfully", vitalsService.update(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not update vital"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @DeleteMapping("/vitals/{id}")
//     public ResponseEntity<ApiResponse<Void>> deleteVital(HttpServletRequest request, @PathVariable Long id) {
//         try {
//             vitalsService.delete(extractPatientIdFromToken(request), id);
//             return ResponseEntity.ok(ApiResponse.success("Vital deleted successfully", null));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not delete vital"));
//         }
//     }

//     // ------------------- BILLING -------------------
//     @PreAuthorize("hasRole('PATIENT')")
//     @GetMapping("/billing")
//     public ResponseEntity<ApiResponse<List<InvoiceDto>>> getMyInvoices(HttpServletRequest request) {
//         try {
//             return ResponseEntity.ok(ApiResponse.success("Invoices retrieved successfully", billingService.getByPatientId(extractPatientIdFromToken(request))));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Failed to retrieve invoices"));
//         }
//     }

//     // ------------------- MEDICATIONS -------------------
//     @PreAuthorize("hasRole('PATIENT')")
//     @GetMapping("/medications")
//     public ResponseEntity<ApiResponse<List<MedicationDto>>> getMyMedications(HttpServletRequest request) {
//         try {
//             return ResponseEntity.ok(ApiResponse.success("Medications retrieved successfully", medicationService.getByPatientId(extractPatientIdFromToken(request))));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Failed to retrieve medications"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PostMapping("/medications")
//     public ResponseEntity<ApiResponse<MedicationDto>> requestMedication(HttpServletRequest request, @RequestBody MedicationDto dto) {
//         try {
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Medication request submitted", medicationService.requestMedication(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not request medication"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PutMapping("/medications/{id}")
//     public ResponseEntity<ApiResponse<MedicationDto>> updateMedication(HttpServletRequest request, @PathVariable Long id, @RequestBody MedicationDto dto) {
//         try {
//             dto.setId(id);
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.ok(ApiResponse.success("Medication updated successfully", medicationService.update(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not update medication"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @DeleteMapping("/medications/{id}")
//     public ResponseEntity<ApiResponse<Void>> deleteMedication(HttpServletRequest request, @PathVariable Long id) {
//         try {
//             medicationService.delete(extractPatientIdFromToken(request), id);
//             return ResponseEntity.ok(ApiResponse.success("Medication deleted successfully", null));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not delete medication"));
//         }
//     }

//     // ------------------- ALLERGIES -------------------
//     @PreAuthorize("hasRole('PATIENT')")
//     @GetMapping("/allergies")
//     public ResponseEntity<ApiResponse<List<AllergyDto>>> getMyAllergies(HttpServletRequest request) {
//         try {
//             return ResponseEntity.ok(ApiResponse.success("Allergies retrieved successfully", allergyService.getByPatientId(extractPatientIdFromToken(request))));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Failed to retrieve allergies"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PostMapping("/allergies")
//     public ResponseEntity<ApiResponse<AllergyDto>> addAllergy(HttpServletRequest request, @RequestBody AllergyDto dto) {
//         try {
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Allergy added successfully", allergyService.create(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not add allergy"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PutMapping("/allergies/{id}")
//     public ResponseEntity<ApiResponse<AllergyDto>> updateAllergy(HttpServletRequest request, @PathVariable Long id, @RequestBody AllergyDto dto) {
//         try {
//             dto.setId(id);
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.ok(ApiResponse.success("Allergy updated successfully", allergyService.update(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not update allergy"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @DeleteMapping("/allergies/{id}")
//     public ResponseEntity<ApiResponse<Void>> deleteAllergy(HttpServletRequest request, @PathVariable Long id) {
//         try {
//             allergyService.delete(extractPatientIdFromToken(request), id);
//             return ResponseEntity.ok(ApiResponse.success("Allergy deleted successfully", null));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not delete allergy"));
//         }
//     }

//     // ------------------- HISTORY -------------------
//     @PreAuthorize("hasRole('PATIENT')")
//     @GetMapping("/history")
//     public ResponseEntity<ApiResponse<List<HistoryDto>>> getMyHistory(HttpServletRequest request) {
//         try {
//             return ResponseEntity.ok(ApiResponse.success("History retrieved successfully", historyService.getByPatientId(extractPatientIdFromToken(request))));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Failed to retrieve history"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PostMapping("/history")
//     public ResponseEntity<ApiResponse<HistoryDto>> addHistory(HttpServletRequest request, @RequestBody HistoryDto dto) {
//         try {
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("History added successfully", historyService.create(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not add history"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @PutMapping("/history/{id}")
//     public ResponseEntity<ApiResponse<HistoryDto>> updateHistory(HttpServletRequest request, @PathVariable Long id, @RequestBody HistoryDto dto) {
//         try {
//             dto.setId(id);
//             dto.setPatientId(extractPatientIdFromToken(request));
//             setRequestContextOrg(request);
//             return ResponseEntity.ok(ApiResponse.success("History updated successfully", historyService.update(dto)));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not update history"));
//         }
//     }

//     @PreAuthorize("hasRole('PATIENT')")
//     @DeleteMapping("/history/{id}")
//     public ResponseEntity<ApiResponse<Void>> deleteHistory(HttpServletRequest request, @PathVariable Long id) {
//         try {
//             historyService.delete(extractPatientIdFromToken(request), id);
//             return ResponseEntity.ok(ApiResponse.success("History deleted successfully", null));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Could not delete history"));
//         }
//     }
// }
//