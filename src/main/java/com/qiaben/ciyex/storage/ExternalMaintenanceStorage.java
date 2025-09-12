package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.MaintenanceDto;
import java.util.List;

public interface ExternalMaintenanceStorage extends ExternalStorage<MaintenanceDto> {
    String createMaintenance(MaintenanceDto dto);
    void updateMaintenance(MaintenanceDto dto, String externalId);
    MaintenanceDto getMaintenance(String externalId);
    void deleteMaintenance(String externalId);
    List<MaintenanceDto> searchAllMaintenance();
}
