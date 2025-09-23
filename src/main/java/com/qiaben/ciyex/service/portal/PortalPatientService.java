package com.qiaben.ciyex.service.portal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.mapper.portal.PortalPatientMapper;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;

@Service
public class PortalPatientService {

    private final PortalPatientRepository patientRepository;
    private final PortalUserRepository userRepository;
    private final PortalPatientMapper mapper;

    public PortalPatientService(
            PortalPatientRepository patientRepository,
            PortalUserRepository userRepository,
            PortalPatientMapper mapper
    ) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Get the patient profile linked to the logged-in user.
     */
    @Transactional(readOnly = true)
    public PortalPatientDto getByUserId(Long userId) {
        PortalPatient patient = patientRepository.findByUser_Id(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Patient profile not found for userId: " + userId
            ));
        return mapper.toDto(patient);
    }

    /**
     * Update the patient profile for the logged-in user.
     */
    @Transactional
    public PortalPatientDto updatePatient(Long userId, PortalPatientDto updatedDto) {
        PortalPatient patient = patientRepository.findByUser_Id(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Patient profile not found for userId: " + userId
            ));

        mapper.updateEntityFromDto(updatedDto, patient);
        PortalPatient saved = patientRepository.save(patient);
        return mapper.toDto(saved);
    }

    /**
     * Ensure that a patient record exists for the given portal user.
     * If missing, auto-creates one from the PortalUser fields.
     */
    @Transactional
    public PortalPatientDto ensurePatientExists(Long userId) {
        if (patientRepository.existsByUser_Id(userId)) {
            return mapper.toDto(
                patientRepository.findByUser_Id(userId).get()
            );
        }

        PortalUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "User not found: " + userId
            ));

        PortalPatient newPatient = PortalPatient.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .dob(user.getDateOfBirth())
                .address(user.getStreet())
                .build();

        PortalPatient saved = patientRepository.save(newPatient);
        return mapper.toDto(saved);
    }
}
