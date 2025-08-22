package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.service.CommunicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/communications")
@Slf4j
public class CommunicationController {

    private final CommunicationService service;

    public CommunicationController(CommunicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommunicationDto>> create(@RequestBody CommunicationDto dto) {
        try {
            CommunicationDto createdCommunication = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication created successfully")
                    .data(createdCommunication)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create communication: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to create communication: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunicationDto>> get(@PathVariable Long id) {
        try {
            CommunicationDto communication = service.getById(id);
            if (communication == null) {
                return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                        .success(false)
                        .message("Communication not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication retrieved successfully")
                    .data(communication)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve communication with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to retrieve communication: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunicationDto>> update(@PathVariable Long id, @RequestBody CommunicationDto dto) {
        try {
            CommunicationDto updatedCommunication = service.update(id, dto);
            if (updatedCommunication == null) {
                return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                        .success(false)
                        .message("Communication not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(true)
                    .message("Communication updated successfully")
                    .data(updatedCommunication)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update communication with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CommunicationDto>builder()
                    .success(false)
                    .message("Failed to update communication: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Communication deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete communication with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete communication: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CommunicationDto>>> getAllCommunications(
            @PageableDefault(sort = "sentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            ApiResponse<Page<CommunicationDto>> response = service.getAllCommunications(pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve all communications: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Page<CommunicationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve communications: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/patient")
    public ResponseEntity<ApiResponse<Page<CommunicationDto>>> getForPatient(
            @RequestBody CommunicationDto dto,
            @PageableDefault(sort = "sentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<CommunicationDto> page = service.getCommunicationsForPatient(dto.getPatientId(), pageable);
            return ResponseEntity.ok(ApiResponse.<Page<CommunicationDto>>builder()
                    .success(true)
                    .message("Patient communications retrieved successfully")
                    .data(page)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve patient communications: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Page<CommunicationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve patient communications: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/provider")
    public ResponseEntity<ApiResponse<Page<CommunicationDto>>> getForProvider(
            @RequestBody CommunicationDto dto,
            @PageableDefault(sort = "sentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<CommunicationDto> page = service.getCommunicationsForProvider(dto.getProviderId(), pageable);
            return ResponseEntity.ok(ApiResponse.<Page<CommunicationDto>>builder()
                    .success(true)
                    .message("Provider communications retrieved successfully")
                    .data(page)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve provider communications: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Page<CommunicationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve provider communications: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}/thread")
    public ResponseEntity<ApiResponse<List<CommunicationDto>>> getThread(@PathVariable Long id) {
        try {
            CommunicationDto comm = service.getById(id);
            List<CommunicationDto> thread = service.getThread(comm.getExternalId());
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(true)
                    .message("Thread retrieved successfully")
                    .data(thread)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve thread for communication id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<CommunicationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve thread: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCommunicationCount() {
        try {
            long count = service.countCommunicationsForCurrentOrg();
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message("Communication count retrieved successfully")
                    .data(count)
                    .build());
        } catch (Exception e) {
            log.error("Failed to count communications: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(false)
                    .message("Failed to count communications: " + e.getMessage())
                    .build());
        }
    }
}