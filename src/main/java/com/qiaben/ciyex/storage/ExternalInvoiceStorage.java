package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.InvoiceDto;
import java.util.List;
import java.util.Optional;

public interface ExternalInvoiceStorage {
    String create(InvoiceDto dto);                 // return FHIR Invoice id
    void update(String externalId, InvoiceDto dto);
    Optional<InvoiceDto> get(String externalId);
    void delete(String externalId);
    List<InvoiceDto> searchAll(Long patientId);
    List<InvoiceDto> searchAll(Long patientId, Long encounterId);
}
