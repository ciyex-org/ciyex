package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ScheduleDto;
import com.qiaben.ciyex.service.ProviderScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ProviderScheduleController {

    private final ProviderScheduleService service;

    public ProviderScheduleController(ProviderScheduleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleDto>> create(@RequestBody ScheduleDto dto) {
        try {
            ScheduleDto result = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true).message("Schedule created successfully").data(result).build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<ScheduleDto>builder()
                    .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<ScheduleDto>builder()
                    .success(false).message("Failed to create schedule").data(null).build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDto>> get(@PathVariable Long id) {
        try {
            ScheduleDto dto = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true).message("Schedule retrieved successfully").data(dto).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<ScheduleDto>builder()
                    .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<ScheduleDto>builder()
                    .success(false).message("Failed to retrieve schedule").data(null).build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDto>> update(@PathVariable Long id, @RequestBody ScheduleDto dto) {
        try {
            ScheduleDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true).message("Schedule updated successfully").data(updated).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<ScheduleDto>builder()
                    .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<ScheduleDto>builder()
                    .success(false).message("Failed to update schedule").data(null).build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Schedule deleted successfully").data(null).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>builder()
                    .success(false).message(e.getMessage()).data(null).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete schedule").data(null).build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> list(
            @RequestParam(required = false) Long providerId
    ) {
        try {
            ApiResponse<List<ScheduleDto>> res = (providerId != null)
                    ? service.listByProvider(providerId)
                    : service.listAll();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<List<ScheduleDto>>builder()
                    .success(false).message("Failed to retrieve schedules").data(null).build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> count() {
        try {
            long cnt = service.countByOrg();
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true).message("Schedule count retrieved successfully").data(cnt).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Long>builder()
                    .success(false).message("Failed to retrieve schedule count").data(null).build());
        }
    }
}
