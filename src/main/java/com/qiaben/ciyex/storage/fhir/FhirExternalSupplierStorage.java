package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.SupplierDto;
import com.qiaben.ciyex.storage.ExternalSupplierStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StorageType("fhir")
@Component("fhirExternalSupplierStorage")
@Slf4j
public class FhirExternalSupplierStorage implements ExternalSupplierStorage {

    @Override
    public String createSupplier(SupplierDto dto) {
        Organization org = new Organization();
        org.setName(dto.getName());
        String externalId = UUID.randomUUID().toString();
        log.info("Generated externalId for Supplier: {}", externalId);
        return externalId;
    }

    @Override
    public void updateSupplier(SupplierDto dto, String externalId) {
        log.info("Updating supplier {} in FHIR", externalId);
    }

    @Override
    public SupplierDto getSupplier(String externalId) {
        SupplierDto dto = new SupplierDto();
        dto.setExternalId(externalId);
        return dto;
    }

    @Override
    public void deleteSupplier(String externalId) {
        log.info("Deleting supplier {} in FHIR", externalId);
    }

    @Override
    public List<SupplierDto> searchAllSuppliers() {
        return Collections.emptyList();
    }

    @Override
    public String create(SupplierDto entityDto) { return createSupplier(entityDto); }
    @Override
    public void update(SupplierDto entityDto, String externalId) { updateSupplier(entityDto, externalId); }
    @Override
    public SupplierDto get(String externalId) { return getSupplier(externalId); }
    @Override
    public void delete(String externalId) { deleteSupplier(externalId); }
    @Override
    public List<SupplierDto> searchAll() { return searchAllSuppliers(); }
    @Override
    public boolean supports(Class<?> entityType) { return SupplierDto.class.isAssignableFrom(entityType); }
}
