package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.PortalProfileDto;
import com.qiaben.ciyex.entity.portal.PortalProfile;
import com.qiaben.ciyex.repository.portal.PortalProfileRepository;

import org.springframework.stereotype.Service;

@Service
public class PortalProfileService {

    private final PortalProfileRepository repository;

    public PortalProfileService(PortalProfileRepository repository) {
        this.repository = repository;
    }

    public PortalProfileDto getProfile(Long userId) {
        return repository.findByUserId(userId).map(this::toDto).orElse(null);
    }

    public PortalProfileDto updateProfile(Long userId, PortalProfileDto dto) {
        PortalProfile profile = repository.findByUserId(userId)
                .orElse(PortalProfile.builder().userId(userId).build());

        profile.setFirstName(dto.getFirstName());
        profile.setLastName(dto.getLastName());
        profile.setPhone(dto.getPhone());
        profile.setEmail(dto.getEmail());
        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setStreet(dto.getStreet());
        profile.setCity(dto.getCity());
        profile.setState(dto.getState());
        profile.setPostalCode(dto.getPostalCode());
        profile.setCountry(dto.getCountry());

        return toDto(repository.save(profile));
    }

    private PortalProfileDto toDto(PortalProfile profile) {
        return PortalProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .email(profile.getEmail())
                .dateOfBirth(profile.getDateOfBirth())
                .street(profile.getStreet())
                .city(profile.getCity())
                .state(profile.getState())
                .postalCode(profile.getPostalCode())
                .country(profile.getCountry())
                .build();
    }
}
