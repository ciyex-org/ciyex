package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ChiefComplaintDto;
import com.qiaben.ciyex.entity.ChiefComplaint;
import com.qiaben.ciyex.repository.ChiefComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChiefComplaintService {

    private final ChiefComplaintRepository chiefComplaintRepository;

    public ChiefComplaintService(ChiefComplaintRepository chiefComplaintRepository) {
        this.chiefComplaintRepository = chiefComplaintRepository;
    }

    // Create Chief Complaint for a specific encounter
    @Transactional
    public ChiefComplaintDto create(ChiefComplaintDto dto) {
        ChiefComplaint chiefComplaint = new ChiefComplaint();
        chiefComplaint.setComplaint(dto.getComplaint());
        chiefComplaint.setDetails(dto.getDetails());
        chiefComplaint.setEncounterId(dto.getEncounterId());
        chiefComplaint.setOrgId(dto.getOrgId());  // Set orgId
        chiefComplaint.setPatientId(dto.getPatientId());
        chiefComplaint = chiefComplaintRepository.save(chiefComplaint);
        return mapToDto(chiefComplaint);
    }

    // Get all Chief Complaints for a specific encounter
    @Transactional(readOnly = true)
    public List<ChiefComplaintDto> getByEncounterId(Long encounterId) {
        List<ChiefComplaint> complaints = chiefComplaintRepository.findByEncounterId(encounterId);
        return complaints.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Update Chief Complaint
    @Transactional
    public ChiefComplaintDto update(Long encounterId, Long id, ChiefComplaintDto dto) {
        ChiefComplaint chiefComplaint = chiefComplaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));

        // Ensure the encounterId matches
        if (!chiefComplaint.getEncounterId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }

        // Set the updated data
        chiefComplaint.setComplaint(dto.getComplaint());
        chiefComplaint.setDetails(dto.getDetails());
        chiefComplaint.setOrgId(dto.getOrgId());  // Update orgId
        chiefComplaint.setPatientId(dto.getPatientId());

        // Save the updated chief complaint
        chiefComplaint = chiefComplaintRepository.save(chiefComplaint);

        // Return the updated DTO
        return mapToDto(chiefComplaint);
    }


    // Delete Chief Complaint
    @Transactional
    public void delete(Long encounterId, Long id) {
        ChiefComplaint chiefComplaint = chiefComplaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chief Complaint not found"));
        if (!chiefComplaint.getEncounterId().equals(encounterId)) {
            throw new RuntimeException("Encounter ID mismatch");
        }
        chiefComplaintRepository.delete(chiefComplaint);
    }

    private ChiefComplaintDto mapToDto(ChiefComplaint chiefComplaint) {
        ChiefComplaintDto dto = new ChiefComplaintDto();
        dto.setId(chiefComplaint.getId());
        dto.setComplaint(chiefComplaint.getComplaint());
        dto.setDetails(chiefComplaint.getDetails());
        dto.setEncounterId(chiefComplaint.getEncounterId());
        dto.setOrgId(chiefComplaint.getOrgId());
        dto.setCreatedAt(chiefComplaint.getCreatedAt());
        dto.setUpdatedAt(chiefComplaint.getUpdatedAt());
        dto.setPatientId(chiefComplaint.getPatientId());
        return dto;
    }
}
