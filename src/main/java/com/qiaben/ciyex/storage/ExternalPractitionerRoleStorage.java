package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.PractitionerRoleDto;
import java.util.List;

public interface ExternalPractitionerRoleStorage {
    PractitionerRoleDto createPractitionerRole(PractitionerRoleDto dto);
    List<PractitionerRoleDto> getAllPractitionerRoles();
}
