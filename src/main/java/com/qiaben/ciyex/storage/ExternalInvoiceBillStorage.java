package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.InvoiceBillDto;

import java.util.List;
import java.util.Optional;

public interface ExternalInvoiceBillStorage {
    String create(InvoiceBillDto dto);
    void update(String externalId, InvoiceBillDto dto);
    Optional<InvoiceBillDto> get(String externalId);
    void delete(String externalId);
    List<InvoiceBillDto> searchAll(Long orgId, Long userId);
}
