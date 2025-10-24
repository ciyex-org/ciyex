package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.qiaben.ciyex.dto.TemplateDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalTemplateStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@StorageType("fhir-template")
@Component("fhirExternalTemplateStorage")
@Slf4j
public class FhirExternalTemplateStorage implements ExternalTemplateStorage {

    private final FhirClientProvider fhirClientProvider;

    public FhirExternalTemplateStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public String create(TemplateDto dto) {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        IGenericClient client = fhirClientProvider.getForCurrentTenant();

        Communication comm = mapToFhir(dto);
        String externalId = client.create().resource(comm).execute().getId().getIdPart();
        log.info("Created FHIR Template externalId={} tenantName={}", externalId, tenantName);
        return externalId;
    }

    @Override
    public void update(TemplateDto dto, String externalId) {
        IGenericClient client = fhirClientProvider.getForCurrentTenant();
        Communication comm = mapToFhir(dto);
        comm.setId(externalId);
        client.update().resource(comm).execute();
        log.info("Updated FHIR Template externalId={}", externalId);
    }

    @Override
    public TemplateDto get(String externalId) {
        IGenericClient client = fhirClientProvider.getForCurrentTenant();
        Communication comm = client.read().resource(Communication.class).withId(externalId).execute();
        return mapFromFhir(comm);
    }

    @Override
    public void delete(String externalId) {
        IGenericClient client = fhirClientProvider.getForCurrentTenant();
        client.delete().resourceById("Communication", externalId).execute();
        log.info("Deleted FHIR Template externalId={}", externalId);
    }

    @Override
    public List<TemplateDto> searchAll() {
        IGenericClient client = fhirClientProvider.getForCurrentTenant();
        return client.search()
                .forResource(Communication.class)
                .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                .execute()
                .getEntry().stream()
                .map(e -> (Communication) e.getResource())
                .map(this::mapFromFhir)
                .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return TemplateDto.class.isAssignableFrom(entityType);
    }

    // === Mapping helpers ===

    private Communication mapToFhir(TemplateDto dto) {
        Communication comm = new Communication();
        if (dto.getTemplateName() != null) {
            comm.addCategory().addCoding().setCode(dto.getTemplateName());
        }
        if (dto.getSubject() != null) {
            comm.addNote(new Annotation().setText(dto.getSubject()));
        }
        if (dto.getBody() != null) {
            comm.addPayload().setContent(new StringType(dto.getBody()));
        }
        return comm;
    }

    private TemplateDto mapFromFhir(Communication comm) {
        TemplateDto dto = new TemplateDto();
        dto.setExternalId(comm.getIdElement().getIdPart());
        if (!comm.getCategory().isEmpty()) {
            dto.setTemplateName(comm.getCategoryFirstRep().getCodingFirstRep().getCode());
        }
        if (!comm.getNote().isEmpty()) {
            dto.setSubject(comm.getNoteFirstRep().getText());
        }
        if (!comm.getPayload().isEmpty() && comm.getPayloadFirstRep().getContent() instanceof StringType) {
            dto.setBody(((StringType) comm.getPayloadFirstRep().getContent()).getValue());
        }
        dto.setTenantName(RequestContext.get() != null ? RequestContext.get().getTenantName() : null);
        return dto;
    }
}
