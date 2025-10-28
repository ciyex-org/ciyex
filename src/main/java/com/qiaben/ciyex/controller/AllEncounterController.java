// package com.qiaben.ciyex.controller;

// import com.qiaben.ciyex.dto.ApiResponse;
// import com.qiaben.ciyex.dto.EncounterDto;
// import com.qiaben.ciyex.entity.EncounterStatus;
// import com.qiaben.ciyex.service.EncounterService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.domain.*;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;


// @RestController
// @RequestMapping("/api/encounters")
// public class AllEncounterController {

//     private final EncounterService encounterService;

//     @Autowired
//     public AllEncounterController(EncounterService encounterService) {
//         this.encounterService = encounterService;
//     }

//     /**
//      * GET /api/encounters
//      * Example:
//      *   /api/encounters?status=UNSIGNED&patientId=123&provider=Dr.%20Smith&from=2025-09-01&to=2025-09-30&page=0&size=20&sort=id,desc
//      *
//      * Notes:
//      * - status binds to EncounterStatus enum (SIGNED | UNSIGNED | INCOMPLETE).
//      * - provider is a String matched against Encounter.encounterProvider (exact match).
//      * - from/to accept yyyy-MM-dd (date-only) or full ISO-8601 instant.
//      * - sort=<field>,<direction>, defaults to id,desc.
//      */
//     @GetMapping
//     public ResponseEntity<ApiResponse<Page<EncounterDto>>> listAll(
//             //             @RequestParam(value = "status", required = false) EncounterStatus status,
//             @RequestParam(value = "patientId", required = false) Long patientId,
//             @RequestParam(value = "provider", required = false) String provider,
//             @RequestParam(value = "from", required = false) String from,
//             @RequestParam(value = "to", required = false) String to,
//             @RequestParam(value = "page", defaultValue = "0") int page,
//             @RequestParam(value = "size", defaultValue = "20") int size,
//             @RequestParam(value = "sort", defaultValue = "id,desc") String sort
//     ) {
//         try {
//             Pageable pageable = toPageable(page, size, sort);
//             Page<EncounterDto> result = encounterService.listAll(
//                     orgId, status, patientId, provider, from, to, pageable
//             );
//             return ResponseEntity.ok(ApiResponse.<Page<EncounterDto>>builder()
//                     .success(true)
//                     .message("Encounters fetched")
//                     .data(result)
//                     .build());
//         } catch (Exception e) {
//             return ResponseEntity.status(500).body(ApiResponse.<Page<EncounterDto>>builder()
//                     .success(false)
//                     .message("Failed to list encounters: " + e.getMessage())
//                     .build());
//         }
//     }

//     private Pageable toPageable(int page, int size, String sort) {
//         try {
//             String[] parts = sort.split(",");
//             if (parts.length == 2) {
//                 return PageRequest.of(page, size,
//                         Sort.by(Sort.Direction.fromString(parts[1]), parts[0]));
//             }
//         } catch (Exception ignored) {}
//         return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
//     }
// }
