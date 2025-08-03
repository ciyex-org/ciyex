package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ProviderDto;

import java.util.List;

public interface ExternalProviderStorage extends ExternalStorage<ProviderDto> {
    /**
     * Creates a new provider in the external storage (e.g., FHIR server) and returns the external ID.
     *
     * @param providerDto The provider DTO to create
     * @return The external ID (e.g., FHIR resource ID)
     */
    String createProvider(ProviderDto providerDto);

    /**
     * Updates an existing provider in the external storage.
     *
     * @param providerDto The provider DTO with updated data
     * @param externalId The external ID of the provider to update
     */
    void updateProvider(ProviderDto providerDto, String externalId);

    /**
     * Retrieves a provider from the external storage by its external ID and maps it to a DTO.
     *
     * @param externalId The external ID of the provider
     * @return The provider DTO, or null if not found
     */
    ProviderDto getProvider(String externalId);

    /**
     * Deletes a provider from the external storage by its external ID.
     *
     * @param externalId The external ID of the provider to delete
     */
    void deleteProvider(String externalId);

    /**
     * Searches for all providers in the external storage filtered by the tenant tag and maps them to DTOs.
     *
     * @return A list of provider DTOs
     */
    List<ProviderDto> searchAllProviders();
}