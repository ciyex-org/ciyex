package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ReferralProviderDto;
import com.qiaben.ciyex.service.ReferralProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referral-providers")
public class ReferralProviderController {

    private final ReferralProviderService service;

    public ReferralProviderController(ReferralProviderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReferralProviderDto> create(@RequestBody ReferralProviderDto dto) {
        ReferralProviderDto createdDto = service.create(dto);
        return ResponseEntity.ok(createdDto);
    }

    @GetMapping("/{id}/with-practice")
    public ResponseEntity<ReferralProviderDto> getWithPractice(@PathVariable Long id) {
        ReferralProviderDto dto = service.getByIdWithPractice(id);
        return ResponseEntity.ok(dto);
    }

    // NEW: list providers by practiceId (each item includes practice {id, name})
    @GetMapping("/by-practice/{practiceId}")
    public ResponseEntity<List<ReferralProviderDto>> getByPractice(@PathVariable Long practiceId) {
        return ResponseEntity.ok(service.getByPracticeId(practiceId));
    }

    // NEW: return just the practice name for a given practiceId
    @GetMapping("/practice/{practiceId}/name")
    public ResponseEntity<PracticeNameResponse> getPracticeName(@PathVariable Long practiceId) {
        String name = service.getPracticeName(practiceId);
        return ResponseEntity.ok(new PracticeNameResponse(practiceId, name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReferralProviderDto> update(@PathVariable Long id, @RequestBody ReferralProviderDto dto) {
        ReferralProviderDto updatedDto = service.update(id, dto);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ReferralProviderDto>> getAll() {
        List<ReferralProviderDto> dtoList = service.getAll();
        return ResponseEntity.ok(dtoList);
    }

    // simple response wrapper
    public record PracticeNameResponse(Long id, String name) {}
}
