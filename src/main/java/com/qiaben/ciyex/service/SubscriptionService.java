package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.SubscriptionDto;
import com.qiaben.ciyex.entity.Subscription;
import com.qiaben.ciyex.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;

    private SubscriptionDto mapToDto(Subscription sub) {
        return SubscriptionDto.builder()
                .id(sub.getId())
                .service(sub.getService())
                .billingCycle(sub.getBillingCycle())
                .scope(sub.getScope())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .price(sub.getPrice())
                .build();
    }

    private Subscription mapToEntity(SubscriptionDto dto) {
        return Subscription.builder()
                .id(dto.getId())
                .service(dto.getService())
                .billingCycle(dto.getBillingCycle())
                .scope(dto.getScope())
                .status(dto.getStatus())
                .startDate(dto.getStartDate())
                .price(dto.getPrice())
                .build();
    }

    public SubscriptionDto create(SubscriptionDto dto) {
        return mapToDto(repository.save(mapToEntity(dto)));
    }

    public SubscriptionDto update(Long id, SubscriptionDto dto) {
        dto.setId(id);
        return mapToDto(repository.save(mapToEntity(dto)));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<SubscriptionDto> getById(Long id) {
        return repository.findById(id).map(this::mapToDto);
    }

    public List<SubscriptionDto> getAll() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }
}
