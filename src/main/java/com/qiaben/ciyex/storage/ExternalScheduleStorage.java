package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.dto.ScheduleDto;
import java.util.List;

/**
 * External schedule storage contract.
 * Defines CRUD operations for schedules in external systems (e.g., FHIR).
 * Mirrors ExternalProviderStorage style for consistency.
 */
public interface ExternalScheduleStorage extends ExternalStorage<ScheduleDto> {

    /**
     * Creates a new schedule in the external storage (e.g., FHIR server)
     * and returns the external ID.
     *
     * @param scheduleDto The schedule DTO to create
     * @return The external ID (e.g., FHIR resource ID)
     */
    String createSchedule(ScheduleDto scheduleDto);

    /**
     * Updates an existing schedule in the external storage.
     *
     * @param scheduleDto The schedule DTO with updated data
     * @param externalId The external ID of the schedule to update
     */
    void updateSchedule(ScheduleDto scheduleDto, String externalId);

    /**
     * Retrieves a schedule from the external storage by its external ID.
     *
     * @param externalId The external ID of the schedule
     * @return The schedule DTO, or null if not found
     */
    ScheduleDto getSchedule(String externalId);

    /**
     * Deletes a schedule from the external storage by its external ID.
     *
     * @param externalId The external ID of the schedule to delete
     */
    void deleteSchedule(String externalId);

    /**
     * Searches for all schedules in the external storage filtered by tenant/org,
     * and maps them to DTOs.
     *
     * @return A list of schedule DTOs
     */
    List<ScheduleDto> searchAllSchedules();

    /**
     * Indicates whether this storage supports schedules.
     * Default returns true; implementations may override if needed.
     *
     * @param entityType The entity class
     * @return true if supported
     */
    @Override
    default boolean supports(Class<?> entityType) {
        return ScheduleDto.class.isAssignableFrom(entityType);
    }
}
