package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.PhysicalExamDto;
import com.qiaben.ciyex.service.PhysicalExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/physical-exams")
public class PhysicalExamController {

    @Autowired
    private PhysicalExamService service;

    // Get all physical exams by patientId
    @GetMapping("/{patient_id}")
    public ResponseEntity<List<PhysicalExamDto>> getAllByPatientId(@PathVariable Long patient_id) {
        List<PhysicalExamDto> exams = service.getAllByPatientId(patient_id);
        return ResponseEntity.ok(exams);
    }

    // Get all physical exams by patientId and encounterId
    @GetMapping("/{patient_id}/{encounter_id}")
    public ResponseEntity<List<PhysicalExamDto>> getAllByPatientIdAndEncounterId(
            @PathVariable Long patient_id, @PathVariable Long encounter_id) {
        List<PhysicalExamDto> exams = service.getAllByPatientIdAndEncounterId(patient_id, encounter_id);
        return ResponseEntity.ok(exams);
    }

    // Get a specific physical exam by patientId, encounterId, and examId
    @GetMapping("/{patient_id}/{encounter_id}/{exam_id}")
    public ResponseEntity<PhysicalExamDto> getById(
            @PathVariable Long patient_id, @PathVariable Long encounter_id, @PathVariable Long exam_id) {
        PhysicalExamDto exam = service.getById(patient_id, encounter_id, exam_id);
        if (exam == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(exam);
    }

    // Create a new physical exam
    @PostMapping("/{patient_id}/{encounter_id}")
    public ResponseEntity<PhysicalExamDto> createPhysicalExam(
            @PathVariable Long patient_id,
            @PathVariable Long encounter_id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PhysicalExamDto dto) {

        // Set orgId in the DTO
        dto.setOrgId(orgId);
        dto.setPatientId(patient_id);
        dto.setEncounterId(encounter_id);

        // Save the exam and return the saved DTO
        PhysicalExamDto savedExam = service.save(dto);
        return ResponseEntity.ok(savedExam);
    }





    // Update a physical exam
    @PutMapping("/{patient_id}/{encounter_id}/{exam_id}")
    public ResponseEntity<PhysicalExamDto> update(
            @PathVariable Long patient_id, @PathVariable Long encounter_id, @PathVariable Long exam_id, @RequestBody PhysicalExamDto dto) {
        dto.setId(exam_id);
        PhysicalExamDto updatedExam = service.save(dto);
        return ResponseEntity.ok(updatedExam);
    }

    // Delete a physical exam
    @DeleteMapping("/{patient_id}/{encounter_id}/{exam_id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long patient_id, @PathVariable Long encounter_id, @PathVariable Long exam_id) {
        service.deleteById(patient_id, encounter_id, exam_id);
        return ResponseEntity.noContent().build();
    }
}
