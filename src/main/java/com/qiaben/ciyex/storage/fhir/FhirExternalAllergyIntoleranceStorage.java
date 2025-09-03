// src/main/java/com/qiaben/ciyex/storage/fhir/FhirExternalAllergyIntoleranceStorage.java
package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.qiaben.ciyex.dto.AllergyIntoleranceDto;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
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

    // ----------------- ExternalStorage -----------------

    @Override
    public String create(AllergyIntoleranceDto dto) {
        Long orgId = orgId();
        log.info("FHIR create AllergyIntolerance List for orgId={} patientId={}", orgId, dto.getPatientId());
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            ListResource list = mapToFhir(dto, orgId);
            String externalId = client.create().resource(list).execute().getId().getIdPart();
            log.info("FHIR create success externalId={} orgId={}", externalId, orgId);
            return externalId;
        });
    }

    @Override
    public void update(AllergyIntoleranceDto dto, String externalId) {
        Long orgId = orgId();
        log.info("FHIR update AllergyIntolerance List externalId={} orgId={}", externalId, orgId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            ListResource list = mapToFhir(dto, orgId);
            list.setId(externalId);
            client.update().resource(list).execute();
            log.info("FHIR update success externalId={} orgId={}", externalId, orgId);
            return null;
        });
    }

    @Override
    public AllergyIntoleranceDto get(String externalId) {
        Long orgId = orgId();
        log.info("FHIR get AllergyIntolerance List externalId={} orgId={}", externalId, orgId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            ListResource list = client.read().resource(ListResource.class).withId(externalId).execute();
            AllergyIntoleranceDto dto = mapFromFhir(list, orgId);
            dto.setExternalId(externalId);

            // best-effort: infer patientId from title, e.g. "Allergy Intolerance – patientId 12"
            Long pid = parsePatientIdFromTitle(list.getTitle());
            if (pid != null) dto.setPatientId(pid);

            log.debug("FHIR get mapped dto externalId={} orgId={} patientId={}", externalId, orgId, dto.getPatientId());
            return dto;
        });
    }

    @Override
    public void delete(String externalId) {
        Long orgId = orgId();
        log.info("FHIR delete AllergyIntolerance List externalId={} orgId={}", externalId, orgId);
        executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();
            client.delete().resourceById("List", externalId).execute();
            log.info("FHIR delete success externalId={} orgId={}", externalId, orgId);
            return null;
        });
    }

    @Override
    public List<AllergyIntoleranceDto> searchAll() {
        Long orgId = orgId();
        log.info("FHIR searchAll AllergyIntolerance Lists orgId={}", orgId);
        return executeWithRetry(() -> {
            IGenericClient client = fhirClientProvider.getForCurrentOrg();

            // Search List resources tagged with this tenant
            Bundle bundle = client.search()
                    .forResource(ListResource.class)
                    .where(new TokenClientParam("_tag").exactly()
                            .systemAndCode(TENANT_TAG_SYSTEM, orgId != null ? String.valueOf(orgId) : ""))
                    .returnBundle(Bundle.class)
                    .execute();

            final List<Bundle.BundleEntryComponent> entries = new ArrayList<>(bundle.getEntry());
            // handle paging
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = client.loadPage().next(bundle).execute();
                entries.addAll(bundle.getEntry());
            }

            List<AllergyIntoleranceDto> out = entries.stream()
                    .map(e -> (ListResource) e.getResource())
                    .map(list -> {
                        AllergyIntoleranceDto dto = mapFromFhir(list, orgId);
                        dto.setExternalId(list.getIdElement().getIdPart());
                        Long pid = parsePatientIdFromTitle(list.getTitle());
                        if (pid != null) dto.setPatientId(pid);
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("FHIR searchAll found {} AllergyIntolerance Lists for orgId={}", out.size(), orgId);
            return out;
        });
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return AllergyIntoleranceDto.class.isAssignableFrom(entityType);
    }

    // ----------------- Mapping -----------------
    /**
     * Map DTO -> FHIR List with contained Basic resources, each holding
     * a compact text "name|reaction|severity|status".
     * One List per patient. Tenant is carried in Meta.tag.
     */
    private ListResource mapToFhir(AllergyIntoleranceDto dto, Long orgId) {
        ListResource list = new ListResource();
        list.setStatus(ListResource.ListStatus.CURRENT);
        list.setMode(ListResource.ListMode.WORKING);
        list.setTitle("Allergy Intolerance – patientId " + dto.getPatientId());

        // Tenant tag for multi-tenancy filtering
        list.getMeta()
                .addTag()
                .setSystem(TENANT_TAG_SYSTEM)
                .setCode(orgId != null ? orgId.toString() : "");

        // Add contained Basics + entries
        if (dto.getAllergiesList() != null) {
            for (AllergyIntoleranceDto.AllergyItem d : dto.getAllergiesList()) {
                String text = safe(d.getAllergyName()) + "|" + safe(d.getReaction()) + "|" +
                        safe(d.getSeverity()) + "|" + safe(d.getStatus());
                CodeableConcept cc = new CodeableConcept().setText(text);

                Basic basic = new Basic();
                basic.setCode(cc);

                // ensure local id for internal reference
                if (!basic.hasId()) {
                    basic.setId(IdType.newRandomUuid());
                }

                list.addContained(basic);
                list.addEntry().setItem(new Reference("#" + basic.getIdElement().getIdPart()));
            }
        }

        return list;
    }

    /**
     * Map FHIR List -> DTO by decoding contained Basic.code.text "name|reaction|severity|status".
     */
    private AllergyIntoleranceDto mapFromFhir(ListResource list, Long orgId) {
        AllergyIntoleranceDto dto = new AllergyIntoleranceDto();
        dto.setOrgId(orgId);

        final List<Resource> contained = list.getContained();
        final List<AllergyIntoleranceDto.AllergyItem> items = contained.stream()
                .filter(r -> r instanceof Basic)
                .map(r -> (Basic) r)
                .map(basic -> {
                    AllergyIntoleranceDto.AllergyItem x = new AllergyIntoleranceDto.AllergyItem();
                    String text = basic.getCode() != null ? basic.getCode().getText() : null;
                    String[] parts = text != null ? text.split("\\|", -1) : new String[0];

                    // id: we don’t map a DB id here (FHIR doesn’t know it). Keep null.
                    x.setAllergyName(partOrNull(parts, 0));
                    x.setReaction(partOrNull(parts, 1));
                    x.setSeverity(partOrNull(parts, 2));
                    x.setStatus(partOrNull(parts, 3));
                    return x;
                })
                .collect(Collectors.toList());

        dto.setAllergiesList(items);
        return dto;
    }

    // ----------------- Retry / Helpers -----------------
    private <T> T executeWithRetry(Callable<T> op) {
        Long orgId = orgId();
        try {
            return op.call();
        } catch (FhirClientConnectionException e) {
            log.error("FHIR connection error orgId={} status={} msg={}", orgId, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == 401) {
                log.warn("401 unauthorized; retrying once with fresh client orgId={}", orgId);
                try {
                    return op.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected FHIR error orgId={} msg={}", orgId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Long orgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
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
