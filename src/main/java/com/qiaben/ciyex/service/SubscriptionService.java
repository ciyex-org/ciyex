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
        String startDateStr = null;
        if (sub.getStartDate() != null) {
            // Convert to date-only string (yyyy-MM-dd) to avoid exposing time portion
            try {
                startDateStr = sub.getStartDate().toLocalDate().toString();
            } catch (Exception e) {
                startDateStr = sub.getStartDate().toString();
            }
        }

        return SubscriptionDto.builder()
                .id(sub.getId())
                .orgId(sub.getOrgId())
                .userId(sub.getUserId())
                .service(sub.getService())
                .billingCycle(sub.getBillingCycle())
                .status(sub.getStatus())
                .startDate(startDateStr)
                .price(sub.getPrice())
                .build();
    }

    private Subscription mapToEntity(SubscriptionDto dto) {
        Subscription.SubscriptionBuilder b = Subscription.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .userId(dto.getUserId())
                .service(dto.getService())
                .billingCycle(dto.getBillingCycle())
                .status(dto.getStatus())
                .price(dto.getPrice());

        if (dto.getStartDate() != null) {
            try {
                LocalDate ld = LocalDate.parse(dto.getStartDate());
                b.startDate(ld.atStartOfDay());
            } catch (Exception e) {
                b.startDate(LocalDateTime.parse(dto.getStartDate()));
            }
        }

        return b.build();
    }

    /* ------------ CRUD ------------ */
    public SubscriptionDto create(SubscriptionDto dto) {
        if (dto.getStatus() == null) {
            dto.setStatus("ACTIVE");
        }

        Subscription saved = repository.save(mapToEntity(dto));

        LocalDateTime startDateTime = saved.getStartDate();
        if (startDateTime == null && dto.getStartDate() != null) {
            try {
                LocalDate localDate = LocalDate.parse(dto.getStartDate());
                startDateTime = localDate.atStartOfDay();
            } catch (Exception e) {
                startDateTime = LocalDateTime.parse(dto.getStartDate());
            }
        }

        LocalDateTime dueDate = dto.getBillingCycle().equalsIgnoreCase("Yearly")
                ? startDateTime.plusYears(1)
                : startDateTime.plusMonths(1);

        if (saved.getUserId() == null && saved.getOrgId() == null) {
            throw new RuntimeException("Subscription must have either userId or orgId for invoice generation");
        }

        InvoiceBillDto invoice = InvoiceBillDto.builder()
                .orgId(saved.getOrgId())
                .userId(saved.getUserId())
                .subscriptionId(saved.getId())
                .amount(saved.getPrice())
                .status(InvoiceStatus.UNPAID)
                .dueDate(dueDate)
                .build();

        invoiceBillService.createInvoice(invoice);

        return mapToDto(saved);
    }

    public SubscriptionDto update(Long id, SubscriptionDto dto) {
        Subscription existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        boolean priceChanged = false;

        if (dto.getService() != null) existing.setService(dto.getService());
        if (dto.getBillingCycle() != null) existing.setBillingCycle(dto.getBillingCycle());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getStartDate() != null) {
            try {
                LocalDate ld = LocalDate.parse(dto.getStartDate());
                existing.setStartDate(ld.atStartOfDay());
            } catch (Exception e) {
                existing.setStartDate(LocalDateTime.parse(dto.getStartDate()));
            }
        }
        if (dto.getPrice() != null && !dto.getPrice().equals(existing.getPrice())) {
            existing.setPrice(dto.getPrice());
            priceChanged = true;
        }

        Subscription saved = repository.save(existing);

        if (priceChanged) {
            LocalDateTime startDateTime = saved.getStartDate();
            if (startDateTime == null && saved.getStartDate() != null) {
                startDateTime = saved.getStartDate();
            }

            LocalDateTime dueDate = saved.getBillingCycle().equalsIgnoreCase("Yearly")
                    ? startDateTime.plusYears(1)
                    : startDateTime.plusMonths(1);

            InvoiceBillDto invoice = InvoiceBillDto.builder()
                    .orgId(saved.getOrgId())
                    .userId(saved.getUserId())
                    .subscriptionId(saved.getId())
                    .amount(saved.getPrice())
                    .status(InvoiceStatus.UNPAID)
                    .dueDate(dueDate)
                    .build();

            invoiceBillService.createInvoice(invoice);
        }

        return mapToDto(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<SubscriptionDto> getById(Long id) {
        return repository.findById(id).map(this::mapToDto);
    }

    public List<SubscriptionDto> getAll() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Optional<SubscriptionDto> getByIdAndOrg(Long id, Long orgId) {
        return repository.findById(id)
                .filter(sub -> sub.getOrgId().equals(orgId))
                .map(this::mapToDto);
    }

    public List<SubscriptionDto> getAllByOrg(Long orgId) {
        return repository.findByOrgId(orgId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}
