package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PractitionerRoleDto;
import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.PractitionerRole;
import com.qiaben.ciyex.repository.OrgRepository;
import com.qiaben.ciyex.repository.PractitionerRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PractitionerRoleService {

    @Autowired
    private PractitionerRoleRepository repository;
    @Autowired
    private OrgRepository orgRepository;

    // Method to create a PractitionerRole
    public PractitionerRoleDto createPractitionerRole(PractitionerRoleDto dto) {
        PractitionerRole role = new PractitionerRole();
        role.setRoleName(dto.getRoleName());
        role.setSpecialty(dto.getSpecialty());

        // Fetch the Org entity using the orgId from the DTO
        Org organization = orgRepository.findById(dto.getOrgId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid organization ID"));

        // Set the organization to the practitioner role
        role.setOrganization(organization);

        // Optionally, set other fields
        role.setLocation(dto.getLocation());
        role.setProviderId(dto.getProviderId());
        role.setOrgId(dto.getOrgId());

        // Save and return the DTO
        PractitionerRole savedRole = repository.save(role);
        return mapToDto(savedRole);
    }


    // Method to get all PractitionerRoles
    public List<PractitionerRoleDto> getAllPractitionerRoles() {
        List<PractitionerRole> roles = repository.findAll();
        return roles.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Method to map PractitionerRole to PractitionerRoleDto
    private PractitionerRoleDto mapToDto(PractitionerRole role) {
        PractitionerRoleDto dto = new PractitionerRoleDto();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setSpecialty(role.getSpecialty());
        dto.setOrg(role.getOrganization());
        dto.setLocation(role.getLocation());
        dto.setProviderId(role.getProviderId());
        dto.setOrgId(role.getOrgId());
       // dto.setLocationId(role.getLocationId());
        return dto;
    }
}
