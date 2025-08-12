package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ImmunizationDto;



public interface ExternalImmunizationStorage {
    void saveImmunization(ImmunizationDto immunizationDto);
    ImmunizationDto getImmunizationById(Long id);
}

//package com.qiaben.ciyex.storage;
//
//import com.qiaben.ciyex.dto.ImmunizationDto;
//import java.util.List;
//
//public interface ExternalImmunizationStorage {
//    List<ImmunizationDto> getImmunizations(Long orgId, Long patientId);
//    ImmunizationDto saveImmunization(Long orgId, ImmunizationDto dto);
//}
