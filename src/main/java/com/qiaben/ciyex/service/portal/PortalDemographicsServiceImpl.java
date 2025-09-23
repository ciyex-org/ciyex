package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.PortalDemographicsDto;
import com.qiaben.ciyex.entity.portal.PortalDemographics;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalDemographicsRepository;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalDemographicsServiceImpl implements PortalDemographicsService {

    private final PortalDemographicsRepository demographicsRepository;
    private final PortalPatientRepository patientRepository;
    private final PortalUserRepository userRepository;

    /**
     * Fetch demographics for the logged-in user (via userId from JWT).
     * Auto-creates patient record if missing.
     */
    @Override
    @Transactional(readOnly = true)
    public PortalDemographicsDto getMyDemographics(Long userId) {
        // Ensure patient exists for this user
        PortalPatient patient = patientRepository.findByUser_Id(userId)
                .orElseGet(() -> autoCreatePatient(userId));

        // Fetch demographics or return blank if none exists
        PortalDemographics demographics = demographicsRepository.findByPatient_User_Id(userId)
                .orElseGet(() -> {
                    PortalDemographics blank = new PortalDemographics();
                    blank.setPatient(patient);
                    return blank;
                });

        return mapToDto(demographics);
    }

    /**
     * Update demographics for the logged-in user.
     * Auto-creates demographics if missing.
     */
    @Override
    @Transactional
    public PortalDemographicsDto updateMyDemographics(Long userId, PortalDemographicsDto dto) {
        // Ensure patient exists for this user
        PortalPatient patient = patientRepository.findByUser_Id(userId)
                .orElseGet(() -> autoCreatePatient(userId));

        // Load existing or create new demographics
        PortalDemographics demographics = demographicsRepository.findByPatient_User_Id(userId)
                .orElseGet(() -> {
                    PortalDemographics newDemo = new PortalDemographics();
                    newDemo.setPatient(patient);
                    return newDemo;
                });

        // Map incoming data
        mapToEntity(dto, demographics);
        demographicsRepository.save(demographics);

        return mapToDto(demographics);
    }

    // === Helper to auto-create a patient profile if missing ===
    private PortalPatient autoCreatePatient(Long userId) {
        PortalUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        PortalPatient newPatient = PortalPatient.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .dob(user.getDateOfBirth())
                .address(user.getStreet())
                .build();

        return patientRepository.save(newPatient);
    }

    // === Mapping helpers ===
    private PortalDemographicsDto mapToDto(PortalDemographics e) {
        PortalDemographicsDto dto = new PortalDemographicsDto();
        dto.setId(e.getId());
        dto.setFirstName(e.getFirstName());
        dto.setMiddleName(e.getMiddleName());
        dto.setLastName(e.getLastName());
        dto.setDob(e.getDob());
        dto.setSex(e.getSex());
        dto.setMaritalStatus(e.getMaritalStatus());
        dto.setAddress(e.getAddress());
        dto.setCity(e.getCity());
        dto.setState(e.getState());
        dto.setPostalCode(e.getPostalCode());
        dto.setCountry(e.getCountry());
        dto.setPhoneMobile(e.getPhoneMobile());
        dto.setContactEmail(e.getContactEmail());
        dto.setEmergencyContactName(e.getEmergencyContactName());
        dto.setEmergencyContactPhone(e.getEmergencyContactPhone());
        dto.setAllowSMS(e.isAllowSMS());
        dto.setAllowEmail(e.isAllowEmail());
        dto.setAllowVoiceMessage(e.isAllowVoiceMessage());
        dto.setAllowMailMessage(e.isAllowMailMessage());
        return dto;
    }

    private void mapToEntity(PortalDemographicsDto dto, PortalDemographics e) {
        e.setFirstName(dto.getFirstName());
        e.setMiddleName(dto.getMiddleName());
        e.setLastName(dto.getLastName());
        e.setDob(dto.getDob());
        e.setSex(dto.getSex());
        e.setMaritalStatus(dto.getMaritalStatus());
        e.setAddress(dto.getAddress());
        e.setCity(dto.getCity());
        e.setState(dto.getState());
        e.setPostalCode(dto.getPostalCode());
        e.setCountry(dto.getCountry());
        e.setPhoneMobile(dto.getPhoneMobile());
        e.setContactEmail(dto.getContactEmail());
        e.setEmergencyContactName(dto.getEmergencyContactName());
        e.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        e.setAllowSMS(dto.isAllowSMS());
        e.setAllowEmail(dto.isAllowEmail());
        e.setAllowVoiceMessage(dto.isAllowVoiceMessage());
        e.setAllowMailMessage(dto.isAllowMailMessage());
    }
}
