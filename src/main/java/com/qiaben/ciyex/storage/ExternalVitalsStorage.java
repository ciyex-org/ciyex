package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.VitalsDto;
import com.qiaben.ciyex.entity.Vitals;

public interface ExternalVitalsStorage extends ExternalStorage<VitalsDto> {
    void save(Vitals vitals);
    void delete(Long id);
    byte[] print(Vitals vitals);
}
