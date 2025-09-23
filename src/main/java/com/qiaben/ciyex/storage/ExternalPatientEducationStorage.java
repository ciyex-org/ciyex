package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PatientEducationDto;
import java.util.List;

public interface ExternalPatientEducationStorage extends ExternalStorage<PatientEducationDto> {
    String createEducation(PatientEducationDto dto);
    void updateEducation(PatientEducationDto dto, String externalId);
    PatientEducationDto getEducation(String externalId);
    void deleteEducation(String externalId);
    List<PatientEducationDto> searchAllEducation();
}
