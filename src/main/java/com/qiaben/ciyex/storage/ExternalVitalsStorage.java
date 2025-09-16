package com.qiaben.ciyex.storage;



import com.qiaben.ciyex.entity.Vitals;

public interface ExternalVitalsStorage {
    void save(Vitals vitals);
    void delete(Long id);
    byte[] print(Vitals vitals);
}
