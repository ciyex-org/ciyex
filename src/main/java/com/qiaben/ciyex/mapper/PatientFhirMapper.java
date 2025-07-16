package com.qiaben.ciyex.mapper;

//import com.qiaben.ciyex.dto.core.*;
//import com.qiaben.ciyex.dto.fhir.*;

import com.qiaben.ciyex.dto.core.*;
import com.qiaben.ciyex.dto.fhir.*;

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
    public static FhirDiagnosticReportDTO fromDiagnosisDTO(DiagnosisDTO diagnosisDTO) {
        FhirDiagnosticReportDTO report = new FhirDiagnosticReportDTO();
        // Map DiagnosisDTO fields to FhirDiagnosticReportDTO fields as needed
        // Example:
        report.setId(diagnosisDTO.getMedicalId()); // Map medicalId to id or another appropriate field
        // You can continue mapping other fields similarly

        return report;
    }
    public static FhirPatientBillDTO fromPatientBillDTO(PatientBillDTO dto) {
        return FhirPatientBillDTO.builder()
                .billId(dto.getBillId())
                .serviceId(dto.getServiceId())
                .serviceDate(dto.getServiceDate())
                .appointmentId(dto.getAppointmentId())
                .quantity(dto.getQuantity())
                .unitCost(dto.getUnitCost())
                .totalCost(dto.getTotalCost())
                .build();
    }

    public static FhirPaymentDTO fromPaymentDTO(PaymentDTO dto) {
        return FhirPaymentDTO.builder()
                .id(dto.getId())
                .billDate(dto.getBillDate())
                .build();
    }

    public static FhirVitalSignsDTO fromVitalSignsDTO(VitalSignsDTO dto) {
        return FhirVitalSignsDTO.builder()
                .patientId(dto.getPatientId())
                .medicalId(dto.getMedicalId())
                .bodyTemperature(dto.getBodyTemperature())
                .heartRate(dto.getHeartRate())
                .systolic(dto.getSystolic())
                .diastolic(dto.getDiastolic())
                .respiratoryRate(dto.getRespiratoryRate())
                .oxygenSaturation(dto.getOxygenSaturation())
                .weight(dto.getWeight())
                .height(dto.getHeight())
                .build();
    }
}
