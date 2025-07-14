package com.qiaben.ciyex.mapper;

import com.qiaben.ciyex.dto.core.PatientFormDTO;
import com.qiaben.ciyex.dto.fhir.FhirPatientDto;
import java.util.List;

public class PatientFhirMapper {

    public static FhirPatientDto fromPatientForm(PatientFormDTO dto) {
        FhirPatientDto fhir = new FhirPatientDto();
        fhir.setResourceType("Patient");

        // Set name
        FhirPatientDto.NameDto name = new FhirPatientDto.NameDto();
        name.setFamily(dto.getLastName());
        name.setGiven(List.of(dto.getFirstName()));
        name.setUse("official");
        fhir.setName(List.of(name));

        // Set gender (FHIR wants lower-case values)
        if (dto.getGender() != null) {
            fhir.setGender(dto.getGender().toLowerCase());
        }

        // Set birth date
        if (dto.getDateOfBirth() != null) {
            // Format as yyyy-MM-dd (FHIR)
            String dob = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dto.getDateOfBirth());
            fhir.setBirthDate(dob);
        }

        // Set address
        FhirPatientDto.AddressDto address = new FhirPatientDto.AddressDto();
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getZipCode());
        address.setLine(List.of(dto.getAddress()));
        fhir.setAddress(List.of(address));

        // (Add more mappings as needed!)

        return fhir;
    }
}
