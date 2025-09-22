package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.OrderDto;
import com.qiaben.ciyex.storage.ExternalOrderStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.hl7.fhir.r4.model.Identifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StorageType("fhir")
@Component("fhirExternalOrderStorage")
@Slf4j
public class FhirExternalOrderStorage implements ExternalOrderStorage {

    @Override
    public String createOrder(OrderDto dto) {
        SupplyRequest request = new SupplyRequest();
        request.setIdentifier(Collections.singletonList(
                new Identifier().setValue(dto.getOrderNumber())
        ));
        request.setStatus(mapStatus(dto.getStatus()));
        request.setAuthoredOnElement(new org.hl7.fhir.r4.model.DateTimeType(dto.getDate()));

        String externalId = UUID.randomUUID().toString();
        log.info("FHIR create Order '{}' supplier={} -> externalId={}",
                dto.getOrderNumber(), dto.getSupplier(), externalId);
        return externalId;
    }

    @Override
    public void updateOrder(OrderDto dto, String externalId) {
        log.info("FHIR update Order externalId={} orderNumber={} status={}",
                externalId, dto.getOrderNumber(), dto.getStatus());
        // TODO: implement mapping and update FHIR SupplyRequest
    }

    @Override
    public OrderDto getOrder(String externalId) {
        log.info("FHIR get Order externalId={}", externalId);
        OrderDto dto = new OrderDto();
        dto.setFhirId(externalId);
        return dto;
    }

    @Override
    public void deleteOrder(String externalId) {
        log.info("FHIR delete Order externalId={}", externalId);
        // TODO: delete FHIR resource
    }

    @Override
    public List<OrderDto> searchAllOrders() {
        log.info("FHIR searchAll Orders");
        return Collections.emptyList();
    }

    /**
     * Map UI statuses → FHIR statuses
     */
    private SupplyRequest.SupplyRequestStatus mapStatus(String status) {
        if (status == null) return SupplyRequest.SupplyRequestStatus.UNKNOWN;
        switch (status.toLowerCase()) {
            case "pending":
                return SupplyRequest.SupplyRequestStatus.DRAFT;
            case "received":
                return SupplyRequest.SupplyRequestStatus.COMPLETED;
            case "cancelled":
                return SupplyRequest.SupplyRequestStatus.CANCELLED;
            default:
                return SupplyRequest.SupplyRequestStatus.UNKNOWN;
        }
    }
}
