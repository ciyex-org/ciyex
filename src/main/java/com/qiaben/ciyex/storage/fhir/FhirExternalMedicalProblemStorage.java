package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.MedicalProblemDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@StorageType("fhir")
@Component("fhirExternalMedicalProblemStorage")
@Slf4j
public class FhirExternalMedicalProblemStorage implements ExternalStorage<MedicalProblemDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";
    private static final Pattern TITLE_PATIENT_PATTERN =
            Pattern.compile("patientId\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    public FhirExternalMedicalProblemStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public String create(MedicalProblemDto dto) {
        String tenantName = tenantName();
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = mapToFhir(dto, tenantName);
            return client.create().resource(list).execute().getId().getIdPart();
        });
    }

    @Override
    public void update(MedicalProblemDto dto, String externalId) {
        String tenantName = tenantName();
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = mapToFhir(dto, tenantName);
            list.setId(externalId);
            client.update().resource(list).execute();
            return null;
        });
    }

    @Override
    public MedicalProblemDto get(String externalId) {
        String tenantName = tenantName();
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            ListResource list = client.read().resource(ListResource.class).withId(externalId).execute();
            MedicalProblemDto dto = mapFromFhir(list, tenantName);
            dto.setExternalId(externalId);
            Long pid = parsePatientIdFromTitle(list.getTitle());
            if (pid != null) dto.setPatientId(pid);
            return dto;
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
    public List<MedicalProblemDto> searchAll() {
        String tenantName = tenantName();
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

            return entries.stream()
                    .map(e -> (ListResource) e.getResource())
                    .map(list -> {
                        MedicalProblemDto dto = mapFromFhir(list, tenantName);
                        dto.setExternalId(list.getIdElement().getIdPart());
                        Long pid = parsePatientIdFromTitle(list.getTitle());
                        if (pid != null) dto.setPatientId(pid);
                        return dto;
                    })
                    .collect(Collectors.toList());
        });
    }

    @Override public boolean supports(Class<?> t) { return MedicalProblemDto.class.isAssignableFrom(t); }

    /* ---------------- Mapping ---------------- */

    // Pack into Basic.code.text as: title|outcome|verificationStatus|occurrence|note
    private ListResource mapToFhir(MedicalProblemDto dto, String tenantName) {
        ListResource list = new ListResource();
        list.setStatus(ListResource.ListStatus.CURRENT);
        list.setMode(ListResource.ListMode.WORKING);
        list.setTitle("Medical Problem – patientId " + dto.getPatientId());

        list.getMeta().addTag().setSystem(TENANT_TAG_SYSTEM).setCode(tenantName != null ? tenantName : "");

        if (dto.getProblemsList() != null) {
            for (MedicalProblemDto.MedicalProblemItem d : dto.getProblemsList()) {
                String text = safe(d.getTitle()) + "|" +
                        safe(d.getOutcome()) + "|" +
                        safe(d.getVerificationStatus()) + "|" +
                        safe(d.getOccurrence()) + "|" +
                        safe(d.getNote());

                Basic basic = new Basic();
                basic.setCode(new CodeableConcept().setText(text));
                if (!basic.hasId()) basic.setId(IdType.newRandomUuid());
                list.addContained(basic);
                list.addEntry().setItem(new Reference("#" + basic.getIdElement().getIdPart()));
            }
        }
        return list;
    }

    private MedicalProblemDto mapFromFhir(ListResource list, String tenantName) {
        MedicalProblemDto dto = new MedicalProblemDto();
        dto.setTenantName(tenantName);

        List<MedicalProblemDto.MedicalProblemItem> items = list.getContained().stream()
                .filter(r -> r instanceof Basic)
                .map(r -> (Basic) r)
                .map(b -> {
                    MedicalProblemDto.MedicalProblemItem x = new MedicalProblemDto.MedicalProblemItem();
                    String[] parts = (b.getCode() != null && b.getCode().getText() != null)
                            ? b.getCode().getText().split("\\|", -1) : new String[0];
                    x.setTitle(partOrNull(parts, 0));
                    x.setOutcome(partOrNull(parts, 1));
                    x.setVerificationStatus(partOrNull(parts, 2));
                    x.setOccurrence(partOrNull(parts, 3));
                    x.setNote(partOrNull(parts, 4));
                    return x;
                })
                .collect(Collectors.toList());

        dto.setProblemsList(items);
        return dto;
    }

    /* ---------------- Retry & helpers ---------------- */

    private <T> T executeWithRetry(Callable<T> op) {
        try {
            return op.call();
        } catch (FhirClientConnectionException e) {
            if (e.getStatusCode() == 401) {
                try { return op.call(); } catch (Exception ex) { throw new RuntimeException(ex); }
            }
            throw e;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String tenantName() { return RequestContext.get() != null ? RequestContext.get().getTenantName() : null; }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String partOrNull(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return null;
        String v = arr[idx]; return (v == null || v.isEmpty()) ? null : v;
    }

    private Long parsePatientIdFromTitle(String title) {
        if (title == null) return null;
        Matcher m = TITLE_PATIENT_PATTERN.matcher(title);
        if (m.find()) {
            try { return Long.parseLong(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
