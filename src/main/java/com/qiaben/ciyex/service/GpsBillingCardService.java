package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.GpsBillingCardDto;
import com.qiaben.ciyex.entity.GpsBillingCard;
import com.qiaben.ciyex.repository.GpsBillingCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GpsBillingCardService {

    private final GpsBillingCardRepository repository;

    /* ---------------- CREATE ---------------- */
    @Transactional
    public GpsBillingCardDto create(GpsBillingCardDto dto) {
        GpsBillingCard entity = toEntity(dto);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        if (entity.isDefault()) {
            clearDefaultForUser(entity.getUserId());
        }

        // ✅ Tokenize card if gpsCustomerVaultId not provided
        if (entity.getGpsCustomerVaultId() == null || entity.getGpsCustomerVaultId().isBlank()) {
            entity.setGpsCustomerVaultId(gpsTokenize(dto));
        }

        return toDto(repository.save(entity));
    }

    /* ---------------- READ ---------------- */
    public List<GpsBillingCardDto> getAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<GpsBillingCardDto> getAllByUser(Long userId) {
       /* return repository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());*/
        return null;
    }

    public Optional<GpsBillingCardDto> getById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    /* ---------------- UPDATE ---------------- */
    @Transactional
    public GpsBillingCardDto update(Long id, GpsBillingCardDto dto) {
        GpsBillingCard entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setAddress(dto.getAddress());
        entity.setStreet(dto.getStreet());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZip(dto.getZip());
        entity.setBrand(dto.getBrand());
        entity.setLast4(dto.getLast4());
        entity.setExpMonth(dto.getExpMonth());
        entity.setExpYear(dto.getExpYear());
        entity.setUpdatedAt(LocalDateTime.now());

        if (dto.isDefault()) {
            clearDefaultForUser(entity.getUserId());
            entity.setDefault(true);
        }

        return toDto(repository.save(entity));
    }

    /* ---------------- DELETE ---------------- */
    @Transactional
    public void delete(Long id) {
        repository.findById(id).ifPresent(repository::delete);
    }

    /* ---------------- SET DEFAULT ---------------- */
    @Transactional
    public GpsBillingCardDto setDefault(Long id) {
        GpsBillingCard card = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        clearDefaultForUser(card.getUserId());
        card.setDefault(true);
        card.setUpdatedAt(LocalDateTime.now());

        return toDto(repository.save(card));
    }

    private void clearDefaultForUser(Long userId) {
        /*repository.findByUserId(userId).forEach(c -> {
            if (c.isDefault()) {
                c.setDefault(false);
                repository.save(c);
            }
        });*/
    }

    /* ---------------- MOCK GPS TOKENIZE ---------------- */
    private String gpsTokenize(GpsBillingCardDto dto) {
        // ✅ If cardNumber+cvv present, mock tokenize
        if (dto.getCardNumber() != null && dto.getCvv() != null) {
            return "GPS-" + dto.getCardNumber().substring(dto.getCardNumber().length() - 4)
                    + "-" + System.currentTimeMillis();
        }
        // fallback
        return "MOCK-" + System.currentTimeMillis();
    }

    /* ---------------- MAPPERS ---------------- */
    private GpsBillingCardDto toDto(GpsBillingCard entity) {
        return GpsBillingCardDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .gpsCustomerVaultId(entity.getGpsCustomerVaultId())
                .brand(entity.getBrand())
                .last4(entity.getLast4())
                .expMonth(entity.getExpMonth())
                .expYear(entity.getExpYear())
                .isDefault(entity.isDefault())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .address(entity.getAddress())
                .street(entity.getStreet())
                .city(entity.getCity())
                .state(entity.getState())
                .zip(entity.getZip())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private GpsBillingCard toEntity(GpsBillingCardDto dto) {
        return GpsBillingCard.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .gpsCustomerVaultId(dto.getGpsCustomerVaultId())
                .brand(dto.getBrand())
                .last4(dto.getLast4())
                .expMonth(dto.getExpMonth())
                .expYear(dto.getExpYear())
                .isDefault(dto.isDefault())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .address(dto.getAddress())
                .street(dto.getStreet())
                .city(dto.getCity())
                .state(dto.getState())
                .zip(dto.getZip())
                .build();
    }
}
