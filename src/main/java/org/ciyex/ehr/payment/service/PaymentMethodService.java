package org.ciyex.ehr.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.payment.dto.PatientPaymentMethodDto;
import org.ciyex.ehr.payment.entity.PatientPaymentMethod;
import org.ciyex.ehr.payment.repository.PatientPaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {

    private final PatientPaymentMethodRepository repo;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    @Transactional(readOnly = true)
    public List<PatientPaymentMethodDto> getByPatient(Long patientId) {
        return repo.findByOrgAliasAndPatientIdAndIsActiveTrueOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PatientPaymentMethodDto getById(Long id) {
        return repo.findByIdAndOrgAlias(id, orgAlias())
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Payment method not found: " + id));
    }

    @Transactional
    public PatientPaymentMethodDto create(Long patientId, PatientPaymentMethodDto dto) {
        var method = PatientPaymentMethod.builder()
                .patientId(patientId)
                .patientName(dto.getPatientName())
                .methodType(dto.getMethodType())
                .cardBrand(dto.getCardBrand())
                .lastFour(dto.getLastFour())
                .expMonth(dto.getExpMonth())
                .expYear(dto.getExpYear())
                .cardholderName(dto.getCardholderName())
                .bankName(dto.getBankName())
                .accountType(dto.getAccountType())
                .routingLastFour(dto.getRoutingLastFour())
                .billingAddress(dto.getBillingAddress())
                .billingZip(dto.getBillingZip())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .isActive(true)
                .stripePaymentMethodId(dto.getStripePaymentMethodId())
                .stripeCustomerId(dto.getStripeCustomerId())
                .tokenReference(dto.getTokenReference())
                .nickname(dto.getNickname())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();

        // If this is set as default, clear other defaults first
        if (Boolean.TRUE.equals(method.getIsDefault())) {
            repo.clearDefaultsForPatient(orgAlias(), patientId);
        }

        method = repo.save(method);
        return toDto(method);
    }

    @Transactional
    public PatientPaymentMethodDto update(Long id, PatientPaymentMethodDto dto) {
        var method = repo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Payment method not found: " + id));

        if (dto.getNickname() != null) method.setNickname(dto.getNickname());
        if (dto.getBillingAddress() != null) method.setBillingAddress(dto.getBillingAddress());
        if (dto.getBillingZip() != null) method.setBillingZip(dto.getBillingZip());
        if (dto.getExpMonth() != null) method.setExpMonth(dto.getExpMonth());
        if (dto.getExpYear() != null) method.setExpYear(dto.getExpYear());
        if (dto.getCardholderName() != null) method.setCardholderName(dto.getCardholderName());
        if (dto.getNotes() != null) method.setNotes(dto.getNotes());

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            repo.clearDefaultsForPatient(orgAlias(), method.getPatientId());
            method.setIsDefault(true);
        }

        return toDto(repo.save(method));
    }

    @Transactional
    public void deactivate(Long id) {
        var method = repo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Payment method not found: " + id));
        method.setIsActive(false);
        method.setIsDefault(false);
        repo.save(method);
    }

    @Transactional
    public PatientPaymentMethodDto setDefault(Long patientId, Long methodId) {
        var method = repo.findByIdAndOrgAlias(methodId, orgAlias())
                .filter(m -> m.getPatientId().equals(patientId))
                .orElseThrow(() -> new NoSuchElementException("Payment method not found: " + methodId));

        repo.clearDefaultsForPatient(orgAlias(), patientId);
        method.setIsDefault(true);
        return toDto(repo.save(method));
    }

    private PatientPaymentMethodDto toDto(PatientPaymentMethod e) {
        return PatientPaymentMethodDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .methodType(e.getMethodType())
                .cardBrand(e.getCardBrand())
                .lastFour(e.getLastFour())
                .expMonth(e.getExpMonth())
                .expYear(e.getExpYear())
                .cardholderName(e.getCardholderName())
                .bankName(e.getBankName())
                .accountType(e.getAccountType())
                .routingLastFour(e.getRoutingLastFour())
                .billingAddress(e.getBillingAddress())
                .billingZip(e.getBillingZip())
                .isDefault(e.getIsDefault())
                .isActive(e.getIsActive())
                .stripePaymentMethodId(e.getStripePaymentMethodId())
                .stripeCustomerId(e.getStripeCustomerId())
                .tokenReference(e.getTokenReference())
                .nickname(e.getNickname())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }
}
