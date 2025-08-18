package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ReviewOfSystemDto;

public interface ExternalReviewOfSystemStorage {

    // Interface methods for interacting with external systems (e.g., FHIR)
    ReviewOfSystemDto createExternalReviewOfSystem(ReviewOfSystemDto dto);
    ReviewOfSystemDto updateExternalReviewOfSystem(Long patientId, Long encounterId, Long rosId, ReviewOfSystemDto dto);
    void deleteExternalReviewOfSystem(Long patientId, Long encounterId, Long rosId);
}
