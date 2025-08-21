//package com.qiaben.ciyex.service;
//
//import com.qiaben.ciyex.dto.PractitionerRoleDto;
//import com.qiaben.ciyex.entity.Org;
//import com.qiaben.ciyex.entity.PractitionerRole;
//import com.qiaben.ciyex.repository.OrgRepository;
//import com.qiaben.ciyex.repository.PractitionerRoleRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class PractitionerRoleService {
//
//    @Autowired
//    private PractitionerRoleRepository repository;
//    @Autowired
//    private OrgRepository orgRepository;
//
//    // Method to create a PractitionerRole
//    public PractitionerRoleDto createPractitionerRole(PractitionerRoleDto dto) {
//        PractitionerRole role = new PractitionerRole();
//        role.setRoleName(dto.getRoleName());
//        role.setSpecialty(dto.getSpecialty());
//
//        // Fetch the Org entity using the orgId from the DTO
//        Org organization = orgRepository.findById(dto.getOrgId())
//                .orElseThrow(() -> new IllegalArgumentException("Invalid organization ID"));
//
//        // Set the organization to the practitioner role
//        role.setOrganization(organization);
//
//        // Optionally, set other fields
//        role.setLocation(dto.getLocation());
//        role.setProviderId(dto.getProviderId());
//        role.setOrgId(dto.getOrgId());
//
//        // Save and return the DTO
//        PractitionerRole savedRole = repository.save(role);
//        return mapToDto(savedRole);
//    }
//
//
//    // Method to get all PractitionerRoles
//    public List<PractitionerRoleDto> getAllPractitionerRoles() {
//        List<PractitionerRole> roles = repository.findAll();
//        return roles.stream().map(this::mapToDto).collect(Collectors.toList());
//    }
//
//    // Method to map PractitionerRole to PractitionerRoleDto
//    private PractitionerRoleDto mapToDto(PractitionerRole role) {
//        PractitionerRoleDto dto = new PractitionerRoleDto();
//        dto.setId(role.getId());
//        dto.setRoleName(role.getRoleName());
//        dto.setSpecialty(role.getSpecialty());
//        dto.setOrg(role.getOrganization());
//        dto.setLocation(role.getLocation());
//        dto.setProviderId(role.getProviderId());
//        dto.setOrgId(role.getOrgId());
//       // dto.setLocationId(role.getLocationId());
//        return dto;
//    }
//}

package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PractitionerRoleDto;
import com.qiaben.ciyex.entity.PractitionerRole;
import com.qiaben.ciyex.repository.PractitionerRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PractitionerRoleService {

    private final PractitionerRoleRepository practitionerRoleRepository;

    @Autowired
    public PractitionerRoleService(PractitionerRoleRepository practitionerRoleRepository) {
        this.practitionerRoleRepository = practitionerRoleRepository;
    }

    // Create PractitionerRole
    public PractitionerRoleDto createPractitionerRole(PractitionerRoleDto practitionerRoleDto, Long orgId) {
        practitionerRoleDto.setOrgId(orgId);  // Set orgId for multi-tenancy
        PractitionerRole practitionerRole = mapToEntity(practitionerRoleDto);
        practitionerRole.setUpdatedAt(System.currentTimeMillis());  // Set updatedAt timestamp
        practitionerRole = practitionerRoleRepository.save(practitionerRole);
        return mapToDto(practitionerRole);
    }

    // Get PractitionerRole by ID
    public PractitionerRoleDto getPractitionerRoleById(Long id, Long orgId) {
        PractitionerRole practitionerRole = practitionerRoleRepository.findById(id)
                .filter(role -> role.getOrgId().equals(orgId))  // Ensure orgId matches
                .orElseThrow(() -> new RuntimeException("PractitionerRole not found with id: " + id));
        return mapToDto(practitionerRole);
    }

    // Update PractitionerRole
    public PractitionerRoleDto updatePractitionerRole(Long id, PractitionerRoleDto practitionerRoleDto, Long orgId) {
        PractitionerRole practitionerRole = practitionerRoleRepository.findById(id)
                .filter(role -> role.getOrgId().equals(orgId))  // Ensure orgId matches
                .orElseThrow(() -> new RuntimeException("PractitionerRole not found with id: " + id));

        // Update the fields based on DTO
        practitionerRole.setRole(practitionerRoleDto.getRole());
        practitionerRole.setSpecialty(practitionerRoleDto.getSpecialty());
        practitionerRole.setProviderId(practitionerRoleDto.getProviderId());
        practitionerRole.setUpdatedAt(System.currentTimeMillis());  // Set the updated timestamp
        practitionerRole = practitionerRoleRepository.save(practitionerRole);
        return mapToDto(practitionerRole);
    }

    // Map Entity to DTO
    private PractitionerRole mapToEntity(PractitionerRoleDto dto) {
        PractitionerRole practitionerRole = new PractitionerRole();
        practitionerRole.setRole(dto.getRole());
        practitionerRole.setSpecialty(dto.getSpecialty());
        practitionerRole.setProviderId(dto.getProviderId());
        practitionerRole.setOrgId(dto.getOrgId());  // Set orgId for multi-tenancy
        return practitionerRole;
    }

    // Map DTO to Entity
    private PractitionerRoleDto mapToDto(PractitionerRole practitionerRole) {
        PractitionerRoleDto dto = new PractitionerRoleDto();
        dto.setId(practitionerRole.getId());
        dto.setRole(practitionerRole.getRole());
        dto.setSpecialty(practitionerRole.getSpecialty());
        dto.setProviderId(practitionerRole.getProviderId());
        dto.setOrgId(practitionerRole.getOrgId());  // Include orgId in the DTO
        dto.setUpdatedAt(practitionerRole.getUpdatedAt());  // Include updatedAt in the DTO
        return dto;
    }
}
