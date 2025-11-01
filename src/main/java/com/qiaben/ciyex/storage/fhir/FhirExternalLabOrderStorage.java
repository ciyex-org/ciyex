package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.LabOrderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalLabOrderStorage")
@Slf4j
public class FhirExternalLabOrderStorage implements ExternalStorage<LabOrderDto> {

    private final FhirClientProvider fhirClientProvider;
    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";
    private static final Pattern TITLE_PATIENT_PATTERN = Pattern.compile("patientId\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    @Autowired
    public FhirExternalLabOrderStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public String create(LabOrderDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create LabOrder List for tenantName={} patientId={}", tenantName, dto.getPatientId());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(list).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(LabOrderDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update LabOrder List externalId={} tenantName={}", externalId, tenantName);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = mapToFhir(dto, tenantName);
            list.setId(externalId);
            client.update().resource(list).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public LabOrderDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get LabOrder List externalId={} tenantName={}", externalId, tenantName);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = client.read().resource(ListResource.class).withId(externalId).execute();
            LabOrderDto dto = mapFromFhir(list, tenantName);
            dto.setExternalId(externalId);
            Long pid = parsePatientIdFromTitle(list.getTitle());
            if (pid != null) dto.setPatientId(pid);
            log.debug("FHIR get mapped dto externalId={} tenantName={} patientId={}", externalId, tenantName, dto.getPatientId());
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR delete LabOrder List externalId={} tenantName={}", externalId, tenantName);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            client.delete().resourceById("List", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<LabOrderDto> searchAll() {
        String tenantName = tenantName();
        log.info("FHIR searchAll LabOrder Lists tenantName={}", tenantName);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            Bundle bundle = client.search()
                    .forResource(ListResource.class)
                    .where(new TokenClientParam("_tag").exactly().systemAndCode(TENANT_TAG_SYSTEM, tenantName != null ? tenantName : ""))
                    .returnBundle(Bundle.class)
                    .execute();

            final List<Bundle.BundleEntryComponent> entries = new ArrayList<>(bundle.getEntry());
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = client.loadPage().next(bundle).execute();
                entries.addAll(bundle.getEntry());
            }

            List<LabOrderDto> out = entries.stream()
                    .map(e -> (ListResource) e.getResource())
                    .map(list -> {
                        LabOrderDto dto = mapFromFhir(list, tenantName);
                        dto.setExternalId(list.getIdElement().getIdPart());
                        Long pid = parsePatientIdFromTitle(list.getTitle());
                        if (pid != null) dto.setPatientId(pid);
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("FHIR searchAll found {} LabOrder Lists for tenantName={}", out.size(), tenantName);
            return out;
        });
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return LabOrderDto.class.isAssignableFrom(entityType);
    }

    // --- helpers ---

    private <T> T executeWithRetry(Callable<T> op) {
        try {
            return op.call();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR LabOrder connection error status={} msg={}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("LabOrder 401 unauthorized; retrying once");
                try { return op.call(); } catch (Exception ex) { throw new RuntimeException(ex); }
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected FHIR LabOrder error msg={}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String partOrNull(String[] arr, int idx) { if (arr == null || idx < 0 || idx >= arr.length) return null; String v = arr[idx]; return (v == null || v.isEmpty()) ? null : v; }
    private static Long parsePatientIdFromTitle(String title) {
        if (title == null) return null;
        Matcher m = TITLE_PATIENT_PATTERN.matcher(title);
        if (m.find()) { try { return Long.parseLong(m.group(1)); } catch (NumberFormatException ignored) { } }
        return null;
    }

    /** Encode LabOrderDto into ListResource with single contained Basic using pipe-separated fields. */
    private ListResource mapToFhir(LabOrderDto d, String tenantName) {
    ListResource list = new ListResource();
    list.setStatus(ListResource.ListStatus.CURRENT);
    list.setMode(ListResource.ListMode.WORKING);
    list.setTitle("Lab Order – patientId " + d.getPatientId());

    list.getMeta()
        .addTag()
        .setSystem(TENANT_TAG_SYSTEM)
        .setCode(tenantName() != null ? tenantName() : "");

    String text = safe(d.getOrderNumber()) + "|" + safe(d.getOrderName()) + "|" + safe(d.getTestCode()) + "|" +
        safe(d.getTestDisplay()) + "|" + safe(d.getStatus()) + "|" + safe(d.getPriority()) + "|" +
        safe(d.getOrderDate()) + "|" + safe(d.getOrderDateTime()) + "|" + safe(d.getPhysicianName()) + "|" +
        safe(d.getLabName()) + "|" + safe(d.getOrderingProvider()) + "|" + safe(d.getNotes()) + "|" +
        safe(d.getDiagnosisCode()) + "|" + safe(d.getProcedureCode()) + "|" + safe(d.getSpecimenId()) + "|" +
        safe(d.getResult());

    Basic basic = new Basic();
    basic.setCode(new CodeableConcept().setText(text));
    if (!basic.hasId()) basic.setId(IdType.newRandomUuid());
    list.addContained(basic);
    list.addEntry().setItem(new Reference("#" + basic.getIdElement().getIdPart()));
    return list;
    }

    private LabOrderDto mapFromFhir(ListResource list, String tenantName) {
        LabOrderDto d = new LabOrderDto();
        final List<Resource> contained = list.getContained();
        contained.stream()
                .filter(r -> r instanceof Basic)
                .map(r -> (Basic) r)
                .findFirst()
                .ifPresent(b -> {
                    String text = b.getCode() != null ? b.getCode().getText() : null;
                    String[] parts = text != null ? text.split("\\|", -1) : new String[0];
                    d.setOrderNumber(partOrNull(parts, 0));
                    d.setOrderName(partOrNull(parts, 1));
                    d.setTestCode(partOrNull(parts, 2));
                    d.setTestDisplay(partOrNull(parts, 3));
                    d.setStatus(partOrNull(parts, 4));
                    d.setPriority(partOrNull(parts, 5));
                    d.setOrderDate(partOrNull(parts, 6));
                    d.setOrderDateTime(partOrNull(parts, 7));
                    d.setPhysicianName(partOrNull(parts, 8));
                    d.setLabName(partOrNull(parts, 9));
                    d.setOrderingProvider(partOrNull(parts, 10));
                    d.setNotes(partOrNull(parts, 11));
                    d.setDiagnosisCode(partOrNull(parts, 12));
                    d.setProcedureCode(partOrNull(parts, 13));
                    d.setSpecimenId(partOrNull(parts, 14));
                    d.setResult(partOrNull(parts, 15));
                });
        return d;
    }

    private String tenantName() {
        return RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
    }
}
