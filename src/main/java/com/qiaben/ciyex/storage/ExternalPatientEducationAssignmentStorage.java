package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import java.util.List;

public interface ExternalPatientEducationAssignmentStorage extends ExternalStorage<PatientEducationAssignmentDto> {
    String createAssignment(PatientEducationAssignmentDto dto);
    void updateAssignment(PatientEducationAssignmentDto dto, String externalId);
    PatientEducationAssignmentDto getAssignment(String externalId);
    void deleteAssignment(String externalId);
    List<PatientEducationAssignmentDto> searchAllAssignments();
}
