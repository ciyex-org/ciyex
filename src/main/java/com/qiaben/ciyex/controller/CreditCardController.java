package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CreditCardDto;
import com.qiaben.ciyex.service.CreditCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-cards")
@RequiredArgsConstructor
@Slf4j
public class CreditCardController {

    private final CreditCardService service;

    /**
     * Create a new credit card
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreditCardDto dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Validation failed")
                            .data(errors)
                            .build());
        }

        try {
            CreditCardDto created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(true)
                            .message("Credit card created successfully")
                            .data(created)
                            .build());
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating credit card", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to create credit card", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message("Failed to create credit card: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get a credit card by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditCardDto>> getById(@PathVariable Long id) {
        try {
            CreditCardDto card = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<CreditCardDto>builder()
                    .success(true)
                    .message("Credit card retrieved successfully")
                    .data(card)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Credit card not found with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to retrieve credit card with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message("Failed to retrieve credit card: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get all credit cards for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<CreditCardDto>>> getByPatientId(@PathVariable Long patientId) {
        try {
            List<CreditCardDto> cards = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<List<CreditCardDto>>builder()
                    .success(true)
                    .message("Credit cards retrieved successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve credit cards for patient ID: {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<CreditCardDto>>builder()
                            .success(false)
                            .message("Failed to retrieve credit cards: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get active credit cards for a patient
     */
    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<ApiResponse<List<CreditCardDto>>> getActiveCards(@PathVariable Long patientId) {
        try {
            List<CreditCardDto> cards = service.getActiveCardsByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<List<CreditCardDto>>builder()
                    .success(true)
                    .message("Active credit cards retrieved successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve active credit cards for patient ID: {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<CreditCardDto>>builder()
                            .success(false)
                            .message("Failed to retrieve active credit cards: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get default credit card for a patient
     */
    @GetMapping("/patient/{patientId}/default")
    public ResponseEntity<ApiResponse<CreditCardDto>> getDefaultCard(@PathVariable Long patientId) {
        try {
            CreditCardDto card = service.getDefaultCard(patientId);
            return ResponseEntity.ok(ApiResponse.<CreditCardDto>builder()
                    .success(true)
                    .message("Default credit card retrieved successfully")
                    .data(card)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Default card not found for patient ID: {}", patientId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to retrieve default credit card for patient ID: {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message("Failed to retrieve default credit card: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Update a credit card
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditCardDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreditCardDto dto,
            BindingResult result) {

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message("Validation failed")
                            .build());
        }

        try {
            CreditCardDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<CreditCardDto>builder()
                    .success(true)
                    .message("Credit card updated successfully")
                    .data(updated)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating credit card with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to update credit card with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message("Failed to update credit card: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Set a card as default
     */
    @PutMapping("/{id}/patient/{patientId}/set-default")
    public ResponseEntity<ApiResponse<CreditCardDto>> setAsDefault(
            @PathVariable Long id,
            @PathVariable Long patientId) {

        try {
            CreditCardDto updated = service.setAsDefault(id, patientId);
            return ResponseEntity.ok(ApiResponse.<CreditCardDto>builder()
                    .success(true)
                    .message("Credit card set as default successfully")
                    .data(updated)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Error setting credit card as default", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to set credit card as default", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<CreditCardDto>builder()
                            .success(false)
                            .message("Failed to set credit card as default: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Deactivate a credit card
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        try {
            service.deactivate(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Credit card deactivated successfully")
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Credit card not found with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to deactivate credit card with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to deactivate credit card: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Delete a credit card
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Credit card deleted successfully")
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Credit card not found with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Failed to delete credit card with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete credit card: " + e.getMessage())
                            .build());
        }
    }
}
