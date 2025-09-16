

package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.entity.ProviderNote;

/**
 * Abstraction to persist/sync provider notes to an external system (e.g., FHIR server).
 * A no-op implementation is provided so the app runs even without integration.
 */
public interface ExternalProviderNoteStorage {
    void onCreated(ProviderNote note);
    void onUpdated(ProviderNote note);
    void onDeleted(ProviderNote note);
}
