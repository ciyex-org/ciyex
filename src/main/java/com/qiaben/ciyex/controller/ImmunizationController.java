//package com.qiaben.ciyex.controller;
//
//
//
//import com.qiaben.ciyex.dto.ImmunizationDto;
//import com.qiaben.ciyex.service.ImmunizationService;
//import com.qiaben.ciyex.dto.ApiResponse;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/immunizations")
//public class ImmunizationController {
//
//    private final ImmunizationService immunizationService;
//
//    public ImmunizationController(ImmunizationService immunizationService) {
//        this.immunizationService = immunizationService;
//    }
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<ImmunizationDto>> create(@RequestBody ImmunizationDto dto) {
//        ImmunizationDto createdImmunization = immunizationService.create(dto);
//        ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
//                .success(true)
//                .message("Immunization created successfully")
//                .data(createdImmunization)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<ImmunizationDto>> getById(@PathVariable Long id) {
//        ImmunizationDto immunization = immunizationService.getById(id);
//        ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
//                .success(true)
//                .message("Immunization fetched successfully")
//                .data(immunization)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<ImmunizationDto>> update(@PathVariable Long id, @RequestBody ImmunizationDto dto) {
//        ImmunizationDto updatedImmunization = immunizationService.update(id, dto);
//        ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
//                .success(true)
//                .message("Immunization updated successfully")
//                .data(updatedImmunization)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
//        immunizationService.delete(id);
//        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
//                .success(true)
//                .message("Immunization deleted successfully")
//                .data(null)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//}


package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.service.ImmunizationService;
import com.qiaben.ciyex.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{encounterId}/immunizations")
public class ImmunizationController {
    private final ImmunizationService immunizationService;

    public ImmunizationController(ImmunizationService immunizationService) {
        this.immunizationService = immunizationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ImmunizationDto>> create(@PathVariable Long encounterId, @RequestBody ImmunizationDto dto) {
        ImmunizationDto createdImmunization = immunizationService.create(encounterId, dto);
        ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
                .success(true)
                .message("Immunization created successfully")
                .data(createdImmunization)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ImmunizationDto>>> getAll(@PathVariable Long encounterId) {
        List<ImmunizationDto> immunizations = immunizationService.getByEncounterId(encounterId);
        ApiResponse<List<ImmunizationDto>> response = new ApiResponse.Builder<List<ImmunizationDto>>()
                .success(true)
                .message("Immunizations fetched successfully")
                .data(immunizations)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ImmunizationDto>> update(@PathVariable Long encounterId, @PathVariable Long id, @RequestBody ImmunizationDto dto) {
        ImmunizationDto updatedImmunization = immunizationService.update(encounterId, id, dto);
        ApiResponse<ImmunizationDto> response = new ApiResponse.Builder<ImmunizationDto>()
                .success(true)
                .message("Immunization updated successfully")
                .data(updatedImmunization)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long encounterId, @PathVariable Long id) {
        immunizationService.delete(encounterId, id);
        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                .success(true)
                .message("Immunization deleted successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}




