





package com.qiaben.ciyex.storage.fhir;


import com.qiaben.ciyex.entity.ProviderNote;
import com.qiaben.ciyex.storage.ExternalProviderNoteStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub FHIR external storage. Replace log statements with actual FHIR client calls when ready.
 */
@Component
@Slf4j
public class FhirExternalProviderNoteStorage implements ExternalProviderNoteStorage {

    @Override
    public void onCreated(ProviderNote note) {
        log.info("[FHIR] create ProviderNote externalId={} encounter={} patient={}",
                note.getExternalId(), note.getEncounterId(), note.getPatientId());
    }

    @Override
    public void onUpdated(ProviderNote note) {
        log.info("[FHIR] update ProviderNote id={} externalId={}", note.getId(), note.getExternalId());
    }

    @Override
    public void onDeleted(ProviderNote note) {
        log.info("[FHIR] delete ProviderNote id={} externalId={}", note.getId(), note.getExternalId());
    }
}

