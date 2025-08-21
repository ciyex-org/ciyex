//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.ChiefComplaintDto;
//import com.qiaben.ciyex.entity.ChiefComplaint;
//import com.qiaben.ciyex.repository.ChiefComplaintRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class ChiefComplaintService {
//
//    private final ChiefComplaintRepository chiefComplaintRepository;
//
//    public ChiefComplaintService(ChiefComplaintRepository chiefComplaintRepository) {
//        this.chiefComplaintRepository = chiefComplaintRepository;
//    }
//
//    // Create Chief Complaint for a specific encounter
//    @Transactional
//    public ChiefComplaintDto create(ChiefComplaintDto dto) {
//        ChiefComplaint chiefComplaint = new ChiefComplaint();
//        chiefComplaint.setComplaint(dto.getComplaint());
//        chiefComplaint.setDetails(dto.getDetails());
//        chiefComplaint.setEncounterId(dto.getEncounterId());
//        chiefComplaint.setOrgId(dto.getOrgId());  // Set orgId
//        chiefComplaint.setPatientId(dto.getPatientId());
//        chiefComplaint = chiefComplaintRepository.save(chiefComplaint);
//        return mapToDto(chiefComplaint);
//    }
//
//    // Get all Chief Complaints for a specific encounter
//    @Transactional(readOnly = true)
//    public List<ChiefComplaintDto> getByEncounterId(Long encounterId) {
//        List<ChiefComplaint> complaints = chiefComplaintRepository.findByEncounterId(encounterId);
//        return complaints.stream().map(this::mapToDto).collect(Collectors.toList());
//    }
//
//    // Update Chief Complaint
//    @Transactional
//    public ChiefComplaintDto update(Long encounterId, Long id, ChiefComplaintDto dto) {
//        ChiefComplaint chiefComplaint = chiefComplaintRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
//
//        // Ensure the encounterId matches
//        if (!chiefComplaint.getEncounterId().equals(encounterId)) {
//            throw new RuntimeException("Encounter ID mismatch");
//        }
//
//        // Set the updated data
//        chiefComplaint.setComplaint(dto.getComplaint());
//        chiefComplaint.setDetails(dto.getDetails());
//        chiefComplaint.setOrgId(dto.getOrgId());  // Update orgId
//        chiefComplaint.setPatientId(dto.getPatientId());
//
//        // Save the updated chief complaint
//        chiefComplaint = chiefComplaintRepository.save(chiefComplaint);
//
//        // Return the updated DTO
//        return mapToDto(chiefComplaint);
//    }
//
//
//    // Delete Chief Complaint
//    @Transactional
//    public void delete(Long encounterId, Long id) {
//        ChiefComplaint chiefComplaint = chiefComplaintRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
//        if (!chiefComplaint.getEncounterId().equals(encounterId)) {
//            throw new RuntimeException("Encounter ID mismatch");
//        }
//        chiefComplaintRepository.delete(chiefComplaint);
//    }
//
//    private ChiefComplaintDto mapToDto(ChiefComplaint chiefComplaint) {
//        ChiefComplaintDto dto = new ChiefComplaintDto();
//        dto.setId(chiefComplaint.getId());
//        dto.setComplaint(chiefComplaint.getComplaint());
//        dto.setDetails(chiefComplaint.getDetails());
//        dto.setEncounterId(chiefComplaint.getEncounterId());
//        dto.setOrgId(chiefComplaint.getOrgId());
//        dto.setCreatedAt(chiefComplaint.getCreatedAt());
//        dto.setUpdatedAt(chiefComplaint.getUpdatedAt());
//        dto.setPatientId(chiefComplaint.getPatientId());
//        return dto;
//    }
//}


package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ChiefComplaintDto;
import com.qiaben.ciyex.entity.ChiefComplaint;
import com.qiaben.ciyex.repository.ChiefComplaintRepository;
import com.qiaben.ciyex.storage.ExternalChiefComplaintStorage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChiefComplaintService {

    private final ChiefComplaintRepository chiefComplaintRepository;
    private final ExternalChiefComplaintStorage externalChiefComplaintStorage;

    public ChiefComplaintService(ChiefComplaintRepository chiefComplaintRepository, ExternalChiefComplaintStorage externalChiefComplaintStorage) {
        this.chiefComplaintRepository = chiefComplaintRepository;
        this.externalChiefComplaintStorage = externalChiefComplaintStorage;
    }

    // Create a new Chief Complaint
    public ChiefComplaintDto create(ChiefComplaintDto dto) {
        // Convert DTO to entity
        ChiefComplaint chiefComplaint = new ChiefComplaint();
        chiefComplaint.setComplaint(dto.getComplaint());
        chiefComplaint.setDetails(dto.getDetails());
        chiefComplaint.setEncounterId(dto.getEncounterId());
        chiefComplaint.setOrgId(dto.getOrgId());
        chiefComplaint.setPatientId(dto.getPatientId());
        chiefComplaint.setCreatedAt(dto.getCreatedAt());
        chiefComplaint.setUpdatedAt(dto.getUpdatedAt());

        // Save to database
        ChiefComplaint savedComplaint = chiefComplaintRepository.save(chiefComplaint);

        // Save to external storage (e.g., FHIR)
        externalChiefComplaintStorage.saveChiefComplaint(dto);

        // Return the DTO with the saved data
        dto.setId(savedComplaint.getId());
        return dto;
    }

    // Get all Chief Complaints for a patient by encounter
    public List<ChiefComplaintDto> getByPatientIdAndEncounterId(Long patientId, Long encounterId) {
        List<ChiefComplaint> complaints = chiefComplaintRepository.findByPatientId(patientId);
        return complaints.stream()
                .filter(complaint -> complaint.getEncounterId().equals(encounterId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Get a specific Chief Complaint by ID, patient ID, and encounter ID
    public ChiefComplaintDto getById(Long patientId, Long encounterId, Long id) {
        ChiefComplaint complaint = chiefComplaintRepository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));

        if (!complaint.getEncounterId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }

        return mapToDto(complaint);
    }

    // Update a Chief Complaint
    public ChiefComplaintDto update(Long patientId, Long encounterId, Long id, ChiefComplaintDto dto) {
        ChiefComplaint complaint = chiefComplaintRepository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));

        if (!complaint.getEncounterId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }

        // Update fields
        complaint.setComplaint(dto.getComplaint());
        complaint.setDetails(dto.getDetails());
        complaint.setUpdatedAt(dto.getUpdatedAt());

        // Save to database
        ChiefComplaint updatedComplaint = chiefComplaintRepository.save(complaint);

        // Save to external storage (e.g., FHIR)
        externalChiefComplaintStorage.saveChiefComplaint(dto);

        return mapToDto(updatedComplaint);
    }

    // Delete a Chief Complaint
    public void delete(Long patientId, Long encounterId, Long id) {
        ChiefComplaint complaint = chiefComplaintRepository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));

        if (!complaint.getEncounterId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }

        chiefComplaintRepository.delete(complaint);

        // Optionally delete from external storage (e.g., FHIR)
        externalChiefComplaintStorage.saveChiefComplaint(null);
    }

    // Helper method to map entity to DTO
    private ChiefComplaintDto mapToDto(ChiefComplaint complaint) {
        ChiefComplaintDto dto = new ChiefComplaintDto();
        dto.setId(complaint.getId());
        dto.setComplaint(complaint.getComplaint());
        dto.setDetails(complaint.getDetails());
        dto.setEncounterId(complaint.getEncounterId());
        dto.setOrgId(complaint.getOrgId());
        dto.setPatientId(complaint.getPatientId());
        dto.setCreatedAt(complaint.getCreatedAt());
        dto.setUpdatedAt(complaint.getUpdatedAt());
        return dto;
    }
}

