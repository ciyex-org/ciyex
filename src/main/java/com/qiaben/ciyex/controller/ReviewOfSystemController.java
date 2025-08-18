package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.service.ReviewOfSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviewofsystems")
public class ReviewOfSystemController {

    @Autowired
    private ReviewOfSystemService service;

    // GET all ROS entries for a specific patient and encounter
    @GetMapping("/{patient_id}/{encounter_id}")
    public ResponseEntity<List<ReviewOfSystemDto>> getAllByPatientIdAndEncounterId(@PathVariable Long patient_id, @PathVariable Long encounter_id) {
        List<ReviewOfSystemDto> dtos = service.getAllByPatientIdAndEncounterId(patient_id, encounter_id);
        return ResponseEntity.ok(dtos);
    }

    // GET a specific ROS entry
    @GetMapping("/{patient_id}/{encounter_id}/{ros_id}")
    public ResponseEntity<ReviewOfSystemDto> getById(@PathVariable Long patient_id,
                                                     @PathVariable Long encounter_id,
                                                     @PathVariable Long ros_id) {
        ReviewOfSystemDto dto = service.getById(patient_id, encounter_id, ros_id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    // POST to create a new ROS entry
    @PostMapping("/{patient_id}/{encounter_id}")
    public ResponseEntity<ReviewOfSystemDto> createReviewOfSystem(@PathVariable Long patient_id,
                                                                  @PathVariable Long encounter_id,
                                                                  @RequestBody ReviewOfSystemDto dto) {
        dto.setPatientId(patient_id);
        dto.setEncounterId(encounter_id);
        ReviewOfSystemDto createdDto = service.createReviewOfSystem(dto);
        return ResponseEntity.ok(createdDto);
    }

    // PUT to update an existing ROS entry
    @PutMapping("/{patient_id}/{encounter_id}/{ros_id}")
    public ResponseEntity<ReviewOfSystemDto> updateReviewOfSystem(@PathVariable Long patient_id,
                                                                  @PathVariable Long encounter_id,
                                                                  @PathVariable Long ros_id,
                                                                  @RequestBody ReviewOfSystemDto dto) {
        ReviewOfSystemDto updatedDto = service.updateReviewOfSystem(patient_id, encounter_id, ros_id, dto);
        return updatedDto != null ? ResponseEntity.ok(updatedDto) : ResponseEntity.notFound().build();
    }

    // DELETE to remove a ROS entry
    @DeleteMapping("/{patient_id}/{encounter_id}/{ros_id}")
    public ResponseEntity<Void> deleteReviewOfSystem(@PathVariable Long patient_id,
                                                     @PathVariable Long encounter_id,
                                                     @PathVariable Long ros_id) {
        service.deleteReviewOfSystem(patient_id, encounter_id, ros_id);
        return ResponseEntity.noContent().build();
    }
}
