package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalInvoiceStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Component @RequiredArgsConstructor @Slf4j
public class FhirExternalInvoiceStorage implements ExternalInvoiceStorage {
    private final FhirClientProvider fhirClientProvider;

    @Override public String create(InvoiceDto dto) {
        log.info("FHIR Invoice create patientId={}, encounterId={}", dto.getPatientId(), dto.getEncounterId());
        // Map dto -> FHIR Invoice (status, totalGross/Net, line items, payments as PriceComponent/Payment)
        return null;
    }
    @Override public void update(String externalId, InvoiceDto dto) { }
    @Override public Optional<InvoiceDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }
    @Override public List<InvoiceDto> searchAll(Long orgId, Long patientId) { return Collections.emptyList(); }
    @Override public List<InvoiceDto> searchAll(Long orgId, Long patientId, Long encounterId) { return Collections.emptyList(); }
}
