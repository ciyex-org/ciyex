package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalImmunizationStorage")
@Slf4j
public class FhirExternalImmunizationStorage implements ExternalStorage<ImmunizationDto> {

    private final FhirClientProvider fhirClientProvider;

    public FhirExternalImmunizationStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public String create(ImmunizationDto dto) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        Immunization fhir = mapToFhir(dto.getImmunizations().get(0));
        return client.create().resource(fhir).execute().getId().getIdPart();
    }

    @Override
    public void update(ImmunizationDto dto, String externalId) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        Immunization fhir = mapToFhir(dto.getImmunizations().get(0));
        fhir.setId(externalId);
        client.update().resource(fhir).execute();
    }

    @Override
    public ImmunizationDto get(String externalId) {
        IGenericClient client = fhirClientProvider.getForCurrentOrg();
        Immunization fhir = client.read().resource(Immunization.class).withId(externalId).execute();
        ImmunizationDto dto = new ImmunizationDto();
        dto.setImmunizations(List.of(mapFromFhir(fhir)));
        return dto;
    }

    @Override
    public void delete(String externalId) {
        fhirClientProvider.getForCurrentOrg().delete().resourceById("Immunization", externalId).execute();
    }

    @Override
    public List<ImmunizationDto> searchAll() {
        Bundle bundle = fhirClientProvider.getForCurrentOrg()
                .search()
                .forResource(Immunization.class)
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream().map(e -> {
            Immunization fhir = (Immunization) e.getResource();
            ImmunizationDto dto = new ImmunizationDto();
            dto.setImmunizations(List.of(mapFromFhir(fhir)));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return ImmunizationDto.class.isAssignableFrom(entityType);
    }

    private Immunization mapToFhir(ImmunizationDto.ImmunizationItem item) {
        Immunization fhir = new Immunization();
        if (item.getCvxCode() != null) {
            fhir.setVaccineCode(new CodeableConcept().addCoding(
                    new Coding().setSystem("http://hl7.org/fhir/sid/cvx").setCode(item.getCvxCode())));
        }
        if (item.getDateTimeAdministered() != null) {
            fhir.setOccurrence(new DateTimeType(item.getDateTimeAdministered()));
        }
        if (item.getLotNumber() != null) fhir.setLotNumber(item.getLotNumber());
        if (item.getManufacturer() != null) fhir.setManufacturer(new Reference().setDisplay(item.getManufacturer()));
        if (item.getNotes() != null) fhir.addNote().setText(item.getNotes());
        if (item.getRoute() != null) fhir.setRoute(new CodeableConcept().setText(item.getRoute()));
        if (item.getAdministrationSite() != null) fhir.setSite(new CodeableConcept().setText(item.getAdministrationSite()));
        return fhir;
    }

    private ImmunizationDto.ImmunizationItem mapFromFhir(Immunization fhir) {
        ImmunizationDto.ImmunizationItem item = new ImmunizationDto.ImmunizationItem();
        if (fhir.getVaccineCode() != null && fhir.getVaccineCode().hasCoding()) {
            item.setCvxCode(fhir.getVaccineCode().getCodingFirstRep().getCode());
        }
        if (fhir.hasOccurrenceDateTimeType()) {
            item.setDateTimeAdministered(fhir.getOccurrenceDateTimeType().getValueAsString());
        }
        item.setLotNumber(fhir.getLotNumber());
        if (fhir.getManufacturer() != null) item.setManufacturer(fhir.getManufacturer().getDisplay());
        if (!fhir.getNote().isEmpty()) item.setNotes(fhir.getNoteFirstRep().getText());
        if (fhir.getRoute() != null) item.setRoute(fhir.getRoute().getText());
        if (fhir.getSite() != null) item.setAdministrationSite(fhir.getSite().getText());
        return item;
    }
}
