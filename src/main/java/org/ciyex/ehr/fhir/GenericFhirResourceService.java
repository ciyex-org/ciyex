package org.ciyex.ehr.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.service.AppointmentEncounterService;
import org.ciyex.ehr.service.PracticeContextService;
import org.ciyex.ehr.settings.service.UiColorConfigService;
import org.ciyex.ehr.tabconfig.entity.TabFieldConfig;
import org.ciyex.ehr.tabconfig.service.TabFieldConfigService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.hl7.fhir.r4.model.*;

import java.util.*;

/**
 * Generic FHIR resource service that uses tab_field_config to dynamically
 * handle CRUD operations for any FHIR resource type.
 *
 * Flow: TabFieldConfig → fhirResources (type + patientSearchParam) → FhirClientService
 * Mapping: field_config.fhirMapping paths → FhirPathMapper → HAPI FHIR Resource
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenericFhirResourceService {

    private static final Set<String> GLOBAL_FHIR_TYPES = Set.of(
            "CodeSystem", "ValueSet", "NamingSystem", "ConceptMap", "StructureDefinition");

    private final FhirClientService fhirClient;
    private final FhirPartitionService fhirPartitionService;
    private final TabFieldConfigService tabFieldConfigService;
    private final FhirPathMapper pathMapper;
    private final PracticeContextService practiceContextService;
    private final ObjectMapper objectMapper;
    private final UiColorConfigService colorService;
    private final FhirScopeEnforcer scopeEnforcer;
    private final FhirFormDataRepository formDataRepository;

    /**
     * List all resources for a tab without patient scope (settings pages).
     * Uses the first (primary) resource type for listing.
     */
    public Map<String, Object> listAll(String tabKey, int page, int size) {
        return listAll(tabKey, page, size, null, null, null);
    }

    /**
     * List all resources for a tab without patient scope, with optional sort.
     * sortParam uses FHIR _sort syntax, e.g. "-date" for descending by date.
     */
    public Map<String, Object> listAll(String tabKey, int page, int size, String sortParam) {
        return listAll(tabKey, page, size, sortParam, null, null);
    }

    /**
     * List all resources for a tab without patient scope, with optional sort and date range.
     * dateFrom/dateTo use yyyy-MM-dd format for FHIR date search parameter.
     */
    public Map<String, Object> listAll(String tabKey, int page, int size, String sortParam, String dateFrom, String dateTo) {
        scopeEnforcer.requireRead(tabKey);
        TabFieldConfig config = getConfig(tabKey);
        List<FhirPathMapper.FieldMapping> mappings = parseMappings(config);
        List<ResourceMeta> resources = parseResourceMeta(config);
        String orgAlias = practiceContextService.getPracticeId();

        if (resources.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("content", List.of());
            empty.put("page", page);
            empty.put("size", size);
            empty.put("totalElements", 0);
            empty.put("totalPages", 0);
            empty.put("hasNext", false);
            return empty;
        }

        // Use the first (primary) resource type for listing
        ResourceMeta primary = resources.get(0);
        Class<? extends Resource> clazz = pathMapper.resolveResourceClass(primary.type);

        var search = fhirClient.getClient(orgAlias).search()
                .forResource(clazz);
        // Apply search filters from fhirResources config (e.g. type=prov for practice orgs)
        for (Map.Entry<String, String> param : primary.searchParams.entrySet()) {
            search = search.where(new ca.uhn.fhir.rest.gclient.TokenClientParam(param.getKey()).exactly().code(param.getValue()));
        }
        // Apply date range filter if provided (FHIR date search param: ge=from, le=to)
        if (dateFrom != null && !dateFrom.isBlank()) {
            search = search.where(new DateClientParam("date").afterOrEquals().day(dateFrom));
        }
        if (dateTo != null && !dateTo.isBlank()) {
            search = search.where(new DateClientParam("date").beforeOrEquals().day(dateTo));
        }
        if (sortParam != null && !sortParam.isEmpty()) {
            boolean descending = sortParam.startsWith("-");
            String sortField = descending ? sortParam.substring(1) : sortParam;
            search = descending
                    ? search.sort().descending(sortField)
                    : search.sort().ascending(sortField);
        }
        // Fetch a large initial page from FHIR to minimize round-trips
        Bundle bundle = search
                .count(500)
                .totalMode(ca.uhn.fhir.rest.api.SearchTotalModeEnum.ACCURATE)
                .returnBundle(Bundle.class)
                .execute();

        log.info("listAll [{}]: searchParams={}, total={}, entries={}",
                tabKey, primary.searchParams, bundle.getTotal(),
                bundle.getEntry() != null ? bundle.getEntry().size() : 0);

        // Fallback: if token search returns 0 results and there are type searchParams,
        // try a :text search which finds legacy resources where the type Coding was stored
        // as text only (due to a prior bug that stripped systemless Codings).
        if (bundle.getTotal() == 0 && (bundle.getEntry() == null || bundle.getEntry().isEmpty())
                && !primary.searchParams.isEmpty()) {
            try {
                var fallbackSearch = fhirClient.getClient(orgAlias).search().forResource(clazz);
                boolean first = true;
                for (Map.Entry<String, String> param : primary.searchParams.entrySet()) {
                    var criterion = new StringClientParam(param.getKey() + ":text").matchesExactly().value(param.getValue());
                    fallbackSearch = first ? fallbackSearch.where(criterion) : fallbackSearch.and(criterion);
                    first = false;
                }
                Bundle fallbackBundle = fallbackSearch.count(500)
                        .totalMode(ca.uhn.fhir.rest.api.SearchTotalModeEnum.ACCURATE)
                        .returnBundle(Bundle.class).execute();
                if (fallbackBundle.getTotal() > 0 || (fallbackBundle.getEntry() != null && !fallbackBundle.getEntry().isEmpty())) {
                    bundle = fallbackBundle;
                    log.info("listAll [{}]: using :text fallback, total={}", tabKey, bundle.getTotal());
                }
            } catch (Exception e) {
                log.debug("listAll [{}]: :text fallback search failed: {}", tabKey, e.getMessage());
            }
        }

        // Collect ALL entries across all FHIR pages (server may paginate even with count=500)
        List<Bundle.BundleEntryComponent> allBundleEntries = new ArrayList<>(
                bundle.getEntry() != null ? bundle.getEntry() : List.of());
        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = fhirClient.getClient(orgAlias)
                    .loadPage()
                    .next(bundle)
                    .execute();
            if (bundle.getEntry() != null) {
                allBundleEntries.addAll(bundle.getEntry());
            }
        }

        // Filter out soft-deleted resources (tagged with "deleted")
        allBundleEntries.removeIf(entry -> {
            if (entry.getResource() instanceof Resource r && r.getMeta() != null) {
                return r.getMeta().getTag().stream()
                        .anyMatch(t -> "deleted".equals(t.getCode()) && "http://ciyex.org/fhir/tags".equals(t.getSystem()));
            }
            return false;
        });

        int totalCount = allBundleEntries.size();
        log.info("listAll [{}]: fetched {} total entries across all FHIR pages (after filtering deleted)", tabKey, totalCount);

        // Apply API-level pagination on the complete result set
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Bundle.BundleEntryComponent> pageEntries = allBundleEntries.subList(fromIndex, toIndex);

        @SuppressWarnings("unchecked")
        Class<Resource> resourceClass = (Class<Resource>) clazz;
        List<Resource> entries = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : pageEntries) {
            if (entry.getResource() != null && resourceClass.isInstance(entry.getResource())) {
                entries.add(resourceClass.cast(entry.getResource()));
            }
        }

        boolean hasNext = toIndex < totalCount;

        // Check if this resource type has field-level mappings
        List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                .filter(m -> primary.type.equals(m.resource()))
                .toList();
        boolean isRawData = resourceMappings.isEmpty();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Resource resource : entries) {
            Map<String, Object> formData;
            if (isRawData) {
                formData = extractFormDataExtension(resource);
                if (formData == null) formData = new LinkedHashMap<>();
            } else {
                formData = pathMapper.fromFhirResource(resource, mappings);
                // Always merge form-data extension for any fields missing or blank from FHIR mapping
                mergeExtData(formData, extractFormDataExtension(resource), resourceMappings);
            }
            formData.put("id", resource.getIdElement().getIdPart());
            formData.put("fhirId", resource.getIdElement().getIdPart());
            formData.put("_resourceType", primary.type);
            if (resource.getMeta() != null && resource.getMeta().getLastUpdated() != null) {
                formData.put("_lastUpdated", resource.getMeta().getLastUpdated().toInstant().toString());
            }
            // Fallback: extract participant references from Appointment resources
            // when FHIRPath type-coded extraction returns nothing (legacy data without type coding)
            if (resource instanceof Appointment appt && appt.hasParticipant()) {
                extractAppointmentParticipantFallbacks(appt, formData);
            }
            // Extract beneficiary patient ID from Coverage resources (needed for reports)
            if (resource instanceof org.hl7.fhir.r4.model.Coverage cov && cov.hasBeneficiary()) {
                String benRef = cov.getBeneficiary().getReference(); // e.g. "Patient/123"
                if (benRef != null && benRef.contains("Patient/")) {
                    formData.putIfAbsent("patientId", benRef.split("Patient/")[1]);
                    formData.putIfAbsent("beneficiaryId", benRef.split("Patient/")[1]);
                }
                formData.putIfAbsent("beneficiary", benRef);
            }
            results.add(formData);
        }

        // Resolve FHIR references (e.g. "Practitioner/1134" → "David Thompson")
        resolveReferences(results, mappings, orgAlias);

        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / size) : 0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", results);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalCount);
        response.put("totalPages", totalPages);
        response.put("hasNext", hasNext);
        return response;
    }

    /**
     * List resources for a tab + patient with pagination.
     * Uses FHIR _count and next-link paging (no _offset).
     */
    public Map<String, Object> list(String tabKey, Long patientId, int page, int size) {
        return list(tabKey, patientId, page, size, null);
    }

    public Map<String, Object> list(String tabKey, Long patientId, int page, int size, String encounterRef) {
        scopeEnforcer.requireRead(tabKey);
        TabFieldConfig config = getConfig(tabKey);
        List<FhirPathMapper.FieldMapping> mappings = parseMappings(config);
        List<ResourceMeta> resources = parseResourceMeta(config);

        List<Map<String, Object>> results = new ArrayList<>();
        String orgAlias = practiceContextService.getPracticeId();
        int totalCount = 0;
        boolean hasNext = false;

        for (ResourceMeta meta : resources) {
            Class<? extends Resource> clazz = pathMapper.resolveResourceClass(meta.type);

            // Non-patient-scoped resource types (Organization, Location, Practitioner, etc.)
            boolean isNonPatientScoped = Patient.class.isAssignableFrom(clazz)
                    || org.hl7.fhir.r4.model.Organization.class.isAssignableFrom(clazz)
                    || org.hl7.fhir.r4.model.Location.class.isAssignableFrom(clazz)
                    || org.hl7.fhir.r4.model.Practitioner.class.isAssignableFrom(clazz)
                    || org.hl7.fhir.r4.model.PractitionerRole.class.isAssignableFrom(clazz);
            if (meta.patientSearchParam == null || meta.patientSearchParam.isBlank()
                    || isNonPatientScoped) {
                // For non-patient-scoped resources, read by ID (they don't have patient references)
                // First try to read by FHIR ID matching the patient ID (with "p-" prefix for non-Patient types)
                boolean isPatientType = Patient.class.isAssignableFrom(clazz);
                String lookupId = isPatientType ? String.valueOf(patientId) : "p-" + patientId;
                var opt = fhirClient.readOptional(clazz, lookupId, orgAlias);
                // Fallback: try legacy numeric-only ID for backwards compatibility
                if (opt.isEmpty() && !isPatientType) {
                    opt = fhirClient.readOptional(clazz, String.valueOf(patientId), orgAlias);
                }
                if (opt.isEmpty() && isPatientType) {
                    // Fallback: search Patient by identifier (EHR patient ID stored as identifier)
                    try {
                        Bundle searchBundle = fhirClient.getClient(orgAlias).search()
                                .forResource(Patient.class)
                                .where(new TokenClientParam("identifier").exactly().code(String.valueOf(patientId)))
                                .count(1)
                                .returnBundle(Bundle.class)
                                .execute();
                        List<Resource> found = fhirClient.extractResources(searchBundle, (Class<Resource>) (Class<?>) Patient.class);
                        if (!found.isEmpty()) {
                            opt = java.util.Optional.of(found.get(0));
                        }
                    } catch (Exception e) {
                        log.debug("Identifier fallback search failed for patient {}: {}", patientId, e.getMessage());
                    }
                }
                if (opt.isPresent()) {
                    Resource resource = (Resource) opt.get();
                    List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                            .filter(m -> meta.type.equals(m.resource()))
                            .toList();
                    Map<String, Object> formData;
                    if (resourceMappings.isEmpty()) {
                        formData = extractFormDataExtension(resource);
                        if (formData == null) formData = new LinkedHashMap<>();
                    } else {
                        formData = pathMapper.fromFhirResource(resource, mappings);
                        mergeExtData(formData, extractFormDataExtension(resource), resourceMappings);
                    }
                    formData.put("id", resource.getIdElement().getIdPart());
                    formData.put("fhirId", resource.getIdElement().getIdPart());
                    formData.put("_resourceType", meta.type);
                    if (resource.getMeta() != null && resource.getMeta().getLastUpdated() != null) {
                        formData.put("_lastUpdated", resource.getMeta().getLastUpdated().toString());
                    }
                    results.add(formData);
                    totalCount++;
                }
                continue;
            }

            // Build search query
            var search = fhirClient.getClient(orgAlias).search()
                    .forResource(clazz)
                    .where(new ReferenceClientParam(meta.patientSearchParam)
                            .hasId(String.valueOf(patientId)));

            // If encounterRef provided, also filter by encounter reference
            if (encounterRef != null && !encounterRef.isBlank()) {
                search = search.and(new ReferenceClientParam("encounter")
                        .hasId(encounterRef));
            }

            Bundle bundle = search.count(size)
                    .totalMode(ca.uhn.fhir.rest.api.SearchTotalModeEnum.ACCURATE)
                    .returnBundle(Bundle.class)
                    .execute();
            // Skip to requested page via next links
            for (int i = 0; i < page && bundle.getLink(Bundle.LINK_NEXT) != null; i++) {
                bundle = fhirClient.getClient(orgAlias).loadPage()
                        .next(bundle)
                        .execute();
            }

            if (bundle.getTotal() > 0) {
                totalCount += bundle.getTotal();
            }
            if (bundle.getLink(Bundle.LINK_NEXT) != null) {
                hasNext = true;
            }

            // Check if this resource type has field-level mappings
            List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                    .filter(m -> meta.type.equals(m.resource()))
                    .toList();
            boolean isRawData = resourceMappings.isEmpty();

            @SuppressWarnings("unchecked")
            Class<Resource> resourceClass = (Class<Resource>) clazz;
            List<Resource> entries = fhirClient.extractResources(bundle, resourceClass);
            for (Resource resource : entries) {
                Map<String, Object> formData;
                if (isRawData) {
                    // No field-level mappings — extract raw data from extension
                    formData = extractFormDataExtension(resource);
                    if (formData == null) formData = new LinkedHashMap<>();
                } else {
                    formData = pathMapper.fromFhirResource(resource, mappings);
                    // Always merge form-data extension for any fields missing or blank from FHIR mapping
                    mergeExtData(formData, extractFormDataExtension(resource), resourceMappings);
                }
                formData.put("id", resource.getIdElement().getIdPart());
                formData.put("fhirId", resource.getIdElement().getIdPart());
                formData.put("_resourceType", meta.type);
                if (resource.getMeta() != null && resource.getMeta().getLastUpdated() != null) {
                    formData.put("_lastUpdated", resource.getMeta().getLastUpdated().toString());
                }
                if (resource instanceof Appointment appt && appt.hasParticipant()) {
                    extractAppointmentParticipantFallbacks(appt, formData);
                }
                results.add(formData);
            }
        }

        // Resolve FHIR references (e.g. "Practitioner/1134" → "David Thompson")
        resolveReferences(results, mappings, orgAlias);

        // Detect single-record mode: any resource with empty patientSearchParam
        boolean singleRecord = resources.stream()
                .anyMatch(m -> m.patientSearchParam == null || m.patientSearchParam.isBlank());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", results);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalCount);
        int calcPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
        response.put("totalPages", hasNext ? Math.max(calcPages, page + 2) : Math.max(calcPages, page + 1));
        response.put("hasNext", hasNext);
        response.put("singleRecord", singleRecord);
        return response;
    }

    /**
     * Get a single resource by ID.
     */
    public Map<String, Object> get(String tabKey, Long patientId, String resourceId) {
        scopeEnforcer.requireRead(tabKey);
        TabFieldConfig config = getConfig(tabKey);
        List<FhirPathMapper.FieldMapping> mappings = parseMappings(config);
        List<ResourceMeta> resources = parseResourceMeta(config);
        String orgAlias = practiceContextService.getPracticeId();

        // Try each resource type until we find a match
        for (ResourceMeta meta : resources) {
            Class<? extends Resource> clazz = pathMapper.resolveResourceClass(meta.type);
            var opt = fhirClient.readOptional(clazz, resourceId, orgAlias);
            if (opt.isPresent()) {
                Resource resource = (Resource) opt.get();

                // Check if this resource type has field-level mappings
                List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                        .filter(m -> meta.type.equals(m.resource()))
                        .toList();

                Map<String, Object> formData;
                if (resourceMappings.isEmpty()) {
                    // No field-level mappings — extract raw data from extension
                    formData = extractFormDataExtension(resource);
                    if (formData == null) formData = new LinkedHashMap<>();
                } else {
                    formData = pathMapper.fromFhirResource(resource, mappings);
                    // Always merge form-data extension for any fields missing or blank from FHIR mapping
                    mergeExtData(formData, extractFormDataExtension(resource), resourceMappings);
                }
                formData.put("id", resource.getIdElement().getIdPart());
                formData.put("fhirId", resource.getIdElement().getIdPart());
                formData.put("_resourceType", meta.type);
                if (resource.getMeta() != null && resource.getMeta().getLastUpdated() != null) {
                    formData.put("_lastUpdated", resource.getMeta().getLastUpdated().toString());
                }
                // Resolve FHIR references for single record
                resolveReferences(List.of(formData), mappings, orgAlias);
                return formData;
            }
        }

        return null;
    }

    /**
     * Create a new resource from form data.
     */
    public Map<String, Object> create(String tabKey, Long patientId, Map<String, Object> formData) {
        return create(tabKey, patientId, formData, null);
    }

    /**
     * Dry-run: build the FHIR resource but don't save it. Returns the JSON for debugging.
     */
    public Map<String, Object> dryRun(String tabKey, Long patientId, Map<String, Object> formData) {
        TabFieldConfig config = getConfig(tabKey);
        List<FhirPathMapper.FieldMapping> mappings = parseMappings(config);
        List<ResourceMeta> resources = parseResourceMeta(config);

        Map<String, Object> result = new LinkedHashMap<>();
        for (ResourceMeta meta : resources) {
            List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                    .filter(m -> meta.type.equals(m.resource())).toList();
            if (resourceMappings.isEmpty()) continue;
            boolean hasData = resourceMappings.stream()
                    .anyMatch(m -> { Object val = formData.get(m.fieldKey()); return val != null && !(val instanceof String s && s.isBlank()); });
            if (!hasData) continue;

            Resource resource = pathMapper.toFhirResource(meta.type, formData, mappings);
            ensureRequiredDefaults(resource, formData, patientId);
            storeFormDataExtension(resource, formData, resourceMappings);
            applySearchParamsToResource(resource, meta.searchParams);
            if (patientId != null && meta.patientSearchParam != null && !meta.patientSearchParam.isBlank()) {
                setPatientReference(resource, meta.patientSearchParam, patientId);
            }
            stripCustomExtensions(resource);
            fixSystemlessCodingsRecursive(resource);

            // Direct AllergyIntolerance fix
            if (resource instanceof AllergyIntolerance ai) {
                String clinCode = "active";
                if (ai.hasClinicalStatus() && ai.getClinicalStatus().hasCoding()) {
                    clinCode = ai.getClinicalStatus().getCodingFirstRep().getCode();
                    if (clinCode == null || clinCode.isBlank()) clinCode = "active";
                }
                ai.setClinicalStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                                .setCode(clinCode)));
                String verCode = "confirmed";
                if (ai.hasVerificationStatus() && ai.getVerificationStatus().hasCoding()) {
                    verCode = ai.getVerificationStatus().getCodingFirstRep().getCode();
                    if (verCode == null || verCode.isBlank()) verCode = "confirmed";
                }
                ai.setVerificationStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                                .setCode(verCode)));
            }

            try {
                var ctx = ca.uhn.fhir.context.FhirContext.forR4Cached();
                String json = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
                result.put("resourceType", meta.type);
                result.put("json", json);
                result.put("jsonLength", json.length());
            } catch (Exception e) {
                result.put("serializationError", e.getMessage());
            }
        }
        return result;
    }

    public Map<String, Object> create(String tabKey, Long patientId, Map<String, Object> formData, String encounterRef) {
        scopeEnforcer.requireWrite(tabKey);
        TabFieldConfig config = getConfig(tabKey);
        List<FhirPathMapper.FieldMapping> mappings = parseMappings(config);
        List<ResourceMeta> resources = parseResourceMeta(config);
        String orgAlias = practiceContextService.getPracticeId();

        Map<String, Object> result = new LinkedHashMap<>();

        for (ResourceMeta meta : resources) {
            // Filter mappings for this resource type
            List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                    .filter(m -> meta.type.equals(m.resource()))
                    .toList();

            Resource resource;

            if (resourceMappings.isEmpty()) {
                // No field-level mappings — only create raw data resource if this is the sole/primary resource type.
                // Skip secondary resource types with no mappings (e.g., RelatedPerson in demographics tab)
                if (resources.size() > 1 && resources.indexOf(meta) > 0) {
                    continue;
                }
                // This is used by encounter forms and other "raw data" forms
                resource = createRawDataResource(meta.type, formData);
            } else {
                // Normal path: field-level FHIR mapping
                boolean hasData = resourceMappings.stream()
                        .anyMatch(m -> {
                            Object val = formData.get(m.fieldKey());
                            return val != null && !(val instanceof String s && s.isBlank());
                        });
                if (!hasData) continue;

                // Resolve reference field values that look like names to FHIR IDs
                resolveReferenceNameValues(formData, resourceMappings, orgAlias);

                resource = pathMapper.toFhirResource(meta.type, formData, mappings);
                // Ensure required FHIR fields have valid defaults
                ensureRequiredDefaults(resource, formData, patientId);
                // Also store form data as extension for fallback extraction
                // Pass mappings so FHIR-mapped fields are excluded (prevents data corruption)
                storeFormDataExtension(resource, formData, resourceMappings);
            }

            // Apply searchParams as properties on the resource (e.g., Organization.type = "prov")
            applySearchParamsToResource(resource, meta.searchParams);

            // Set patient reference if applicable (only for patient-scoped resources)
            if (patientId != null && meta.patientSearchParam != null && !meta.patientSearchParam.isBlank()) {
                setPatientReference(resource, meta.patientSearchParam, patientId);
            }

            // Set encounter reference if provided
            if (encounterRef != null && !encounterRef.isBlank()) {
                setEncounterReference(resource, encounterRef);
            }

            String createAlias = GLOBAL_FHIR_TYPES.contains(meta.type) ? "" : orgAlias;

            // For single-record tabs (no patientSearchParam, non-Patient type), use patientId as
            // the resource ID so the read-by-ID lookup in list() can find it later.
            boolean isPatientType = Patient.class.isAssignableFrom(pathMapper.resolveResourceClass(meta.type));
            boolean singleRecordTab = patientId != null
                    && (meta.patientSearchParam == null || meta.patientSearchParam.isBlank())
                    && !isPatientType;
            if (singleRecordTab) {
                // Prefix with "p-" to ensure non-numeric ID (HAPI FHIR rejects purely numeric IDs)
                resource.setId(new IdType(meta.type, "p-" + patientId));
            }
            // For Patient type in demographics, set ID to patientId so list() can find it by read-by-ID
            if (isPatientType && patientId != null) {
                resource.setId(new IdType(meta.type, String.valueOf(patientId)));
            }

            // Strip custom extensions before sending to FHIR server (it rejects unknown URLs)
            stripCustomExtensions(resource);

            // Fix any remaining Codings without system (FHIR server rejects these)
            fixSystemlessCodingsRecursive(resource);

            // Direct typed fix for AllergyIntolerance — rebuild clinicalStatus and verificationStatus from scratch
            if (resource instanceof AllergyIntolerance ai) {
                // Clear and rebuild clinicalStatus
                String clinCode = "active";
                if (ai.hasClinicalStatus() && ai.getClinicalStatus().hasCoding()) {
                    clinCode = ai.getClinicalStatus().getCodingFirstRep().getCode();
                    if (clinCode == null || clinCode.isBlank()) clinCode = "active";
                }
                ai.setClinicalStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                                .setCode(clinCode)));
                // Clear and rebuild verificationStatus
                String verCode = "confirmed";
                if (ai.hasVerificationStatus() && ai.getVerificationStatus().hasCoding()) {
                    verCode = ai.getVerificationStatus().getCodingFirstRep().getCode();
                    if (verCode == null || verCode.isBlank()) verCode = "confirmed";
                }
                ai.setVerificationStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                                .setCode(verCode)));
                log.info("AllergyIntolerance direct fix: clinicalStatus={}/{}, verificationStatus={}/{}",
                        ai.getClinicalStatus().getCodingFirstRep().getSystem(),
                        ai.getClinicalStatus().getCodingFirstRep().getCode(),
                        ai.getVerificationStatus().getCodingFirstRep().getSystem(),
                        ai.getVerificationStatus().getCodingFirstRep().getCode());
            }

            // Use update (upsert) when we have a known ID (existing patient/single-record),
            // but use create when patientId is null (brand-new patient) to avoid HAPI-1396
            boolean useUpdate = singleRecordTab || (isPatientType && patientId != null);
            MethodOutcome outcome;
            try {
                outcome = executeFhirCreateOrUpdate(resource, createAlias, useUpdate, meta.type, tabKey);
            } catch (Exception e) {
                // Auto-create FHIR partition if it doesn't exist (new org first operation)
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("partition") || msg.contains("Partition")
                        || msg.contains("HAPI-0302") || msg.contains("Unknown partition")
                        || msg.contains("HTTP 400") || msg.contains("HTTP 404")) {
                    log.warn("FHIR operation failed — attempting to auto-create partition '{}': {}", createAlias, msg);
                    try {
                        fhirPartitionService.createPartition(createAlias, createAlias);
                        log.info("Auto-created FHIR partition '{}', retrying operation", createAlias);
                        outcome = executeFhirCreateOrUpdate(resource, createAlias, useUpdate, meta.type, tabKey);
                    } catch (Exception retryEx) {
                        log.error("Retry after partition creation also failed: {}", retryEx.getMessage());
                        throw e; // throw original
                    }
                } else {
                    throw e;
                }
            }
            String fhirId = outcome.getId().getIdPart();

            result.put("id", fhirId);
            result.put("fhirId", fhirId);
            result.put("_resourceType", meta.type);

            // Store form data in local DB (not on FHIR resource)
            storeFormDataLocally(meta.type, fhirId, orgAlias, formData, resourceMappings);

            log.info("Created {} with FHIR ID: {} for tab: {}", meta.type, fhirId, tabKey);

            // Auto-assign a calendar color for new providers and locations
            try {
                if ("providers".equals(tabKey) && "Practitioner".equals(meta.type)) {
                    String label = buildLabel(formData, "identification.firstName", "identification.lastName");
                    colorService.autoAssignColorIfMissing(orgAlias, "provider", fhirId, label);
                } else if ("facilities".equals(tabKey) && "Location".equals(meta.type)) {
                    String label = String.valueOf(formData.getOrDefault("name", ""));
                    colorService.autoAssignColorIfMissing(orgAlias, "location", fhirId, label);
                }
            } catch (Exception e) {
                log.warn("Failed to auto-assign color for {} {}: {}", meta.type, fhirId, e.getMessage());
            }
        }

        result.putAll(formData);
        return result;
    }

    private String buildLabel(Map<String, Object> formData, String... keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            Object val = formData.get(key);
            if (val != null && !val.toString().isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(val);
            }
        }
        return sb.toString();
    }

    /**
     * Execute a FHIR create or update with fallback from update→create on HAPI-1396.
     */
    private MethodOutcome executeFhirCreateOrUpdate(Resource resource, String alias,
                                                     boolean useUpdate, String type, String tabKey) {
        log.info("FHIR op: type={}, tab={}, useUpdate={}, hasId={}, id={}, alias={}",
                type, tabKey, useUpdate, resource.hasId(), resource.getId(), alias);
        if (useUpdate) {
            try {
                return fhirClient.update(resource, alias);
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("HAPI-1396")
                        || e.getMessage().contains("No ID supplied"))) {
                    log.warn("FHIR update failed for {} ({}), falling back to create: {}", type, tabKey, e.getMessage());
                    resource.setId((String) null);
                    return fhirClient.create(resource, alias);
                }
                throw e;
            }
        }
        // For create: ensure no stale ID (some mappers may set it)
        if (resource.hasId()) {
            log.warn("Resource {} has unexpected ID '{}' for create — clearing it", type, resource.getId());
            resource.setId((String) null);
        }
        return fhirClient.create(resource, alias);
    }

    /**
     * Update an existing resource.
     */
    public Map<String, Object> update(String tabKey, Long patientId, String resourceId, Map<String, Object> formData) {
        scopeEnforcer.requireWrite(tabKey);
        TabFieldConfig config = getConfig(tabKey);
        List<FhirPathMapper.FieldMapping> mappings = parseMappings(config);
        List<ResourceMeta> resources = parseResourceMeta(config);
        String orgAlias = practiceContextService.getPracticeId();

        for (ResourceMeta meta : resources) {
            List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                    .filter(m -> meta.type.equals(m.resource()))
                    .toList();
            boolean isRawData = resourceMappings.isEmpty();

            Class<? extends Resource> clazz = pathMapper.resolveResourceClass(meta.type);
            String typeAlias = GLOBAL_FHIR_TYPES.contains(meta.type) ? "" : orgAlias;
            var opt = fhirClient.readOptional(clazz, resourceId, typeAlias);
            if (opt.isPresent()) {
                Resource existing = (Resource) opt.get();
                Resource updated;

                if (isRawData) {
                    // No field-level mappings — update the JSON extension on existing resource
                    updated = existing;
                    storeFormDataExtension(updated, formData);
                } else {
                    // Resolve reference field values that look like names to FHIR IDs
                    resolveReferenceNameValues(formData, resourceMappings, orgAlias);
                    // Merge form data onto existing resource (preserves fields not in form)
                    updated = pathMapper.applyFormData(existing, meta.type, formData, resourceMappings);
                    ensureRequiredDefaults(updated, formData, patientId);
                    // Also store form data as extension for fallback extraction
                    // Pass mappings so FHIR-mapped fields are excluded (prevents data corruption)
                    storeFormDataExtension(updated, formData, resourceMappings);
                }
                updated.setId(resourceId);

                // Preserve patient reference (only for patient-scoped resources)
                if (patientId != null && meta.patientSearchParam != null && !meta.patientSearchParam.isBlank()) {
                    setPatientReference(updated, meta.patientSearchParam, patientId);
                }

                // Strip references to deleted resources (e.g., Encounter.appointment)
                stripDeletedReferences(updated, orgAlias);

                // Re-apply searchParams (e.g. Organization.type=dept) so existing records
                // that were stored without the type coding get it re-added on update
                applySearchParamsToResource(updated, meta.searchParams);

                // Strip custom extensions before sending to FHIR server
                stripCustomExtensions(updated);

                // Fix any remaining Codings without system (FHIR server rejects these)
                fixSystemlessCodingsRecursive(updated);

                // Direct typed fix for AllergyIntolerance — rebuild clinicalStatus and verificationStatus from scratch
                if (updated instanceof AllergyIntolerance ai) {
                    String clinCode = "active";
                    if (ai.hasClinicalStatus() && ai.getClinicalStatus().hasCoding()) {
                        clinCode = ai.getClinicalStatus().getCodingFirstRep().getCode();
                        if (clinCode == null || clinCode.isBlank()) clinCode = "active";
                    }
                    ai.setClinicalStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                            .addCoding(new Coding()
                                    .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                                    .setCode(clinCode)));
                    String verCode = "confirmed";
                    if (ai.hasVerificationStatus() && ai.getVerificationStatus().hasCoding()) {
                        verCode = ai.getVerificationStatus().getCodingFirstRep().getCode();
                        if (verCode == null || verCode.isBlank()) verCode = "confirmed";
                    }
                    ai.setVerificationStatus(new org.hl7.fhir.r4.model.CodeableConcept()
                            .addCoding(new Coding()
                                    .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                                    .setCode(verCode)));
                }

                fhirClient.update(updated, typeAlias);

                // Store form data in local DB
                storeFormDataLocally(meta.type, resourceId, orgAlias, formData, resourceMappings);

                Map<String, Object> result;
                if (isRawData) {
                    result = new LinkedHashMap<>(formData);
                } else {
                    result = pathMapper.fromFhirResource(updated, mappings);
                }
                result.put("id", resourceId);
                result.put("fhirId", resourceId);
                result.put("_resourceType", meta.type);
                log.info("Updated {} with FHIR ID: {} for tab: {}", meta.type, resourceId, tabKey);
                return result;
            }
        }

        // For single-record non-patient-scoped tabs (e.g. facility/location), fall back to
        // create/upsert if the resource cannot be found — handles deleted or never-created records.
        boolean hasSingleRecordResource = resources.stream()
                .anyMatch(m -> m.patientSearchParam == null || m.patientSearchParam.isBlank());
        if (hasSingleRecordResource && patientId != null) {
            log.info("Resource {} not found for single-record tab '{}', falling back to upsert (p-{})",
                    resourceId, tabKey, patientId);
            return create(tabKey, patientId, formData);
        }
        throw new IllegalArgumentException("Resource not found: " + resourceId);
    }

    /**
     * Delete a resource.
     */
    @Transactional
    public void delete(String tabKey, String resourceId) {
        scopeEnforcer.requireWrite(tabKey);
        TabFieldConfig config = getConfig(tabKey);
        List<ResourceMeta> resources = parseResourceMeta(config);
        String orgAlias = practiceContextService.getPracticeId();

        // First, determine the actual resource type by trying to read the resource
        String actualType = null;
        IBaseResource existingResource = null;
        for (ResourceMeta meta : resources) {
            try {
                existingResource = fhirClient.readByResourceName(meta.type, resourceId, orgAlias);
                actualType = meta.type;
                break;
            } catch (Exception e) {
                log.debug("Resource {} not found as {}, trying next type", resourceId, meta.type);
            }
        }

        if (actualType == null) {
            throw new IllegalArgumentException("Resource not found for deletion: " + resourceId);
        }

        // Try FHIR delete first; if referential integrity prevents it,
        // fall back to soft-delete by marking the resource with a "deleted" extension
        try {
            fhirClient.deleteByResourceName(actualType, resourceId, orgAlias);
            log.info("Hard-deleted {} with FHIR ID: {} for tab: {}", actualType, resourceId, tabKey);
        } catch (Exception e) {
            log.info("FHIR delete failed for {} {} (likely referential integrity), applying soft-delete: {}",
                    actualType, resourceId, e.getMessage());
            // Soft-delete: add a "deleted" tag and set active=false to exclude from searches
            if (existingResource instanceof Resource fhirResource) {
                fhirResource.getMeta().addTag("http://ciyex.org/fhir/tags", "deleted", "Soft Deleted");
                // Set active=false for resource types that support it
                if (fhirResource instanceof org.hl7.fhir.r4.model.Practitioner p) { p.setActive(false); }
                else if (fhirResource instanceof org.hl7.fhir.r4.model.Location l) { l.setStatus(org.hl7.fhir.r4.model.Location.LocationStatus.INACTIVE); }
                else if (fhirResource instanceof org.hl7.fhir.r4.model.Organization o) { o.setActive(false); }
                fhirClient.createOrUpdate(fhirResource, orgAlias);
                log.info("Soft-deleted {} with FHIR ID: {} for tab: {}", actualType, resourceId, tabKey);
            }
        }

        // Also clean up local form data
        try {
            formDataRepository.deleteByResourceIdAndOrgAlias(resourceId, orgAlias);
        } catch (Exception ex) {
            log.debug("No local form data to clean up for resource {}", resourceId);
        }
    }

    /**
     * Count all resources for a tab without patient scope.
     */
    public long count(String tabKey) {
        TabFieldConfig config = getConfig(tabKey);
        List<ResourceMeta> resources = parseResourceMeta(config);
        String orgAlias = practiceContextService.getPracticeId();
        if (resources.isEmpty()) return 0;
        ResourceMeta primary = resources.get(0);
        Class<? extends Resource> clazz = pathMapper.resolveResourceClass(primary.type);
        var search = fhirClient.getClient(orgAlias).search().forResource(clazz);
        for (Map.Entry<String, String> param : primary.searchParams.entrySet()) {
            search = search.where(new TokenClientParam(param.getKey()).exactly().code(param.getValue()));
        }
        Bundle bundle = search.count(1).returnBundle(Bundle.class).execute();
        return bundle.getTotal();
    }

    /**
     * Search resources by name (FHIR name:contains search parameter).
     * Falls back to in-memory filtering of all records when FHIR search returns empty,
     * to handle cases where the FHIR server doesn't support :contains or name is not indexed.
     */
    public Map<String, Object> searchByName(String tabKey, String query, int page, int size) {
        scopeEnforcer.requireRead(tabKey);
        TabFieldConfig config = getConfig(tabKey);
        List<FhirPathMapper.FieldMapping> mappings = parseMappings(config);
        List<ResourceMeta> resources = parseResourceMeta(config);
        String orgAlias = practiceContextService.getPracticeId();

        if (resources.isEmpty() || query == null || query.isBlank()) {
            return listAll(tabKey, page, size);
        }

        ResourceMeta primary = resources.get(0);
        Class<? extends Resource> clazz = pathMapper.resolveResourceClass(primary.type);

        Bundle bundle = null;
        try {
            var search = fhirClient.getClient(orgAlias).search()
                    .forResource(clazz)
                    .where(new StringClientParam("name").contains().value(query));
            for (Map.Entry<String, String> param : primary.searchParams.entrySet()) {
                search = search.and(new TokenClientParam(param.getKey()).exactly().code(param.getValue()));
            }
            bundle = search.count(size).totalMode(ca.uhn.fhir.rest.api.SearchTotalModeEnum.ACCURATE).returnBundle(Bundle.class).execute();
        } catch (Exception fhirEx) {
            log.debug("FHIR name:contains search failed for tab {}: {} — will use in-memory fallback", tabKey, fhirEx.getMessage());
        }

        @SuppressWarnings("unchecked")
        Class<Resource> resourceClass = (Class<Resource>) clazz;
        List<Resource> entries = bundle != null ? fhirClient.extractResources(bundle, resourceClass) : List.of();

        // Fallback: if FHIR name search returns nothing, load all records and filter in-memory
        if (entries.isEmpty()) {
            return searchByNameInMemory(tabKey, query, page, size);
        }

        for (int i = 0; i < page && bundle.getLink(Bundle.LINK_NEXT) != null; i++) {
            bundle = fhirClient.getClient(orgAlias).loadPage().next(bundle).execute();
            entries = fhirClient.extractResources(bundle, resourceClass);
        }
        boolean hasNext = bundle.getLink(Bundle.LINK_NEXT) != null;

        List<FhirPathMapper.FieldMapping> resourceMappings = mappings.stream()
                .filter(m -> primary.type.equals(m.resource())).toList();
        boolean isRawData = resourceMappings.isEmpty();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Resource resource : entries) {
            Map<String, Object> formData;
            if (isRawData) {
                formData = extractFormDataExtension(resource);
                if (formData == null) formData = new LinkedHashMap<>();
            } else {
                formData = pathMapper.fromFhirResource(resource, mappings);
                // Always merge form-data extension for any fields missing or blank from FHIR mapping
                mergeExtData(formData, extractFormDataExtension(resource), resourceMappings);
            }
            formData.put("id", resource.getIdElement().getIdPart());
            formData.put("fhirId", resource.getIdElement().getIdPart());
            formData.put("_resourceType", primary.type);
            if (resource.getMeta() != null && resource.getMeta().getLastUpdated() != null) {
                formData.put("_lastUpdated", resource.getMeta().getLastUpdated().toInstant().toString());
            }
            results.add(formData);
        }

        resolveReferences(results, mappings, orgAlias);

        int bundleTotal = bundle.getTotal() > 0 ? bundle.getTotal() : 0;
        int totalCount = !hasNext ? results.size() : Math.max(bundleTotal, results.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", results);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalCount);
        int calcPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
        response.put("totalPages", hasNext ? Math.max(calcPages, page + 2) : Math.max(calcPages, page + 1));
        response.put("hasNext", hasNext);
        return response;
    }

    /**
     * In-memory fallback: load all records for the tab and filter by query string.
     * Used when FHIR name search returns no results (server may not support name:contains).
     */
    private Map<String, Object> searchByNameInMemory(String tabKey, String query, int page, int size) {
        Map<String, Object> all = listAll(tabKey, 0, 500);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allContent = (List<Map<String, Object>>) all.get("content");
        if (allContent == null || allContent.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("content", List.of());
            empty.put("page", page);
            empty.put("size", size);
            empty.put("totalElements", 0);
            empty.put("totalPages", 0);
            empty.put("hasNext", false);
            return empty;
        }
        String lq = query.toLowerCase();
        List<Map<String, Object>> filtered = allContent.stream()
                .filter(r -> r.entrySet().stream()
                        .anyMatch(e -> !e.getKey().startsWith("_")
                                && e.getValue() != null
                                && e.getValue().toString().toLowerCase().contains(lq)))
                .collect(java.util.stream.Collectors.toList());
        int total = filtered.size();
        int start = page * size;
        List<Map<String, Object>> pageContent = filtered.stream().skip(start).limit(size).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", pageContent);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", total);
        response.put("totalPages", total > 0 ? (int) Math.ceil((double) total / size) : 0);
        response.put("hasNext", start + size < total);
        return response;
    }

    /**
     * Read a raw FHIR resource by type and ID.
     * Used by facade code that needs direct FHIR resource access (e.g., encounter status updates).
     */
    public Resource readRawResource(String resourceType, String resourceId) {
        String orgAlias = practiceContextService.getPracticeId();
        Class<? extends Resource> clazz = pathMapper.resolveResourceClass(resourceType);
        return (Resource) fhirClient.read(clazz, resourceId, orgAlias);
    }

    /**
     * Update a raw FHIR resource directly.
     */
    public void updateRawResource(Resource resource) {
        String orgAlias = practiceContextService.getPracticeId();
        fhirClient.update(resource, orgAlias);
    }

    // ==================== Internal helpers ====================

    private TabFieldConfig getConfig(String tabKey) {
        TabFieldConfig config = tabFieldConfigService.getEffectiveFieldConfig(tabKey, "*", getOrgId());
        if (config == null) {
            throw new IllegalArgumentException("No field config found for tab: " + tabKey);
        }
        // For org-specific rows, always use universal fhir_resources (migration fixes only
        // update universal rows, so org copies become stale). Also inherit field_config from
        // universal UNLESS this is an explicitly customised config (encounter-form settings).
        if (!"*".equals(config.getOrgId())) {
            TabFieldConfig universal = tabFieldConfigService.getPracticeTypeFieldConfig(tabKey, "*");
            if (universal != null) {
                config.setFhirResources(universal.getFhirResources());
                if (!"encounter-form".equals(tabKey)) {
                    config.setFieldConfig(universal.getFieldConfig());
                }
            }
        }
        return config;
    }

    private String getOrgId() {
        try {
            return practiceContextService.getPracticeId();
        } catch (Exception e) {
            return "*";
        }
    }

    /**
     * Parse field mappings from field_config JSON.
     */
    private List<FhirPathMapper.FieldMapping> parseMappings(TabFieldConfig config) {
        List<FhirPathMapper.FieldMapping> mappings = new ArrayList<>();
        try {
            JsonNode fieldConfig = objectMapper.readTree(config.getFieldConfig());
            JsonNode sections = fieldConfig.get("sections");
            if (sections != null && sections.isArray()) {
                for (JsonNode section : sections) {
                    JsonNode fields = section.get("fields");
                    if (fields != null && fields.isArray()) {
                        for (JsonNode field : fields) {
                            String key = field.has("key") ? field.get("key").asText() : null;
                            JsonNode fhirMapping = field.get("fhirMapping");
                            if (key != null && fhirMapping != null) {
                                FhirPathMapper.FieldMapping mapping = FhirPathMapper.FieldMapping.fromJson(key, fhirMapping);
                                if (mapping != null) mappings.add(mapping);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse field config for tab: {}", config.getTabKey(), e);
        }
        return mappings;
    }

    /**
     * Parse resource metadata from fhir_resources JSON.
     * Supports both old format: ["Appointment"] and new format: [{"type":"Appointment","patientSearchParam":"patient"}]
     */
    private List<ResourceMeta> parseResourceMeta(TabFieldConfig config) {
        List<ResourceMeta> result = new ArrayList<>();
        try {
            JsonNode resources = objectMapper.readTree(config.getFhirResources());
            if (resources.isArray()) {
                for (JsonNode node : resources) {
                    if (node.isTextual()) {
                        // Old format: simple string
                        result.add(new ResourceMeta(node.asText(), "subject", Map.of()));
                    } else if (node.isObject()) {
                        // New format: object with type, patientSearchParam, and optional searchParams
                        String type = node.get("type").asText();
                        String param = node.has("patientSearchParam") ? node.get("patientSearchParam").asText() : "subject";
                        Map<String, String> searchParams = new LinkedHashMap<>();
                        if (node.has("searchParams") && node.get("searchParams").isObject()) {
                            node.get("searchParams").fields().forEachRemaining(
                                    f -> searchParams.put(f.getKey(), f.getValue().asText()));
                        }
                        result.add(new ResourceMeta(type, param, searchParams));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse fhir_resources for tab: {}", config.getTabKey(), e);
        }
        return result;
    }

    /**
     * Apply searchParams from fhirResources config onto a newly created resource.
     * E.g., for insurance: searchParams={"type":"ins"} → sets Organization.type = [CodeableConcept with code "ins"]
     */
    private void applySearchParamsToResource(Resource resource, Map<String, String> searchParams) {
        if (searchParams == null || searchParams.isEmpty()) return;
        for (Map.Entry<String, String> entry : searchParams.entrySet()) {
            try {
                // For "type" on Organization, set as CodeableConcept with proper system
                // so fixSystemlessCodingsRecursive does not strip the coding
                if ("type".equals(entry.getKey()) && resource instanceof org.hl7.fhir.r4.model.Organization org) {
                    String typeSystem = "http://terminology.hl7.org/CodeSystem/organization-type";
                    String typeCode = entry.getValue();
                    // Remove any existing type coding with this code (avoid duplicates on update)
                    org.getType().forEach(cc -> cc.getCoding().removeIf(
                            c -> typeCode.equals(c.getCode()) && (c.getSystem() == null || typeSystem.equals(c.getSystem()))));
                    org.addType(new org.hl7.fhir.r4.model.CodeableConcept()
                            .addCoding(new org.hl7.fhir.r4.model.Coding()
                                    .setSystem(typeSystem)
                                    .setCode(typeCode)));
                }
            } catch (Exception e) {
                log.debug("Could not apply searchParam {} = {} on {}: {}", entry.getKey(), entry.getValue(), resource.fhirType(), e.getMessage());
            }
        }
    }

    private static final String FORM_DATA_EXTENSION_URL = "http://ciyex.org/fhir/StructureDefinition/form-data";

    /** Custom extension URL prefixes that the FHIR server does not recognize. */
    private static final List<String> CUSTOM_EXTENSION_PREFIXES = List.of(
            "http://ciyex.org/", "http://ciyex.com/");

    /**
     * Strip custom extensions (form-data, appointment-room, etc.) from a resource
     * before sending to the FHIR server, which rejects unknown extension URLs.
     */
    private void stripCustomExtensions(Resource resource) {
        if (resource instanceof DomainResource dr) {
            dr.getExtension().removeIf(ext -> {
                String url = ext.getUrl();
                if (url == null) return true;
                // Strip custom Ciyex extensions
                if (CUSTOM_EXTENSION_PREFIXES.stream().anyMatch(url::startsWith)) return true;
                // Strip extensions with non-absolute URLs (FHIR requires absolute URLs)
                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("urn:")) return true;
                return false;
            });
        }
    }

    /**
     * Create a FHIR resource with raw form data stored as a JSON extension.
     * Used when no field-level fhirMappings are defined (e.g. encounter forms).
     */
    private Resource createRawDataResource(String resourceType, Map<String, Object> formData) {
        try {
            Resource resource = pathMapper.resolveResourceClass(resourceType)
                    .getDeclaredConstructor()
                    .newInstance();
            storeFormDataExtension(resource, formData);
            return resource;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot instantiate FHIR resource: " + resourceType, e);
        }
    }

    /**
     * Resolve FHIR reference strings (e.g. "Practitioner/1134") to display names
     * for all reference-type fields in the form data results.
     */
    private void resolveReferences(List<Map<String, Object>> results, List<FhirPathMapper.FieldMapping> mappings, String orgAlias) {
        // Find all reference-type field keys
        List<String> refFieldKeys = mappings.stream()
                .filter(m -> "reference".equals(m.type()))
                .map(FhirPathMapper.FieldMapping::fieldKey)
                .toList();
        if (refFieldKeys.isEmpty()) return;

        // Collect all unique reference strings (with or without resource type prefix)
        Map<String, String> refToDisplay = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            for (String key : refFieldKeys) {
                Object val = row.get(key);
                // Skip if display already exists (from form-data extension)
                if (val instanceof String ref && !ref.isBlank() && !row.containsKey(key + "Display")) {
                    refToDisplay.putIfAbsent(ref, null);
                }
            }
        }
        if (refToDisplay.isEmpty()) return;

        // Resolve each reference
        for (String ref : new ArrayList<>(refToDisplay.keySet())) {
            try {
                // Normalize absolute URLs to relative references
                String normalizedRef = FhirPathMapper.toRelativeReference(ref);
                if (ref.contains("/")) {
                    // Full reference: "Practitioner/123" (or absolute URL now normalized)
                    String[] parts = normalizedRef.split("/", 2);
                    String resourceType = parts[0];
                    String id = parts[1];
                    Class<? extends Resource> clazz = pathMapper.resolveResourceClass(resourceType);
                    var opt = fhirClient.readOptional(clazz, id, orgAlias);
                    if (opt.isPresent()) {
                        String display = extractDisplayName((Resource) opt.get());
                        if (display != null) refToDisplay.put(ref, display);
                    }
                } else {
                    // Bare ID: try common resource types (Practitioner, Patient, Location)
                    for (Class<? extends Resource> clazz : List.of(Practitioner.class, Patient.class, Location.class)) {
                        try {
                            var opt = fhirClient.readOptional(clazz, ref, orgAlias);
                            if (opt.isPresent()) {
                                String display = extractDisplayName((Resource) opt.get());
                                if (display != null) {
                                    refToDisplay.put(ref, display);
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                log.debug("Could not resolve reference {}: {}", ref, e.getMessage());
            }
        }

        // Update form data: keep raw reference in original key, add display name in {key}Display
        for (Map<String, Object> row : results) {
            for (String key : refFieldKeys) {
                Object val = row.get(key);
                if (val instanceof String ref && refToDisplay.containsKey(ref)) {
                    String display = refToDisplay.get(ref);
                    if (display != null) {
                        row.put(key + "Display", display);
                    }
                }
            }
        }
    }

    /**
     * Pre-process form data to resolve reference field values that look like display names
     * (e.g., "ABC Insurance") to FHIR resource IDs. This prevents errors like
     * "Organization/ABC Insurance not found" when the UI sends a name instead of an ID.
     *
     * Only processes fields of type "reference" where the value contains spaces
     * (indicating it's a name, not a valid FHIR resource ID).
     */
    private void resolveReferenceNameValues(Map<String, Object> formData,
                                             List<FhirPathMapper.FieldMapping> mappings, String orgAlias) {
        for (FhirPathMapper.FieldMapping mapping : mappings) {
            if (!"reference".equals(mapping.type())) continue;
            Object val = formData.get(mapping.fieldKey());
            if (!(val instanceof String strVal) || strVal.isBlank()) continue;

            // Skip values that are already valid IDs (numeric or UUID-like, no spaces)
            if (!strVal.contains(" ")) continue;

            // This value looks like a display name — try to resolve it by searching
            String pathLower = mapping.path() != null ? mapping.path().toLowerCase() : "";
            String resourceType = null;

            if (pathLower.contains("insurer") || pathLower.contains("organization")) {
                resourceType = "Organization";
            } else if (pathLower.contains("provider") || pathLower.contains("practitioner")) {
                resourceType = "Practitioner";
            }

            if (resourceType == null) continue;

            try {
                Class<? extends Resource> clazz = pathMapper.resolveResourceClass(resourceType);
                Bundle bundle = fhirClient.search(clazz, orgAlias);
                var resources = fhirClient.extractResources(bundle, clazz);
                for (var resource : resources) {
                    String displayName = extractDisplayName((Resource) resource);
                    if (displayName != null && displayName.equalsIgnoreCase(strVal)) {
                        String fhirId = ((Resource) resource).getIdElement().getIdPart();
                        log.info("Resolved {} reference name '{}' to FHIR ID '{}'", resourceType, strVal, fhirId);
                        formData.put(mapping.fieldKey(), fhirId);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to resolve {} reference by name '{}': {}", resourceType, strVal, e.getMessage());
            }
        }
    }

    private String extractDisplayName(Resource resource) {
        try {
            if (resource instanceof org.hl7.fhir.r4.model.Practitioner p) {
                if (p.hasName()) {
                    var name = p.getNameFirstRep();
                    String given = name.hasGiven() ? name.getGiven().get(0).getValue() : "";
                    String family = name.hasFamily() ? name.getFamily() : "";
                    return (given + " " + family).trim();
                }
            } else if (resource instanceof org.hl7.fhir.r4.model.Location l) {
                return l.hasName() ? l.getName() : null;
            } else if (resource instanceof org.hl7.fhir.r4.model.Patient p) {
                if (p.hasName()) {
                    var name = p.getNameFirstRep();
                    String given = name.hasGiven() ? name.getGiven().get(0).getValue() : "";
                    String family = name.hasFamily() ? name.getFamily() : "";
                    return (given + " " + family).trim();
                }
            }
            // Fallback: try to get "name" property
            var prop = resource.getNamedProperty("name");
            if (prop != null && !prop.getValues().isEmpty()) {
                Base val = prop.getValues().get(0);
                if (val.isPrimitive()) {
                    return val.primitiveValue();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract display name from {}: {}", resource.fhirType(), e.getMessage());
        }
        return null;
    }

    private void storeFormDataExtension(Resource resource, Map<String, Object> formData) {
        storeFormDataExtension(resource, formData, null);
    }

    /**
     * Merge form-data extension (local DB) into the FHIR-extracted field map.
     * For FHIR-mapped fields: local DB value takes precedence over the FHIR path value,
     * since FHIR indexed-list data (e.g. HumanName.given) may be corrupted by old
     * append-based bugs (given[1] == given[0] == firstName).
     * For non-FHIR fields: local DB fills in null/blank FHIR values only.
     */
    private void mergeExtData(Map<String, Object> fd, Map<String, Object> extData,
                               List<FhirPathMapper.FieldMapping> resourceMappings) {
        if (extData == null) return;
        Set<String> fhirMappedKeys = resourceMappings.stream()
                .map(FhirPathMapper.FieldMapping::fieldKey)
                .collect(java.util.stream.Collectors.toSet());
        extData.forEach((k, v) -> {
            if (fhirMappedKeys.contains(k)) {
                // Local DB stores the user's last submitted value — use it to override
                // potentially-corrupted FHIR indexed-list data.
                if (v != null) fd.put(k, v);
            } else {
                Object existing = fd.get(k);
                if (existing == null || (existing instanceof String s && s.isBlank())) {
                    fd.put(k, v);
                } else {
                    fd.putIfAbsent(k, v);
                }
            }
        });
    }

    private void storeFormDataExtension(Resource resource, Map<String, Object> formData,
                                         Collection<FhirPathMapper.FieldMapping> mappings) {
        // Strip any existing form-data extension from the FHIR resource to avoid server rejection
        if (resource instanceof DomainResource dr) {
            dr.getExtension().removeIf(e -> FORM_DATA_EXTENSION_URL.equals(e.getUrl()));
        }
        // Form data will be stored in local DB after FHIR resource is created (see storeFormDataLocally)
    }

    /**
     * Store form data in local database (called after FHIR resource is created/updated).
     */
    private void storeFormDataLocally(String resourceType, String resourceId, String orgAlias,
                                      Map<String, Object> formData, Collection<FhirPathMapper.FieldMapping> mappings) {
        try {
            // Store ALL form data locally — FHIR-mapped fields are also stored as backup
            // since custom extension mappings get stripped before sending to FHIR server.
            Map<String, Object> dataToStore = formData;
            if (dataToStore.isEmpty()) return;
            String json = objectMapper.writeValueAsString(dataToStore);
            FhirFormDataEntity entity = formDataRepository
                    .findByResourceTypeAndResourceIdAndOrgAlias(resourceType, resourceId, orgAlias)
                    .orElseGet(() -> {
                        FhirFormDataEntity e = new FhirFormDataEntity();
                        e.setResourceType(resourceType);
                        e.setResourceId(resourceId);
                        e.setOrgAlias(orgAlias);
                        return e;
                    });
            entity.setFormData(json);
            formDataRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to store form data locally for {}/{}: {}", resourceType, resourceId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFormDataExtension(Resource resource) {
        // First try the local DB
        String orgAlias = practiceContextService.getPracticeId();
        String resourceId = resource.getIdElement().getIdPart();
        String resourceType = resource.fhirType();
        if (resourceId != null && orgAlias != null) {
            try {
                var opt = formDataRepository.findByResourceTypeAndResourceIdAndOrgAlias(
                        resourceType, resourceId, orgAlias);
                if (opt.isPresent()) {
                    return objectMapper.readValue(opt.get().getFormData(), Map.class);
                }
            } catch (Exception e) {
                log.debug("Failed to read form data from local DB for {}/{}: {}", resourceType, resourceId, e.getMessage());
            }
        }
        // Fallback: try legacy FHIR extension (for resources created before local storage)
        if (resource instanceof DomainResource dr) {
            Extension ext = dr.getExtensionByUrl(FORM_DATA_EXTENSION_URL);
            if (ext != null && ext.getValue() instanceof StringType st) {
                try {
                    return objectMapper.readValue(st.getValue(), Map.class);
                } catch (Exception e) {
                    log.error("Failed to deserialize form data from extension: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Set encounter reference on a resource.
     */
    private void setEncounterReference(Resource resource, String encounterRef) {
        try {
            resource.setProperty("encounter", new Reference("Encounter/" + encounterRef));
        } catch (Exception e) {
            log.debug("Could not set encounter reference on {}: {}", resource.fhirType(), e.getMessage());
        }
    }

    /**
     * Ensure required FHIR fields have valid defaults.
     * Some resource types require mandatory fields (e.g., Observation.status) that may not be
     * provided or may have been mapped from incompatible form field types (e.g., boolean → code).
     */
    private void ensureRequiredDefaults(Resource resource, Map<String, Object> formData, Long patientId) {
        if (resource instanceof Observation obs) {
            // Observation.status is required. Map signed boolean → proper status code.
            if (obs.getStatus() == null || obs.getStatus() == Observation.ObservationStatus.NULL) {
                Object signed = formData.get("signed");
                boolean isSigned = "true".equalsIgnoreCase(String.valueOf(signed))
                        || Boolean.TRUE.equals(signed);
                obs.setStatus(isSigned
                        ? Observation.ObservationStatus.FINAL
                        : Observation.ObservationStatus.PRELIMINARY);
            } else {
                // Validate status isn't a bad value like "true"/"false" from boolean mapping
                String statusCode = obs.getStatus().toCode();
                if ("true".equals(statusCode) || "false".equals(statusCode)) {
                    obs.setStatus("true".equals(statusCode)
                            ? Observation.ObservationStatus.FINAL
                            : Observation.ObservationStatus.PRELIMINARY);
                }
            }
            // Set vitals code if not already set
            if (!obs.hasCode()) {
                obs.getCode().addCoding()
                        .setSystem("http://loinc.org")
                        .setCode("85353-1")
                        .setDisplay("Vital signs, weight, height, head circumference, oxygen saturation and BMI panel");
            }
        } else if (resource instanceof AllergyIntolerance ai) {
            // Ensure clinicalStatus has system (FHIR R4 required binding)
            if (!ai.hasClinicalStatus() || !ai.getClinicalStatus().hasCoding()) {
                ai.setClinicalStatus(new CodeableConcept().addCoding(
                        new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                                .setCode("active")));
            } else {
                ensureSystemOnCodings(ai.getClinicalStatus(),
                        "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical");
            }
            // Ensure verificationStatus has system (FHIR R4 required binding)
            if (ai.getVerificationStatus() == null || !ai.getVerificationStatus().hasCoding()) {
                ai.getVerificationStatus().addCoding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                        .setCode("confirmed");
            } else {
                ensureSystemOnCodings(ai.getVerificationStatus(),
                        "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification");
            }
            // Ensure code (allergen) has system if it has codings
            if (ai.hasCode() && ai.getCode().hasCoding()) {
                ensureSystemOnCodings(ai.getCode(), "http://snomed.info/sct");
            }
            // Ensure reaction codings have systems
            for (var reaction : ai.getReaction()) {
                if (reaction.hasSubstance() && reaction.getSubstance().hasCoding()) {
                    ensureSystemOnCodings(reaction.getSubstance(), "http://snomed.info/sct");
                }
                for (var manifestation : reaction.getManifestation()) {
                    if (manifestation.hasCoding()) {
                        ensureSystemOnCodings(manifestation, "http://snomed.info/sct");
                    }
                }
                if (reaction.hasExposureRoute() && reaction.getExposureRoute().hasCoding()) {
                    ensureSystemOnCodings(reaction.getExposureRoute(), "http://snomed.info/sct");
                }
            }
            // Log the serialized resource for debugging
            log.info("AllergyIntolerance before FHIR save: clinicalStatus={}, verificationStatus={}, code={}",
                    ai.getClinicalStatus().hasCoding() ? ai.getClinicalStatus().getCodingFirstRep().getSystem() + "|" + ai.getClinicalStatus().getCodingFirstRep().getCode() : "none",
                    ai.getVerificationStatus().hasCoding() ? ai.getVerificationStatus().getCodingFirstRep().getSystem() + "|" + ai.getVerificationStatus().getCodingFirstRep().getCode() : "none",
                    ai.hasCode() ? ai.getCode().getText() : "none");
        } else if (resource instanceof Condition cond) {
            if (!cond.hasClinicalStatus() || !cond.getClinicalStatus().hasCoding()) {
                cond.setClinicalStatus(new CodeableConcept().addCoding(
                        new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                                .setCode("active")));
            } else {
                for (Coding c : cond.getClinicalStatus().getCoding()) {
                    if (c.getSystem() == null || c.getSystem().isBlank()) {
                        c.setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical");
                    }
                }
            }
        } else if (resource instanceof DocumentReference dr) {
            if (dr.getStatus() == null) {
                dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
            }
            // DocumentReference.content is required (minimum 1) — add a placeholder if empty
            if (dr.getContent() == null || dr.getContent().isEmpty()) {
                DocumentReference.DocumentReferenceContentComponent content = new DocumentReference.DocumentReferenceContentComponent();
                Attachment attachment = new Attachment();
                attachment.setContentType("text/plain");
                attachment.setTitle(formData.getOrDefault("title", formData.getOrDefault("name", "Template Document")).toString());
                content.setAttachment(attachment);
                dr.addContent(content);
            }
        } else if (resource instanceof Encounter enc) {
            // Encounter.status is required
            if (enc.getStatus() == null || enc.getStatus() == Encounter.EncounterStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "planned";
                try {
                    enc.setStatus(Encounter.EncounterStatus.fromCode(statusStr));
                } catch (Exception e) {
                    enc.setStatus(Encounter.EncounterStatus.PLANNED);
                }
            }
            // Encounter.class is required
            if (enc.getClass_() == null || enc.getClass_().getCode() == null || enc.getClass_().getCode().isBlank()) {
                String typeStr = formData.get("type") != null ? String.valueOf(formData.get("type")) : "AMB";
                enc.getClass_().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode").setCode(typeStr);
            }
        } else if (resource instanceof org.hl7.fhir.r4.model.DiagnosticReport dr) {
            // DiagnosticReport.status is required
            if (dr.getStatus() == null || dr.getStatus() == org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "final";
                try {
                    dr.setStatus(org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus.fromCode(statusStr));
                } catch (Exception e) {
                    dr.setStatus(org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus.FINAL);
                }
            }
            // DiagnosticReport.code is required
            if (!dr.hasCode()) {
                String code = formData.get("code") != null ? String.valueOf(formData.get("code")) : null;
                String display = formData.get("display") != null ? String.valueOf(formData.get("display")) : null;
                if (code != null) {
                    dr.getCode().addCoding().setSystem("http://loinc.org").setCode(code).setDisplay(display != null ? display : code);
                } else if (display != null) {
                    dr.getCode().setText(display);
                } else {
                    dr.getCode().setText("Lab Report");
                }
            }
            // DiagnosticReport.subject is required
            if (!dr.hasSubject() && patientId != null) {
                dr.setSubject(new Reference("Patient/" + patientId));
            }
        } else if (resource instanceof org.hl7.fhir.r4.model.Procedure proc) {
            // Procedure.status is required
            if (proc.getStatus() == null || proc.getStatus() == org.hl7.fhir.r4.model.Procedure.ProcedureStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "completed";
                try {
                    proc.setStatus(org.hl7.fhir.r4.model.Procedure.ProcedureStatus.fromCode(statusStr));
                } catch (Exception e) {
                    proc.setStatus(org.hl7.fhir.r4.model.Procedure.ProcedureStatus.COMPLETED);
                }
            }
            // Procedure.code is required — set from cptCode if provided
            if (!proc.hasCode()) {
                String cpt = formData.get("cptCode") != null ? String.valueOf(formData.get("cptCode")) : null;
                String name = formData.get("procedureName") != null ? String.valueOf(formData.get("procedureName")) : null;
                if (cpt != null || name != null) {
                    var coding = proc.getCode().addCoding();
                    if (cpt != null) coding.setSystem("http://www.ama-assn.org/go/cpt").setCode(cpt);
                    if (name != null) proc.getCode().setText(name);
                }
            }
        } else if (resource instanceof org.hl7.fhir.r4.model.Claim claim) {
            // Claim.status, type, use, and priority are required
            if (claim.getStatus() == null || claim.getStatus() == org.hl7.fhir.r4.model.Claim.ClaimStatus.NULL) {
                claim.setStatus(org.hl7.fhir.r4.model.Claim.ClaimStatus.DRAFT);
            }
            if (!claim.hasType()) {
                claim.getType().addCoding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/claim-type")
                        .setCode("professional");
            }
            if (claim.getUse() == null || claim.getUse() == org.hl7.fhir.r4.model.Claim.Use.NULL) {
                claim.setUse(org.hl7.fhir.r4.model.Claim.Use.CLAIM);
            }
            if (!claim.hasPriority()) {
                claim.getPriority().addCoding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/processpriority")
                        .setCode("normal");
            }
            // Auto-set created date if not provided or if stored without a value
            if (!claim.hasCreated() || claim.getCreatedElement().isEmpty()) {
                claim.setCreated(new java.util.Date());
            }
            // Claim.patient is required
            if (!claim.hasPatient() && patientId != null) {
                claim.setPatient(new org.hl7.fhir.r4.model.Reference("Patient/" + patientId));
            }
            // Claim.insurance is required (min 1) — add self-pay if none present
            if (!claim.hasInsurance()) {
                org.hl7.fhir.r4.model.Claim.InsuranceComponent ins = claim.addInsurance();
                ins.setSequence(1);
                ins.setFocal(true);
                ins.setCoverage(new org.hl7.fhir.r4.model.Reference().setDisplay("Self-pay"));
            }
            // Claim.insurance.sequence is required (min 1)
            for (int i = 0; i < claim.getInsurance().size(); i++) {
                var ins = claim.getInsurance().get(i);
                if (!ins.hasSequence() || ins.getSequence() == 0) {
                    ins.setSequence(i + 1);
                }
            }
            // Claim.diagnosis.sequence is required (min 1)
            for (int i = 0; i < claim.getDiagnosis().size(); i++) {
                var diag = claim.getDiagnosis().get(i);
                if (!diag.hasSequence() || diag.getSequence() == 0) {
                    diag.setSequence(i + 1);
                }
            }
            // Claim.procedure.sequence is required (min 1)
            for (int i = 0; i < claim.getProcedure().size(); i++) {
                var proc = claim.getProcedure().get(i);
                if (!proc.hasSequence() || proc.getSequence() == 0) {
                    proc.setSequence(i + 1);
                }
            }
            // Claim.item.sequence is required (min 1)
            for (int i = 0; i < claim.getItem().size(); i++) {
                var item = claim.getItem().get(i);
                if (!item.hasSequence() || item.getSequence() == 0) {
                    item.setSequence(i + 1);
                }
            }
            // FHIR constraint per-1: "If present, start SHALL have a lower value than end"
            if (claim.hasBillablePeriod()) {
                var period = claim.getBillablePeriod();
                if (period.hasStart() && period.hasEnd() && period.getStart().after(period.getEnd())) {
                    // Swap start and end if reversed
                    java.util.Date tmp = period.getStart();
                    period.setStart(period.getEnd());
                    period.setEnd(tmp);
                } else if (period.hasEnd() && !period.hasStart()) {
                    // If only end is set, copy it to start so period is valid
                    period.setStart(period.getEnd());
                }
            }
        } else if (resource instanceof org.hl7.fhir.r4.model.Coverage cov) {
            // Coverage.status is required
            if (cov.getStatus() == null || cov.getStatus() == org.hl7.fhir.r4.model.Coverage.CoverageStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "active";
                try {
                    cov.setStatus(org.hl7.fhir.r4.model.Coverage.CoverageStatus.fromCode(statusStr));
                } catch (Exception e) {
                    cov.setStatus(org.hl7.fhir.r4.model.Coverage.CoverageStatus.ACTIVE);
                }
            }
            // Coverage.relationship: fix system on subscriber relationship coding
            if (cov.hasRelationship() && cov.getRelationship().hasCoding()) {
                ensureSystemOnCodings(cov.getRelationship(),
                        "http://terminology.hl7.org/CodeSystem/subscriber-relationship");
            }
            // Coverage.class.type is required when class exists
            for (int i = 0; i < cov.getClass_().size(); i++) {
                var cls = cov.getClass_().get(i);
                if (!cls.hasType()) {
                    // Assign type based on position: 0=plan, 1=group
                    String code = i == 0 ? "plan" : i == 1 ? "group" : "subgroup";
                    cls.setType(new CodeableConcept().addCoding(
                            new Coding().setSystem("http://terminology.hl7.org/CodeSystem/coverage-class")
                                    .setCode(code)));
                }
            }
            // Coverage.payor is required (min 1)
            if (!cov.hasPayor()) {
                // Use insuranceCompany from form data if available
                String insurer = formData.get("insuranceCompany") != null ? String.valueOf(formData.get("insuranceCompany"))
                        : formData.get("insurer") != null ? String.valueOf(formData.get("insurer"))
                        : formData.get("payerName") != null ? String.valueOf(formData.get("payerName")) : null;
                if (insurer != null && !insurer.isBlank()) {
                    cov.addPayor(new org.hl7.fhir.r4.model.Reference().setDisplay(insurer));
                } else {
                    cov.addPayor(new org.hl7.fhir.r4.model.Reference().setDisplay("Unknown Payor"));
                }
            }
        } else if (resource instanceof Appointment appt) {
            // Appointment.status is required — map custom status names to FHIR codes
            String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "booked";
            String fhirCode = AppointmentEncounterService.toFhirCode(statusStr);
            try {
                appt.setStatus(Appointment.AppointmentStatus.fromCode(fhirCode));
            } catch (Exception e) {
                appt.setStatus(Appointment.AppointmentStatus.BOOKED);
            }
            // Appointment.participant is required (min 1) — add patient as participant if none present
            if (!appt.hasParticipant() && patientId != null) {
                Appointment.AppointmentParticipantComponent participant = appt.addParticipant();
                participant.setActor(new org.hl7.fhir.r4.model.Reference("Patient/" + patientId));
                participant.addType(new CodeableConcept().addCoding(
                        new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType").setCode("PART")));
                participant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
            }
            // Also add provider participant if provided in form data
            Object providerVal = formData.get("provider");
            if (providerVal != null && !String.valueOf(providerVal).isBlank()) {
                String provRef = String.valueOf(providerVal);
                if (!provRef.startsWith("Practitioner/")) provRef = "Practitioner/" + provRef;
                boolean hasProvider = appt.getParticipant().stream()
                        .anyMatch(p -> p.hasActor() && p.getActor().hasReference()
                                && p.getActor().getReference().startsWith("Practitioner/"));
                if (!hasProvider) {
                    Appointment.AppointmentParticipantComponent prov = appt.addParticipant();
                    prov.setActor(new org.hl7.fhir.r4.model.Reference(provRef));
                    prov.addType(new CodeableConcept().addCoding(
                            new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType").setCode("PART")));
                    prov.setStatus(Appointment.ParticipationStatus.ACCEPTED);
                }
            }
            // Ensure ALL participants have a status and valid type system
            for (Appointment.AppointmentParticipantComponent p : appt.getParticipant()) {
                if (p.getStatus() == null || p.getStatus() == Appointment.ParticipationStatus.NULL) {
                    p.setStatus(Appointment.ParticipationStatus.ACCEPTED);
                }
                // Fix participant type codings: internal codes like patient/practitioner/location
                // are not valid v3-ParticipationType values. Use a local system URL so HAPI
                // does not validate them against the HL7 terminology server.
                for (CodeableConcept type : p.getType()) {
                    for (Coding coding : type.getCoding()) {
                        String code = coding.hasCode() ? coding.getCode() : "";
                        if ("patient".equals(code) || "practitioner".equals(code) || "location".equals(code)) {
                            coding.setSystem("http://ciyex.org/fhir/participant-type");
                        } else if (!coding.hasSystem()) {
                            coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType");
                        }
                    }
                }
            }
            // Strip extensions with non-absolute URLs (FHIR requires absolute extension URLs)
            appt.getExtension().removeIf(ext -> {
                String url = ext.getUrl();
                return url != null && !url.startsWith("http://") && !url.startsWith("https://");
            });
        } else if (resource instanceof Flag flag) {
            // Flag.status is required
            if (flag.getStatus() == null || flag.getStatus() == Flag.FlagStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "active";
                try {
                    flag.setStatus(Flag.FlagStatus.fromCode(statusStr));
                } catch (Exception e) {
                    flag.setStatus(Flag.FlagStatus.ACTIVE);
                }
            }
            // Flag.code is required
            if (!flag.hasCode()) {
                String alertName = formData.get("alertName") != null ? String.valueOf(formData.get("alertName"))
                        : formData.get("name") != null ? String.valueOf(formData.get("name")) : "Clinical Alert";
                flag.setCode(new CodeableConcept().setText(alertName));
            }
            // Fix Flag.category codings — use proper flag-category system instead of v3-NullFlavor
            if (flag.hasCategory()) {
                for (CodeableConcept cat : flag.getCategory()) {
                    for (Coding c : cat.getCoding()) {
                        String code = c.hasCode() ? c.getCode() : "";
                        if ("clinical".equals(code) || "safety".equals(code) || "behavioral".equals(code)
                                || "research".equals(code) || "advance-directive".equals(code)
                                || "drug".equals(code) || "diet".equals(code) || "lab".equals(code)
                                || "contact".equals(code) || "admin".equals(code)) {
                            c.setSystem("http://terminology.hl7.org/CodeSystem/flag-category");
                        }
                    }
                }
            }
            // Flag.subject is required (min 1)
            if (!flag.hasSubject() && patientId != null) {
                flag.setSubject(new Reference("Patient/" + patientId));
            }
        } else if (resource instanceof Composition comp) {
            // Composition.status is required
            if (comp.getStatus() == null || comp.getStatus() == Composition.CompositionStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) :
                        (formData.get("noteStatus") != null ? String.valueOf(formData.get("noteStatus")) : "preliminary");
                try {
                    comp.setStatus(Composition.CompositionStatus.fromCode(statusStr));
                } catch (Exception e) {
                    comp.setStatus(Composition.CompositionStatus.PRELIMINARY);
                }
            }
            // Composition.type is required
            if (!comp.hasType()) {
                comp.setType(new CodeableConcept().addCoding(
                        new Coding().setSystem("http://loinc.org").setCode("34109-0").setDisplay("Note")));
            }
            // Composition.subject is required
            if (!comp.hasSubject() && patientId != null) {
                comp.setSubject(new Reference("Patient/" + patientId));
            }
            // Composition.date is required
            if (!comp.hasDate()) {
                comp.setDate(new java.util.Date());
            }
        } else if (resource instanceof ServiceRequest sr) {
            // ServiceRequest.status is required
            if (sr.getStatus() == null || sr.getStatus() == ServiceRequest.ServiceRequestStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "active";
                try {
                    sr.setStatus(ServiceRequest.ServiceRequestStatus.fromCode(statusStr));
                } catch (Exception e) {
                    sr.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
                }
            }
            // ServiceRequest.intent is required
            if (sr.getIntent() == null || sr.getIntent() == ServiceRequest.ServiceRequestIntent.NULL) {
                String intentStr = formData.get("intent") != null ? String.valueOf(formData.get("intent")) : "order";
                try {
                    sr.setIntent(ServiceRequest.ServiceRequestIntent.fromCode(intentStr));
                } catch (Exception e) {
                    sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
                }
            }
            // ServiceRequest.subject is required
            if (!sr.hasSubject() && patientId != null) {
                sr.setSubject(new Reference("Patient/" + patientId));
            }
        } else if (resource instanceof MedicationRequest mr) {
            // MedicationRequest.status is required
            if (mr.getStatus() == null || mr.getStatus() == MedicationRequest.MedicationRequestStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "active";
                try {
                    mr.setStatus(MedicationRequest.MedicationRequestStatus.fromCode(statusStr));
                } catch (Exception e) {
                    mr.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
                }
            }
            // MedicationRequest.intent is required
            if (mr.getIntent() == null || mr.getIntent() == MedicationRequest.MedicationRequestIntent.NULL) {
                String intentStr = formData.get("intent") != null ? String.valueOf(formData.get("intent")) : "order";
                try {
                    mr.setIntent(MedicationRequest.MedicationRequestIntent.fromCode(intentStr));
                } catch (Exception e) {
                    mr.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
                }
            }
            // MedicationRequest.medication[x] is required
            if (!mr.hasMedication()) {
                String medName = formData.get("medication") != null ? String.valueOf(formData.get("medication")) :
                                 formData.get("medicationName") != null ? String.valueOf(formData.get("medicationName")) : null;
                if (medName != null && !medName.isBlank()) {
                    mr.setMedication(new CodeableConcept().setText(medName));
                } else {
                    mr.setMedication(new CodeableConcept().setText("Unspecified medication"));
                }
            }
            // MedicationRequest.subject is required
            if (!mr.hasSubject() && patientId != null) {
                mr.setSubject(new Reference("Patient/" + patientId));
            }
        } else if (resource instanceof Immunization imm) {
            // Immunization.status is required
            if (imm.getStatus() == null || imm.getStatus() == Immunization.ImmunizationStatus.NULL) {
                String statusStr = formData.get("status") != null ? String.valueOf(formData.get("status")) : "completed";
                try {
                    imm.setStatus(Immunization.ImmunizationStatus.fromCode(statusStr));
                } catch (Exception e) {
                    imm.setStatus(Immunization.ImmunizationStatus.COMPLETED);
                }
            }
            // Immunization.patient is required
            if (!imm.hasPatient() && patientId != null) {
                imm.setPatient(new Reference("Patient/" + patientId));
            }
            // Immunization.vaccineCode is required
            if (!imm.hasVaccineCode()) {
                String vaccineName = formData.get("vaccineName") != null ? String.valueOf(formData.get("vaccineName")) : "Unknown";
                imm.getVaccineCode().setText(vaccineName);
            }
            // Immunization.occurrence is required
            if (imm.getOccurrence() == null) {
                imm.setOccurrence(new DateTimeType(new java.util.Date()));
            }
        } else if (resource instanceof RelatedPerson rp) {
            // Normalize relationship codes to valid FHIR value set codes
            if (rp.hasRelationship()) {
                for (CodeableConcept rel : rp.getRelationship()) {
                    for (Coding c : rel.getCoding()) {
                        if (c.hasCode()) {
                            c.setCode(normalizeRelationshipCode(c.getCode()));
                        }
                        if (c.getSystem() == null || c.getSystem().isBlank()) {
                            c.setSystem("http://terminology.hl7.org/CodeSystem/v3-RoleCode");
                        }
                    }
                }
            }
            // RelatedPerson.patient is required
            if (!rp.hasPatient() && patientId != null) {
                rp.setPatient(new Reference("Patient/" + patientId));
            }
        }
    }

    /**
     * Normalize relationship type codes to valid FHIR v3-RoleCode values.
     * Maps common frontend display values to proper FHIR codes.
     */
    private String normalizeRelationshipCode(String code) {
        if (code == null) return "FAMMEMB";
        return switch (code.toLowerCase().trim()) {
            case "child", "son", "daughter" -> "CHILD";
            case "parent", "mother", "father" -> "PRN";
            case "spouse", "husband", "wife" -> "SPS";
            case "sibling", "brother", "sister" -> "SIB";
            case "guardian" -> "GUARD";
            case "friend" -> "FRND";
            case "emergency", "emergency contact", "emergencycontact" -> "ECON";
            case "other" -> "FAMMEMB";
            default -> code.toUpperCase();
        };
    }

    /**
     * Extract participant references from Appointment resources as fallback.
     * Legacy appointments may lack type.coding on participants, so FHIRPath
     * "participant.where(type.coding.code='practitioner').actor" returns nothing.
     * This scans the actor reference prefix to determine type.
     */
    private void extractAppointmentParticipantFallbacks(Appointment appt, Map<String, Object> formData) {
        for (Appointment.AppointmentParticipantComponent p : appt.getParticipant()) {
            if (!p.hasActor() || !p.getActor().hasReference()) continue;
            String ref = p.getActor().getReference();
            if (ref.startsWith("Practitioner/")) {
                Object existing = formData.get("provider");
                if (existing == null || (existing instanceof String s && s.isBlank())) {
                    formData.put("provider", ref);
                }
            } else if (ref.startsWith("Location/")) {
                Object existing = formData.get("location");
                if (existing == null || (existing instanceof String s && s.isBlank())) {
                    formData.put("location", ref);
                }
            } else if (ref.startsWith("Patient/")) {
                Object existing = formData.get("patient");
                if (existing == null || (existing instanceof String s && s.isBlank())) {
                    formData.put("patient", ref);
                }
            }
        }
    }

    /**
     * Recursively walk a FHIR resource and fix any Coding that has a code but no system.
     * Uses a generic fallback system URI based on the resource type context.
     */
    /**
     * Fix systemless Codings at the JSON level. Parses the JSON, walks all objects,
     * and adds system URIs where Coding objects have code but no system.
     */
    @SuppressWarnings("unchecked")
    private String fixSystemlessCodingsInJson(String json) {
        try {
            var node = objectMapper.readTree(json);
            if (node.isObject()) {
                fixCodingsInJsonNode((com.fasterxml.jackson.databind.node.ObjectNode) node, null);
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("fixSystemlessCodingsInJson failed: {}", e.getMessage());
            return json;
        }
    }

    private void fixCodingsInJsonNode(com.fasterxml.jackson.databind.JsonNode node, String parentKey) {
        if (node == null) return;
        if (node.isObject()) {
            var obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
            // Check if this is a Coding object (has "code" but no "system")
            if (obj.has("code") && !obj.has("system") && obj.get("code").isTextual()) {
                String code = obj.get("code").asText();
                String system = inferCodingSystem(code);
                obj.put("system", system);
                log.info("JSON fix: added system={} for code={} in parent={}", system, code, parentKey);
            }
            var fields = obj.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                fixCodingsInJsonNode(entry.getValue(), entry.getKey());
            }
        } else if (node.isArray()) {
            for (var element : node) {
                fixCodingsInJsonNode(element, parentKey);
            }
        }
    }

    private void fixSystemlessCodingsRecursive(Resource resource) {
        try {
            walkAndFixCodings(resource);
        } catch (Exception e) {
            log.warn("Error in fixSystemlessCodingsRecursive for {}: {}", resource.fhirType(), e.getMessage());
        }
    }

    private void walkAndFixCodings(org.hl7.fhir.instance.model.api.IBase element) {
        if (element == null) return;
        if (element instanceof Coding coding) {
            if (coding.hasCode() && (coding.getSystem() == null || coding.getSystem().isBlank())) {
                String code = coding.getCode();
                String system = inferCodingSystem(code);
                if (system != null && !system.contains("data-absent-reason")) {
                    coding.setSystem(system);
                    log.info("Fixed systemless Coding: code={} -> system={}", code, system);
                }
                // If data-absent-reason would be used, skip — leave systemless rather than assign invalid system
            }
            return;
        }
        if (element instanceof org.hl7.fhir.r4.model.CodeableConcept cc) {
            var toRemove = new java.util.ArrayList<Coding>();
            for (var coding : cc.getCoding()) {
                if (coding.hasCode() && (coding.getSystem() == null || coding.getSystem().isBlank())) {
                    String code = coding.getCode();
                    String system = inferCodingSystem(code);
                    if (system != null && system.contains("data-absent-reason")) {
                        // Unknown code — move to text instead of assigning invalid system
                        if (!cc.hasText()) {
                            cc.setText(coding.hasDisplay() ? coding.getDisplay() : code);
                        }
                        toRemove.add(coding);
                    } else {
                        coding.setSystem(system);
                        log.info("Fixed systemless Coding in CC: code={} -> system={}", code, system);
                    }
                }
            }
            cc.getCoding().removeAll(toRemove);
            return;
        }
        // Walk all children using HAPI's Property API
        if (element instanceof org.hl7.fhir.r4.model.Base base) {
            try {
                for (var prop : base.children()) {
                    if (prop != null) {
                        for (var child : prop.getValues()) {
                            walkAndFixCodings(child);
                        }
                    }
                }
            } catch (Exception e) {
                // Some elements don't support children() — ignore
            }
        }
    }

    private String inferCodingSystem(String code) {
        if (code == null) return "http://terminology.hl7.org/CodeSystem/data-absent-reason";
        // Organization type codes
        if ("prov".equals(code) || "dept".equals(code) || "ins".equals(code)
                || "bus".equals(code) || "other".equals(code) || "edu".equals(code)
                || "govt".equals(code) || "cg".equals(code) || "team".equals(code)
                || "reli".equals(code) || "crs".equals(code) || "crsv".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/organization-type";
        }
        // AllergyIntolerance clinical status codes
        if ("active".equals(code) || "inactive".equals(code) || "resolved".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical";
        }
        // AllergyIntolerance verification status codes
        if ("confirmed".equals(code) || "unconfirmed".equals(code) || "refuted".equals(code) || "entered-in-error".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification";
        }
        // Condition clinical status
        if ("recurrence".equals(code) || "relapse".equals(code) || "remission".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/condition-clinical";
        }
        // Flag category codes
        if ("clinical".equals(code) || "safety".equals(code) || "behavioral".equals(code)
                || "research".equals(code) || "advance-directive".equals(code)
                || "drug".equals(code) || "diet".equals(code) || "lab".equals(code)
                || "contact".equals(code) || "admin".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/flag-category";
        }
        // FHIR encounter class codes (v3-ActCode)
        if ("AMB".equals(code) || "EMER".equals(code) || "IMP".equals(code)
                || "ACUTE".equals(code) || "NONAC".equals(code) || "OBSENC".equals(code)
                || "PRENC".equals(code) || "SS".equals(code) || "VR".equals(code)
                || "HH".equals(code) || "FLD".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/v3-ActCode";
        }
        // Claim type codes
        if ("institutional".equals(code) || "professional".equals(code) || "oral".equals(code)
                || "pharmacy".equals(code) || "vision".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/claim-type";
        }
        // Coverage class codes
        if ("plan".equals(code) || "group".equals(code) || "subgroup".equals(code)
                || "subplan".equals(code) || "class".equals(code) || "subclass".equals(code)
                || "rxbin".equals(code) || "rxpcn".equals(code) || "rxid".equals(code)
                || "rxgroup".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/coverage-class";
        }
        // LOINC codes (numeric pattern like 85353-1)
        if (code.matches("\\d{4,5}-\\d")) {
            return "http://loinc.org";
        }
        // Participant type codes
        if ("PART".equals(code) || "ATND".equals(code) || "ADM".equals(code)
                || "PPRF".equals(code) || "SPRF".equals(code) || "CON".equals(code)
                || "REF".equals(code) || "DIS".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/v3-ParticipationType";
        }
        // Subscriber relationship
        if ("self".equals(code) || "spouse".equals(code) || "child".equals(code)
                || "parent".equals(code) || "common".equals(code) || "other".equals(code)
                || "injured".equals(code)) {
            return "http://terminology.hl7.org/CodeSystem/subscriber-relationship";
        }
        // Default: use data-absent-reason (safer than v3-NullFlavor)
        return "http://terminology.hl7.org/CodeSystem/data-absent-reason";
    }

    /**
     * Ensure all Codings in a CodeableConcept have a system URI.
     */
    private void ensureSystemOnCodings(org.hl7.fhir.r4.model.CodeableConcept cc, String defaultSystem) {
        if (cc == null) return;
        for (var coding : cc.getCoding()) {
            if (coding.hasCode() && (coding.getSystem() == null || coding.getSystem().isBlank())) {
                coding.setSystem(defaultSystem);
            }
        }
    }

    /**
     * Set the patient reference on a resource based on the search parameter name.
     */
    private void setPatientReference(Resource resource, String paramName, Long patientId) {
        String ref = "Patient/" + patientId;
        try {
            // Use typed setters for known resource types to ensure correct property setting
            if (resource instanceof org.hl7.fhir.r4.model.Coverage cov && "beneficiary".equals(paramName)) {
                cov.setBeneficiary(new org.hl7.fhir.r4.model.Reference(ref));
                // Also set subscriber to patient if not already set
                if (!cov.hasSubscriber()) {
                    cov.setSubscriber(new org.hl7.fhir.r4.model.Reference(ref));
                }
            } else {
                resource.setProperty(paramName, new org.hl7.fhir.r4.model.Reference(ref));
            }
        } catch (Exception e) {
            log.debug("Could not set patient reference via param '{}' on {}: {}",
                    paramName, resource.fhirType(), e.getMessage());
        }
    }

    /**
     * Strip references to deleted resources from a FHIR resource before update.
     * This prevents HAPI-1096 errors when referenced resources (e.g., Appointment) have been deleted.
     */
    private void stripDeletedReferences(Resource resource, String orgAlias) {
        if (resource instanceof org.hl7.fhir.r4.model.Encounter encounter) {
            // Check appointment references
            if (encounter.hasAppointment()) {
                var validAppointments = new java.util.ArrayList<>(encounter.getAppointment());
                validAppointments.removeIf(ref -> {
                    if (ref.hasReference()) {
                        try {
                            String refStr = ref.getReference();
                            String refId = refStr.contains("/") ? refStr.substring(refStr.lastIndexOf("/") + 1) : refStr;
                            fhirClient.readOptional(org.hl7.fhir.r4.model.Appointment.class, refId, orgAlias);
                            return false; // Appointment exists, keep it
                        } catch (Exception e) {
                            log.warn("Stripping deleted appointment reference '{}' from Encounter/{}", ref.getReference(), resource.getIdElement().getIdPart());
                            return true; // Appointment deleted/not found, remove it
                        }
                    }
                    return false;
                });
                encounter.setAppointment(validAppointments);
            }
        }
    }

    private record ResourceMeta(String type, String patientSearchParam, Map<String, String> searchParams) {}
}
