package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.AllergyIntoleranceDto;
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
@Component("fhirExternalAllergyIntoleranceStorage")
@Slf4j
public class FhirExternalAllergyIntoleranceStorage implements ExternalStorage<AllergyIntoleranceDto> {

    private final FhirClientProvider fhirClientProvider;

    private static final String TENANT_TAG_SYSTEM = "http://ciyex.com/tenant";

    private static final Pattern TITLE_PATIENT_PATTERN =
            Pattern.compile("patientId\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    public FhirExternalAllergyIntoleranceStorage(FhirClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
        log.info("Initializing FhirExternalAllergyIntoleranceStorage");
    }

    @Override
    public String create(AllergyIntoleranceDto dto) {
        String tenantName = tenantName();
        log.info("FHIR create AllergyIntolerance List for tenantName={} patientId={}", tenantName, dto.getPatientId());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null, skipping external storage sync");
                return null;
            }
            ListResource list = mapToFhir(dto, tenantName);
            String externalId = client.create().resource(list).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} tenantName={}", externalId, tenantName);
            return externalId;
        });
    }

    @Override
    public void update(AllergyIntoleranceDto dto, String externalId) {
        String tenantName = tenantName();
        log.info("FHIR update AllergyIntolerance List externalId={} tenantName={}", externalId, tenantName);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null, skipping external storage sync");
                return null;
            }
            ListResource list = mapToFhir(dto, tenantName);
            list.setId(externalId);
            client.update().resource(list).execute();
            log.info("FHIR update success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public AllergyIntoleranceDto get(String externalId) {
        String tenantName = tenantName();
        log.info("FHIR get AllergyIntolerance List externalId={} tenantName={}", externalId, tenantName);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null, skipping external storage sync");
                return null;
            }
            ListResource list = client.read().resource(ListResource.class).withId(externalId).execute();
            AllergyIntoleranceDto dto = mapFromFhir(list, tenantName);
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
        log.info("FHIR delete AllergyIntolerance List externalId={} tenantName={}", externalId, tenantName);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null, skipping external storage sync");
                return null;
            }
            client.delete().resourceById("List", externalId).execute();
            log.info("FHIR delete success externalId={} tenantName={}", externalId, tenantName);
            return null;
        });
    }

    @Override
    public List<AllergyIntoleranceDto> searchAll() {
        String tenantName = tenantName();
        log.info("FHIR searchAll AllergyIntolerance Lists tenantName={}", tenantName);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentTenant();
            if (client == null) {
                log.warn("FHIR client is null, skipping external storage sync");
                return new ArrayList<>();
            }

        Bundle bundle = client.search()
                    .forResource(ListResource.class)
            .where(new TokenClientParam("_tag").exactly()
                .systemAndCode(TENANT_TAG_SYSTEM, tenantName != null ? tenantName : ""))
                    .returnBundle(Bundle.class)
                    .execute();

            final List<Bundle.BundleEntryComponent> entries = new ArrayList<>(bundle.getEntry());
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = client.loadPage().next(bundle).execute();
                entries.addAll(bundle.getEntry());
            }

            List<AllergyIntoleranceDto> out = entries.stream()
                    .map(e -> (ListResource) e.getResource())
                    .map(list -> {
                        AllergyIntoleranceDto dto = mapFromFhir(list, tenantName);
                        dto.setExternalId(list.getIdElement().getIdPart());
                        Long pid = parsePatientIdFromTitle(list.getTitle());
                        if (pid != null) dto.setPatientId(pid);
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("FHIR searchAll found {} AllergyIntolerance Lists for tenantName={}", out.size(), tenantName);
            return out;
        });
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return AllergyIntoleranceDto.class.isAssignableFrom(entityType);
    }

    /** Map DTO -> FHIR List with contained Basics; code.text = name|reaction|severity|status|startDate|endDate|comments */
    private ListResource mapToFhir(AllergyIntoleranceDto dto, String tenantName) {
        ListResource list = new ListResource();
        list.setStatus(ListResource.ListStatus.CURRENT);
        list.setMode(ListResource.ListMode.WORKING);
        list.setTitle("Allergy Intolerance – patientId " + dto.getPatientId());

    list.getMeta()
                .addTag()
                .setSystem(TENANT_TAG_SYSTEM)
        .setCode(tenantName != null ? tenantName : "");

        if (dto.getAllergiesList() != null) {
            for (AllergyIntoleranceDto.AllergyItem d : dto.getAllergiesList()) {
                String text = safe(d.getAllergyName()) + "|" + safe(d.getReaction()) + "|" +
                        safe(d.getSeverity()) + "|" + safe(d.getStatus()) + "|" +
                        safe(d.getStartDate()) + "|" + safe(d.getEndDate()) + "|" +
                        safe(d.getComments());                    // 7th part
                CodeableConcept cc = new CodeableConcept().setText(text);

                Basic basic = new Basic();
                basic.setCode(cc);
                if (!basic.hasId()) {
                    basic.setId(IdType.newRandomUuid());
                }

                list.addContained(basic);
                list.addEntry().setItem(new Reference("#" + basic.getIdElement().getIdPart()));
            }
        }
        return list;
    }

    /** Map FHIR List -> DTO; supports 4-part (legacy), 6-part (dates), and 7-part (with comments) encodings. */
    private AllergyIntoleranceDto mapFromFhir(ListResource list, String tenantName) {
        AllergyIntoleranceDto dto = new AllergyIntoleranceDto();
        // orgId deprecated; tenantName available via RequestContext

        final List<Resource> contained = list.getContained();
        final List<AllergyIntoleranceDto.AllergyItem> items = contained.stream()
                .filter(r -> r instanceof Basic)
                .map(r -> (Basic) r)
                .map(basic -> {
                    AllergyIntoleranceDto.AllergyItem x = new AllergyIntoleranceDto.AllergyItem();
                    String text = basic.getCode() != null ? basic.getCode().getText() : null;
                    String[] parts = text != null ? text.split("\\|", -1) : new String[0];

                    x.setAllergyName(partOrNull(parts, 0));
                    x.setReaction(partOrNull(parts, 1));
                    x.setSeverity(partOrNull(parts, 2));
                    x.setStatus(partOrNull(parts, 3));
                    x.setStartDate(partOrNull(parts, 4)); // may be null for legacy
                    x.setEndDate(partOrNull(parts, 5));   // may be null for legacy
                    x.setComments(partOrNull(parts, 6));  // may be null for legacy
                    return x;
                })
                .collect(Collectors.toList());

        dto.setAllergiesList(items);
        return dto;
    }

    private <T> T executeWithRetry(Callable<T> op) {
        String tenantName = tenantName();
        try {
            return op.call();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR connection error tenantName={} status={} msg={}", tenantName, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("401 unauthorized; retrying once with fresh client tenantName={}", tenantName);
                try {
                    return op.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected FHIR error tenantName={} msg={}", tenantName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    private String tenantName() {
        return RequestContext.get() != null ? RequestContext.get().getTenantName() : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String partOrNull(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return null;
        String v = arr[idx];
        return (v == null || v.isEmpty()) ? null : v;
    }

    private static Long parsePatientIdFromTitle(String title) {
        if (title == null) return null;
        Matcher m = TITLE_PATIENT_PATTERN.matcher(title);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }
}
