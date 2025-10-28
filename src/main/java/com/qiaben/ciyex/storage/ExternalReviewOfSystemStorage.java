package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ReviewOfSystemDto;

import java.util.List;
import java.util.Optional;

public interface ExternalReviewOfSystemStorage {
    String create(ReviewOfSystemDto dto);
    void update(String externalId, ReviewOfSystemDto dto);
    Optional<ReviewOfSystemDto> get(String externalId);
    void delete(String externalId);

    List<ReviewOfSystemDto> searchAll(Long patientId);
    List<ReviewOfSystemDto> searchAll(Long patientId, Long encounterId);
}
