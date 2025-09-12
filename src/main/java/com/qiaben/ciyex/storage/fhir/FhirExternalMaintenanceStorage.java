package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.MaintenanceDto;
import com.qiaben.ciyex.storage.ExternalMaintenanceStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@StorageType("fhir")
@Component("fhirExternalMaintenanceStorage")
@Slf4j
public class FhirExternalMaintenanceStorage implements ExternalMaintenanceStorage {

    @Override
    public String createMaintenance(MaintenanceDto dto) {
        Task task = new Task();
        task.setStatus(Task.TaskStatus.REQUESTED);

        // ✅ Use fields that exist in MaintenanceDto
        task.setDescription(
                String.format("[%s] %s (Priority: %s)",
                        dto.getCategory(),
                        dto.getEquipment(),
                        dto.getPriority())
        );

        String externalId = UUID.randomUUID().toString();
        log.info("Generated externalId for Maintenance: {}", externalId);
        return externalId;
    }

    @Override
    public void updateMaintenance(MaintenanceDto dto, String externalId) {
        log.info("Updating maintenance {} in FHIR", externalId);
    }

    @Override
    public MaintenanceDto getMaintenance(String externalId) {
        MaintenanceDto dto = new MaintenanceDto();
        dto.setFhirId(externalId);
        return dto;
    }

    @Override
    public void deleteMaintenance(String externalId) {
        log.info("Deleting maintenance {} in FHIR", externalId);
    }

    @Override
    public List<MaintenanceDto> searchAllMaintenance() {
        return Collections.emptyList();
    }

    // 🔹 Bridge methods from ExternalStorage
    @Override
    public String create(MaintenanceDto entityDto) { return createMaintenance(entityDto); }
    @Override
    public void update(MaintenanceDto entityDto, String externalId) { updateMaintenance(entityDto, externalId); }
    @Override
    public MaintenanceDto get(String externalId) { return getMaintenance(externalId); }
    @Override
    public void delete(String externalId) { deleteMaintenance(externalId); }
    @Override
    public List<MaintenanceDto> searchAll() { return searchAllMaintenance(); }
    @Override
    public boolean supports(Class<?> entityType) { return MaintenanceDto.class.isAssignableFrom(entityType); }
}
