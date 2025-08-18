package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.storage.ExternalEncounterStorage;
import org.springframework.stereotype.Component;

@Component
public class FhirExternalEncounterStorage implements ExternalEncounterStorage {

    @Override
    public void storeEncounter(EncounterDto encounterDto) {
        // Ensure all necessary fields are populated before storing
        if (encounterDto.getInCollection() == null) {
            encounterDto.setInCollection(false); // Provide a default value if null
        }

        // Add your logic to store the encounter data to an external FHIR system
        // This could be saving it to the database, making a call to an external API, etc.

        // Example of storing the encounter (you can modify this as needed)
        System.out.println("Storing encounter: " + encounterDto);
    }
}

