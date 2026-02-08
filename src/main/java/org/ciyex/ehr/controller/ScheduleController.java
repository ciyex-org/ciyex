package org.ciyex.ehr.controller;


import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.ScheduleDto;
import org.ciyex.ehr.service.ScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController
@RequestMapping("/api/schedules")
@Slf4j
public class ScheduleController {


    private final ScheduleService service;


    public ScheduleController(ScheduleService service) {
        this.service = service;
    }


    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleDto>> create(@RequestBody ScheduleDto dto) {
        try {
            ScheduleDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true)
                    .message("Schedule created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create schedule: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(false)
                    .message("Failed to create schedule: " + e.getMessage())
                    .build());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDto>> get(@PathVariable String id) {
        try {
            ScheduleDto schedule = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true)
                    .message("Schedule retrieved successfully")
                    .data(schedule)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve schedule: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(false)
                    .message("Failed to retrieve schedule: " + e.getMessage())
                    .build());
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleDto>> update(@PathVariable String id, @RequestBody ScheduleDto dto) {
        try {
            ScheduleDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(true)
                    .message("Schedule updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update schedule: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<ScheduleDto>builder()
                    .success(false)
                    .message("Failed to update schedule: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Schedule deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete schedule: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete schedule: " + e.getMessage())
                    .build());
        }
    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> getAll() {
        try {
            ApiResponse<List<ScheduleDto>> response = service.getAllSchedules();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve schedules: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<ScheduleDto>>builder()
                    .success(false)
                    .message("Failed to retrieve schedules: " + e.getMessage())
                    .build());
        }
    }


}