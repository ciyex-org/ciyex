package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.HealthcareServiceDto;

import java.util.List;

public interface ExternalHealthcareServiceStorage {

    HealthcareServiceDto createHealthcareService(HealthcareServiceDto dto);

    List<HealthcareServiceDto> getHealthcareServices();
}
