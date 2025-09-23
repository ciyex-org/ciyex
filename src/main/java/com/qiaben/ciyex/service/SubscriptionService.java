package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.InvoiceBillDto;
import com.qiaben.ciyex.dto.SubscriptionDto;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.entity.Subscription;
import com.qiaben.ciyex.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final InvoiceBillService invoiceBillService;

    /* ------------ Mapping ------------ */
    private SubscriptionDto mapToDto(Subscription sub) {
        return SubscriptionDto.builder()
                .id(sub.getId())
                .orgId(sub.getOrgId())
                .userId(sub.getUserId())
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
                .orgId(dto.getOrgId())
                .userId(dto.getUserId())
                .service(dto.getService())
                .billingCycle(dto.getBillingCycle())
                .scope(dto.getScope())
                .status(dto.getStatus())
                .startDate(dto.getStartDate())
                .price(dto.getPrice())
                .build();
    }

    /* ------------ CRUD ------------ */
    public SubscriptionDto create(SubscriptionDto dto) {
        Subscription saved = repository.save(mapToEntity(dto));

        // 🔹 Auto-create invoice when subscription is created
        LocalDate start = LocalDate.parse(dto.getStartDate());
        LocalDateTime dueDate = dto.getBillingCycle().equalsIgnoreCase("Yearly")
                ? start.plusYears(1).atStartOfDay()
                : start.plusMonths(1).atStartOfDay();

        InvoiceBillDto invoice = InvoiceBillDto.builder()
                .orgId(saved.getOrgId())
                .userId(saved.getUserId())
                .subscriptionId(saved.getId())
                .amount(saved.getPrice())
                .status(InvoiceStatus.UNPAID)   // ✅ FIX: use enum
                .dueDate(dueDate)
                .createdAt(LocalDateTime.now())
                .build();

        invoiceBillService.createInvoice(invoice);
        return mapToDto(saved);
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

    /* ------------ ORG-specific Methods ------------ */
    public Optional<SubscriptionDto> getByIdAndOrg(Long id, Long orgId) {
        return repository.findById(id)
                .filter(sub -> sub.getOrgId().equals(orgId))
                .map(this::mapToDto);
    }

    public List<SubscriptionDto> getAllByOrg(Long orgId) {
        return repository.findAll().stream()
                .filter(sub -> sub.getOrgId().equals(orgId))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}
