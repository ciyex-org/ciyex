package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.SupplierDto;
import java.util.List;

public interface ExternalSupplierStorage extends ExternalStorage<SupplierDto> {
    String createSupplier(SupplierDto dto);
    void updateSupplier(SupplierDto dto, String externalId);
    SupplierDto getSupplier(String externalId);
    void deleteSupplier(String externalId);
    List<SupplierDto> searchAllSuppliers();
}
