package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import com.qiaben.ciyex.entity.PatientEducation;
import com.qiaben.ciyex.entity.PatientEducationAssignment;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.repository.PatientEducationAssignmentRepository;
import com.qiaben.ciyex.repository.PatientEducationRepository;
import com.qiaben.ciyex.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientEducationAssignmentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PatientEducationRepository educationRepository;
    private final PatientEducationAssignmentRepository assignmentRepository;
    private final PatientRepository patientRepository;

    private PatientEducationAssignmentDto toDto(PatientEducationAssignment a) {
        PatientEducationAssignmentDto dto = new PatientEducationAssignmentDto();
        dto.setId(a.getId());
        dto.setPatientId(a.getPatientId());
        dto.setNotes(a.getNotes());
        dto.setDelivered(a.isDelivered());
        dto.setPatientName(a.getPatientName());
        dto.setAssignedDate(
                a.getAssignedDate() != null
                        ? a.getAssignedDate().format(DateTimeFormatter.ISO_DATE_TIME)
                        : null
        );

        // Wrap education into topic
        PatientEducation edu = a.getEducation();
        if (edu != null) {
            PatientEducationAssignmentDto.TopicDto topic = new PatientEducationAssignmentDto.TopicDto();
            topic.setId(edu.getId());
            topic.setTitle(edu.getTitle());
            topic.setSummary(edu.getSummary());
            topic.setCategory(edu.getCategory());
            topic.setLanguage(edu.getLanguage());
            topic.setReadingLevel(edu.getReadingLevel());
            topic.setContent(edu.getContent());
            topic.setFhirId(edu.getExternalId());
            dto.setTopic(topic);
        }

        PatientEducationAssignmentDto.Audit audit = new PatientEducationAssignmentDto.Audit();
        if (a.getCreatedDate() != null) {
            audit.setCreatedDate(a.getCreatedDate().format(DATE_FORMATTER));
        }
        if (a.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(a.getLastModifiedDate().format(DATE_FORMATTER));
        }
        dto.setAudit(audit);

        return dto;
    }

    public PatientEducationAssignmentDto assign(Long educationId, PatientEducationAssignmentDto dto) {
        PatientEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new RuntimeException("Education not found"));

        // If fhirId is provided in the request, update the education entity
        if (dto.getFhirId() != null) {
            education.setExternalId(dto.getFhirId());
            educationRepository.save(education);
        }

        // Fetch patient to get their name
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        PatientEducationAssignment assignment = PatientEducationAssignment.builder()
                .education(education)
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName()) // populate patientName
                .notes(dto.getNotes())
                .delivered(true)
                .assignedDate(LocalDateTime.now())
                .build();

        return toDto(assignmentRepository.save(assignment));
    }

    public List<PatientEducationAssignmentDto> getByPatient(Long patientId) {
        return assignmentRepository.findByPatientId(patientId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        assignmentRepository.deleteById(id);
    }

    public PatientEducationAssignmentDto markDelivered(Long id) {
        PatientEducationAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        assignment.setDelivered(true);
        return toDto(assignmentRepository.save(assignment));
    }

    public long count() {
        return assignmentRepository.count();
    }

    public List<PatientEducationAssignmentDto> getAll() {
        return assignmentRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
