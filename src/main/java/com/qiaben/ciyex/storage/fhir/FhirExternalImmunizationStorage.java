//package com.qiaben.ciyex.storage.fhir;
//
//
//import com.qiaben.ciyex.dto.ImmunizationDto;
//import com.qiaben.ciyex.storage.ExternalImmunizationStorage;
//import org.hl7.fhir.r4.model.Immunization;
//import org.hl7.fhir.r4.model.CodeableConcept;
//import org.hl7.fhir.r4.model.DateType;
//import org.hl7.fhir.r4.model.Immunization.ImmunizationPerformerComponent;
//import org.springframework.stereotype.Component;
//import java.util.Collections;
//
//@Component
//public class FhirExternalImmunizationStorage implements ExternalImmunizationStorage {
//
//    @Override
//    public void saveImmunization(ImmunizationDto immunizationDto) {
//        // Create a new FHIR Immunization object
//        Immunization fhirImmunization = new Immunization();
//
//        // Mapping vaccine name to FHIR Immunization vaccineCode
//        CodeableConcept vaccineCode = new CodeableConcept();
//        vaccineCode.setText(immunizationDto.getVaccineName());
//        fhirImmunization.setVaccineCode(vaccineCode);
//
//        // Correct usage of DateType for Date
//        DateType dateAdministered = new DateType(immunizationDto.getDateAdministered()); // Ensure format is "yyyy-MM-dd"
//        fhirImmunization.setExpirationDateElement(dateAdministered);
//
//        // Mapping performer (administeredBy) to FHIR Immunization performer
//        ImmunizationPerformerComponent performer = new ImmunizationPerformerComponent();
//        performer.setActor(new org.hl7.fhir.r4.model.Reference(immunizationDto.getAdministeredBy()));
//
//        // Set performer in a list (FHIR requires list)
//        fhirImmunization.setPerformer(Collections.singletonList(performer));
//
//        // You would then send `fhirImmunization` to the FHIR server, e.g., via an API.
//        // fhirClient.save(fhirImmunization);
//    }
//
//    @Override
//    public ImmunizationDto getImmunizationById(Long id) {
//        // Fetch Immunization from FHIR system (pseudo-code)
//        // Example: Immunization fhirImmunization = fhirClient.getImmunization(id);
//
//        // Convert fetched FHIR Immunization back to ImmunizationDto
//        ImmunizationDto immunizationDto = new ImmunizationDto();
//        immunizationDto.setId(id);
//        immunizationDto.setVaccineName("Sample Vaccine");
//        immunizationDto.setDateAdministered("2023-08-07"); // Convert from FHIR DateType
//        immunizationDto.setPatientId(123L);
//        immunizationDto.setAdministeredBy("Dr. John Doe");
//
//        return immunizationDto;
//    }
//}

package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.storage.ExternalImmunizationStorage;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Immunization.ImmunizationPerformerComponent;
import org.springframework.stereotype.Component;
import java.util.Collections;

@Component
public class FhirExternalImmunizationStorage implements ExternalImmunizationStorage {

    @Override
    public void saveImmunization(ImmunizationDto immunizationDto) {
        Immunization fhirImmunization = new Immunization();
        CodeableConcept vaccineCode = new CodeableConcept();
        vaccineCode.setText(immunizationDto.getVaccineName());
        fhirImmunization.setVaccineCode(vaccineCode);
        DateType dateAdministered = new DateType(immunizationDto.getDateAdministered());
        fhirImmunization.setExpirationDateElement(dateAdministered);
        ImmunizationPerformerComponent performer = new ImmunizationPerformerComponent();
        performer.setActor(new org.hl7.fhir.r4.model.Reference(immunizationDto.getAdministeredBy()));
        fhirImmunization.setPerformer(Collections.singletonList(performer));

        // Add encounterId as an identifier using the encounter relationship
        if (immunizationDto.getEncounterId() != null) {
            fhirImmunization.addIdentifier().setValue(String.valueOf(immunizationDto.getEncounterId()));
        }

        // Send to FHIR server (pseudo-code)
        // fhirClient.save(fhirImmunization);
    }

    @Override
    public ImmunizationDto getImmunizationById(Long id) {
        // Fetch from FHIR system (pseudo-code)
        ImmunizationDto immunizationDto = new ImmunizationDto();
        immunizationDto.setId(id);
        immunizationDto.setVaccineName("Sample Vaccine");
        immunizationDto.setDateAdministered("2023-08-07");
        immunizationDto.setPatientId(123L);
        immunizationDto.setAdministeredBy("Dr. John Doe");
        immunizationDto.setEncounterId(1L); // Example encounterId
        immunizationDto.setOrgId(1L);
        //immunizationDto.setImmuid(55L);
        immunizationDto.setExternaleId(56L);
        return immunizationDto;
    }
}

//package com.qiaben.ciyex.storage.fhir;
//
//import com.qiaben.ciyex.dto.ImmunizationDto;
//import com.qiaben.ciyex.storage.ExternalImmunizationStorage;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class FhirExternalImmunizationStorage implements ExternalImmunizationStorage {
//
//    @Override
//    public List<ImmunizationDto> getImmunizations(Long orgId, Long patientId) {
//        // Logic for retrieving immunization data from FHIR or any external source
//        return null;
//    }
//
//    @Override
//    public ImmunizationDto saveImmunization(Long orgId, ImmunizationDto dto) {
//        // Logic for saving immunization data into FHIR or another external source
//        return null;
//    }
//}
