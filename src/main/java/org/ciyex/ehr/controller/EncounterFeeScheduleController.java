




package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.FeeScheduleDto;
import org.ciyex.ehr.service.EncounterFeeScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fee-schedules")
@RequiredArgsConstructor
@Slf4j
public class EncounterFeeScheduleController {

    private final EncounterFeeScheduleService service;

    // LIST by patient
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<FeeScheduleDto>>> listByPatient(
            @PathVariable Long patientId) {
        var list = service.listByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<FeeScheduleDto>>builder()
                .success(true).message("Fee schedules fetched").data(list).build());
    }

    // LIST in encounter
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<FeeScheduleDto>>> listInEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId) {
        var list = service.listInEncounter(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<FeeScheduleDto>>builder()
                .success(true).message("Fee schedules fetched").data(list).build());
    }

    // GET one
    @GetMapping("/{patientId}/{encounterId}/{scheduleId}")
    public ResponseEntity<ApiResponse<FeeScheduleDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId) {
        var dto = service.getOne(patientId, encounterId, scheduleId);
        return ResponseEntity.ok(ApiResponse.<FeeScheduleDto>builder()
                .success(true).message("Fee schedule fetched").data(dto).build());
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<FeeScheduleDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId,
            @RequestBody FeeScheduleDto dto) {
        var created = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<FeeScheduleDto>builder()
                .success(true).message("Fee schedule created").data(created).build());
    }

    // UPDATE
    @PutMapping("/{patientId}/{encounterId}/{scheduleId}")
    public ResponseEntity<ApiResponse<FeeScheduleDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId,
            @RequestBody FeeScheduleDto dto) {
        var updated = service.update(patientId, encounterId, scheduleId, dto);
        return ResponseEntity.ok(ApiResponse.<FeeScheduleDto>builder()
                .success(true).message("Fee schedule updated").data(updated).build());
    }

    // DELETE
    @DeleteMapping("/{patientId}/{encounterId}/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId) {
        service.delete(patientId, encounterId, scheduleId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Fee schedule deleted").build());
    }

    // ENTRIES
    @PostMapping("/{patientId}/{encounterId}/{scheduleId}/entries")
    public ResponseEntity<ApiResponse<FeeScheduleDto.FeeScheduleEntryDto>> addEntry(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId,
            @RequestBody FeeScheduleDto.FeeScheduleEntryDto dto) {
        var created = service.addEntry(patientId, encounterId, scheduleId, dto);
        return ResponseEntity.ok(ApiResponse.<FeeScheduleDto.FeeScheduleEntryDto>builder()
                .success(true).message("Fee schedule entry added").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{scheduleId}/entries/{entryId}")
    public ResponseEntity<ApiResponse<FeeScheduleDto.FeeScheduleEntryDto>> updateEntry(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId,
            @PathVariable Long entryId, @RequestBody FeeScheduleDto.FeeScheduleEntryDto dto) {
        var updated = service.updateEntry(patientId, encounterId, scheduleId, entryId, dto);
        return ResponseEntity.ok(ApiResponse.<FeeScheduleDto.FeeScheduleEntryDto>builder()
                .success(true).message("Fee schedule entry updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{scheduleId}/entries/{entryId}")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId,
            @PathVariable Long entryId) {
        service.deleteEntry(patientId, encounterId, scheduleId, entryId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Fee schedule entry deleted").build());
    }

    @GetMapping("/{patientId}/{encounterId}/{scheduleId}/entries")
    public ResponseEntity<ApiResponse<List<FeeScheduleDto.FeeScheduleEntryDto>>> listEntries(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId) {
        var list = service.listEntries(patientId, encounterId, scheduleId);
        return ResponseEntity.ok(ApiResponse.<List<FeeScheduleDto.FeeScheduleEntryDto>>builder()
                .success(true).message("Entries fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{scheduleId}/entries/search")
    public ResponseEntity<ApiResponse<List<FeeScheduleDto.FeeScheduleEntryDto>>> searchEntries(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable String scheduleId,
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "codeType", required = false) String codeType,
            @RequestParam(value = "active", required = false) Boolean active) {
        var list = service.searchEntries(patientId, encounterId, scheduleId, codeType, active, q);
        return ResponseEntity.ok(ApiResponse.<List<FeeScheduleDto.FeeScheduleEntryDto>>builder()
                .success(true).message("Entries search results").data(list).build());
    }
}
