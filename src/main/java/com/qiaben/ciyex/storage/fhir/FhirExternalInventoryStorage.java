package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.InventoryDto;
import com.qiaben.ciyex.storage.ExternalInventoryStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Identifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StorageType("fhir")
@Component("fhirExternalInventoryStorage")
@Slf4j
public class FhirExternalInventoryStorage implements ExternalInventoryStorage {

    @Override
    public String createInventory(InventoryDto dto) {
        // Example mapping to FHIR Device (simple stub)
        Device device = new Device();
        device.setDeviceName(Collections.singletonList(
                new Device.DeviceDeviceNameComponent().setName(dto.getName())
        ));
        device.setLotNumber(dto.getLot());
        if (dto.getSku() != null) {
            device.setIdentifier(Collections.singletonList(new Identifier().setValue(dto.getSku())));
        }
        String externalId = UUID.randomUUID().toString();
        log.info("FHIR create Inventory '{}' -> externalId={}", dto.getName(), externalId);
        return externalId;
    }

    @Override
    public void updateInventory(InventoryDto dto, String externalId) {
        log.info("FHIR update Inventory externalId={} name={}", externalId, dto.getName());
        // TODO: map & update FHIR Device
    }

    @Override
    public InventoryDto getInventory(String externalId) {
        log.info("FHIR get Inventory externalId={}", externalId);
        InventoryDto dto = new InventoryDto();
        dto.setFhirId(externalId);
        return dto;
    }

    @Override
    public void deleteInventory(String externalId) {
        log.info("FHIR delete Inventory externalId={}", externalId);
        // TODO: delete FHIR resource
    }

    @Override
    public List<InventoryDto> searchAllInventory() {
        log.info("FHIR searchAll Inventory");
        return Collections.emptyList();
    }
}
