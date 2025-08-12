package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.HealthcareServiceDto;

public interface ExternalHealthcareServiceStorage {
    void saveHealthcareService(HealthcareServiceDto healthcareServiceDto);
    HealthcareServiceDto getHealthcareServiceById(Long id);
}
