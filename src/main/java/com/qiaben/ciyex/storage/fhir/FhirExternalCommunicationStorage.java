package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.CommunicationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;

@StorageType("fhir")
@Component("fhirExternalCommunicationStorage")
@Slf4j
public class FhirExternalCommunicationStorage implements ExternalStorage<CommunicationDto> {

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";
    private final FhirClientProvider fhirClientProvider;

    public FhirExternalCommunicationStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public String create(CommunicationDto dto) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = mapToFhir(dto);
            return client.create().resource(list).execute().getId().getIdPart();
        });
    }

    @Override
    public void update(CommunicationDto dto, String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = mapToFhir(dto);
            list.setId(externalId);
            client.update().resource(list).execute();
            return null;
        });
    }

    @Override
    public CommunicationDto get(String externalId) {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = client.read().resource(ListResource.class).withId(externalId).execute();
            return mapFromFhir(list);
        });
    }

    @Override
    public void delete(String externalId) {
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            client.delete().resourceById("List", externalId).execute();
            return null;
        });
    }

    @Override
    public List<CommunicationDto> searchAll() {
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
        Bundle bundle = client.search()
            .forResource(ListResource.class)
            .where(new TokenClientParam("_tag").exactly().systemAndCode(TENANT_TAG_SYSTEM, tenantNameStr()))
                    .returnBundle(Bundle.class)
                    .execute();

            List<CommunicationDto> out = new ArrayList<>();
            List<Bundle.BundleEntryComponent> entries = new ArrayList<>(bundle.getEntry());

            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = client.loadPage().next(bundle).execute();
                entries.addAll(bundle.getEntry());
            }

            for (Bundle.BundleEntryComponent e : entries) {
                ListResource list = (ListResource) e.getResource();
                out.add(mapFromFhir(list));
            }

            return out;
        });
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return CommunicationDto.class.isAssignableFrom(clazz);
    }

    private ListResource mapToFhir(CommunicationDto dto) {
        ListResource list = new ListResource();
        list.setStatus(ListResource.ListStatus.CURRENT);
        list.setMode(ListResource.ListMode.WORKING);
        list.setTitle("Communication List for patientId " + dto.getPatientId());
    list.getMeta().addTag().setSystem(TENANT_TAG_SYSTEM).setCode(tenantNameStr());

        Communication comm = new Communication();
        comm.setStatus(Communication.CommunicationStatus.COMPLETED);
        comm.setSentElement(new DateTimeType(dto.getSentDate()));
        comm.setSubject(new Reference("Patient/" + dto.getPatientId()));
        comm.setPayload(Collections.singletonList(
                new Communication.CommunicationPayloadComponent().setContent(new StringType(dto.getPayload()))));

        if (!comm.hasId()) comm.setId(UUID.randomUUID().toString());

        list.addContained(comm);
        list.addEntry().setItem(new Reference("#" + comm.getIdElement().getIdPart()));

        return list;
    }

    private CommunicationDto mapFromFhir(ListResource list) {
        CommunicationDto dto = new CommunicationDto();

        if (list.hasContained()) {
            for (Resource r : list.getContained()) {
                if (r instanceof Communication comm) {
                    dto.setPayload(comm.hasPayload() ? comm.getPayloadFirstRep().getContentStringType().getValue() : null);
                    dto.setSentDate(comm.hasSent() ? comm.getSentElement().asStringValue() : null);
                    if (comm.hasSubject()) {
                        String ref = comm.getSubject().getReference();
                        if (ref != null && ref.startsWith("Patient/")) {
                            dto.setPatientId(Long.valueOf(ref.replace("Patient/", "")));
                        }
                    }
                }
            }
        }

        dto.setExternalId(list.getIdElement().getIdPart());
        return dto;
    }

    private String tenantNameStr() {
        String tenantName = RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
        return tenantName != null ? tenantName : "";
    }

    private <T> T executeWithRetry(Callable<T> op) {
        try {
            return op.call();
        } catch (FhirClientConnectionException e) {
            if (e.getStatusCode() == 401) {
                try {
                    return op.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}