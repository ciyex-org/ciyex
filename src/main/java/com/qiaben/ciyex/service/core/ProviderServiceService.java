package com.qiaben.ciyex.service.core;


import com.qiaben.ciyex.dto.core.ServiceDTO;
import com.qiaben.ciyex.entity.ProviderService;
import com.qiaben.ciyex.repository.ProviderServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderServiceService {

    private final ProviderServiceRepository repository;

    public ProviderService saveService(ServiceDTO dto) {
        ProviderService entity = ProviderService.builder()
                .serviceName(dto.getServiceName())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .isOnline(dto.getIsOnline())
                .hospitalName(dto.getHospitalName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .status(dto.getStatus())
                .mode(dto.getMode())
                .category(dto.getCategory())
                .insuranceAccepted(dto.getInsuranceAccepted())
                .build();
        return repository.save(entity);
    }
}
