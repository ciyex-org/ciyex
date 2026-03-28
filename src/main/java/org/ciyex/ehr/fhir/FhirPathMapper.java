package org.ciyex.ehr.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bidirectional mapper between flat form data (Map&lt;String,Object&gt;) and HAPI FHIR R4 Resources.
 * Uses fhirMapping paths from tab_field_config to navigate FHIR resource structure.
 *
 * Supported path patterns:
 * - Simple: "status", "priority", "comment"
 * - Nested: "name[0].given[0]", "period.start", "code.text"
 * - Indexed: "participant[0].actor.reference", "telecom[2].value"
 * - Coded: "code.coding[0].code" with system
 * - Extension: "extension[url=http://...].valueString"
 * - Reference: "subject.reference", "performer[0].reference"
 */
@Component
@Slf4j
public class FhirPathMapper {

    // Pattern to match array indices like [0], [1]
    private static final Pattern INDEX_PATTERN = Pattern.compile("(.+?)\\[(\\d+)]");
    // Pattern to match extension filter like [url=http://...]
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("extension\\[url=(.+?)]");
    // Pattern to match where() filter like telecom.where(system='phone') or component.where(code.coding.code='29463-7')
    private static final Pattern WHERE_PATTERN = Pattern.compile("(\\w+)\\.where\\(([\\w.]+)='([^']+)'\\)");

    /**
     * Split a FHIR path by '.' while respecting brackets (don't split inside [...]).
     * E.g., "extension[url=http://ciyex.org/fhir/ext/insurance-tier].valueCode"
     * → ["extension[url=http://ciyex.org/fhir/ext/insurance-tier]", "valueCode"]
     */
    private static String[] splitPath(String path) {
        List<String> segments = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '[' || c == '(') depth++;
            else if (c == ']' || c == ')') depth--;
            else if (c == '.' && depth == 0) {
                segments.add(path.substring(start, i));
                start = i + 1;
            }
        }
        if (start < path.length()) segments.add(path.substring(start));
        return segments.toArray(new String[0]);
    }

    /**
     * Mapping descriptor parsed from field_config's fhirMapping object.
     */
    public record FieldMapping(
            String fieldKey,
            String resource,
            String path,
            String type,
            String system,
            String loincCode,
            String unit
    ) {
        public static FieldMapping fromJson(String fieldKey, JsonNode mapping) {
            if (mapping == null) return null;
            return new FieldMapping(
                    fieldKey,
                    mapping.has("resource") ? mapping.get("resource").asText() : null,
                    mapping.has("path") ? mapping.get("path").asText() : null,
                    mapping.has("type") ? mapping.get("type").asText() : "string",
                    mapping.has("system") ? mapping.get("system").asText() : null,
                    mapping.has("loincCode") ? mapping.get("loincCode").asText() : null,
                    mapping.has("unit") ? mapping.get("unit").asText() : null
            );
        }
    }

    /**
     * Resolve a HAPI FHIR resource class from a resource type name string.
     * Uses reflection to support ANY FHIR R4 resource type dynamically.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Resource> resolveResourceClass(String resourceType) {
        try {
            var clazz = Class.forName("org.hl7.fhir.r4.model." + resourceType);
            if (Resource.class.isAssignableFrom(clazz)) {
                return (Class<? extends Resource>) clazz;
            }
        } catch (ClassNotFoundException ignored) {}
        throw new IllegalArgumentException("Unknown FHIR resource type: " + resourceType);
    }

    /**
     * Create a new FHIR resource instance from a resource type name.
     * Uses reflection to support ANY FHIR R4 resource type dynamically.
     */
    public Resource createResource(String resourceType) {
        try {
            Class<? extends Resource> clazz = resolveResourceClass(resourceType);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create FHIR resource: " + resourceType, e);
        }
    }

    /**
     * Convert flat form data → FHIR Resource using field mappings.
     */
    public Resource toFhirResource(String resourceType, Map<String, Object> formData, List<FieldMapping> mappings) {
        Resource resource = createResource(resourceType);
        return applyFormData(resource, resourceType, formData, mappings);
    }

    /**
     * Apply form data onto an existing FHIR Resource (merge/update).
     * Preserves all fields not included in formData.
     */
    public Resource applyFormData(Resource resource, String resourceType, Map<String, Object> formData, List<FieldMapping> mappings) {

        for (FieldMapping mapping : mappings) {
            if (mapping == null || mapping.path() == null) continue;
            if (!resourceType.equals(mapping.resource())) continue;

            Object value = formData.get(mapping.fieldKey());
            if (value == null || (value instanceof String s && s.isBlank())) continue;

            try {
                setValueOnResource(resource, mapping.path(), value, mapping);
            } catch (Exception e) {
                log.warn("Failed to set field {} at path {} on {}: {}",
                        mapping.fieldKey(), mapping.path(), resourceType, e.getMessage());
            }
        }
        return resource;
    }

    /**
     * Convert FHIR Resource → flat form data using field mappings.
     */
    public Map<String, Object> fromFhirResource(Resource resource, List<FieldMapping> mappings) {
        Map<String, Object> formData = new LinkedHashMap<>();
        String resourceType = resource.fhirType();

        // Always include the FHIR ID
        if (resource.hasId()) {
            formData.put("id", resource.getIdElement().getIdPart());
            formData.put("fhirId", resource.getIdElement().getIdPart());
        }

        for (FieldMapping mapping : mappings) {
            if (mapping == null || mapping.path() == null) continue;
            if (!resourceType.equals(mapping.resource())) continue;

            try {
                Object value = getValueFromResource(resource, mapping.path(), mapping);
                if (value != null) {
                    formData.put(mapping.fieldKey(), value);
                }
            } catch (Exception e) {
                log.debug("Failed to read field {} at path {} from {}: {}",
                        mapping.fieldKey(), mapping.path(), resourceType, e.getMessage());
            }
        }

        // Add meta info
        if (resource.hasMeta() && resource.getMeta().hasLastUpdated()) {
            formData.put("_lastUpdated", resource.getMeta().getLastUpdated().toInstant().toString());
        }

        return formData;
    }

    // ==================== SET value on resource ====================

    private void setValueOnResource(Resource resource, String path, Object value, FieldMapping mapping) {
        // Handle where() filter: e.g. "identifier.where(system='http://...').value"
        Matcher whereMatcher = WHERE_PATTERN.matcher(path);
        if (whereMatcher.find()) {
            String listProp = whereMatcher.group(1);
            String filterProp = whereMatcher.group(2);
            String filterValue = whereMatcher.group(3);
            String remainder = path.substring(whereMatcher.end());
            if (remainder.startsWith(".")) remainder = remainder.substring(1);

            setValueViaWhereFilter(resource, listProp, filterProp, filterValue, remainder, value, mapping);
            return;
        }

        String[] segments = splitPath(path);
        Base current = resource;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);

            // Handle extension[url=...] pattern
            Matcher extMatcher = EXTENSION_PATTERN.matcher(segment);
            if (extMatcher.matches()) {
                String extUrl = extMatcher.group(1);
                if (current instanceof DomainResource dr) {
                    Extension ext = dr.getExtensionByUrl(extUrl);
                    if (ext == null) {
                        ext = new Extension(extUrl);
                        dr.addExtension(ext);
                    }
                    // Next segment is the value type (e.g., valueString, valueCode, valueDateTime)
                    if (i + 1 < segments.length) {
                        setExtensionValue(ext, segments[i + 1], value, mapping);
                    }
                    return;
                }
                continue;
            }

            // Handle indexed access like participant[0]
            Matcher indexMatcher = INDEX_PATTERN.matcher(segment);
            if (indexMatcher.matches()) {
                String propName = indexMatcher.group(1);
                int index = Integer.parseInt(indexMatcher.group(2));

                if (isLast) {
                    setPropertyValue(current, propName, index, value, mapping);
                } else {
                    current = getOrCreateChild(current, propName, index);
                    if (current == null) return;
                }
                continue;
            }

            // Simple property
            if (isLast) {
                setPropertyValue(current, segment, -1, value, mapping);
            } else {
                current = getOrCreateChild(current, segment, -1);
                if (current == null) return;
            }
        }
    }

    private void setExtensionValue(Extension ext, String valueType, Object value, FieldMapping mapping) {
        // Guard: if the extension has nested child extensions (complex extension), clear them first.
        // Setting a direct value on an extension that already has child extensions causes
        // HAPI-1827 "Extension contains both a value and nested extensions".
        if (!ext.getExtension().isEmpty()) {
            ext.getExtension().clear();
        }
        String strVal = String.valueOf(value);
        switch (valueType) {
            case "valueString" -> ext.setValue(new StringType(strVal));
            case "valueCode" -> ext.setValue(new CodeType(strVal));
            case "valueDateTime" -> ext.setValue(new DateTimeType(strVal));
            case "valueDate" -> ext.setValue(new DateType(strVal));
            case "valueBoolean" -> ext.setValue(new BooleanType(Boolean.parseBoolean(strVal)));
            case "valueInteger" -> ext.setValue(new IntegerType(Integer.parseInt(strVal)));
            case "valueDecimal" -> ext.setValue(new DecimalType(new BigDecimal(strVal)));
            case "valueCoding" -> ext.setValue(new Coding().setCode(strVal));
            case "valueCodeableConcept" -> ext.setValue(new CodeableConcept().addCoding(
                    new Coding().setCode(strVal)));
            case "valueReference" -> ext.setValue(new Reference(strVal));
            default -> ext.setValue(new StringType(strVal));
        }
    }

    /**
     * Set a value on a resource list element filtered by a where() clause.
     * Supports nested filter paths: e.g., component.where(code.coding.code='29463-7').valueQuantity.value
     * And simple filter paths: e.g., telecom.where(system='phone').value
     */
    private void setValueViaWhereFilter(Resource resource, String listProp, String filterProp,
                                         String filterValue, String targetProp, Object value, FieldMapping mapping) {
        try {
            var prop = resource.getNamedProperty(listProp);
            if (prop == null) return;

            boolean isNestedFilter = filterProp.contains(".");

            // Search existing entries for matching filter
            Base matched = null;
            for (Base item : prop.getValues()) {
                if (isNestedFilter) {
                    Base filterBase = navigateDottedPath(item, filterProp);
                    if (filterBase != null && filterValue.equals(filterBase.primitiveValue())) {
                        matched = item;
                        break;
                    }
                } else {
                    var filterField = item.getNamedProperty(filterProp);
                    if (filterField != null && !filterField.getValues().isEmpty()) {
                        String existing = filterField.getValues().get(0).primitiveValue();
                        if (filterValue.equals(existing)) {
                            matched = item;
                            break;
                        }
                    }
                }
            }

            // If not found, create a new entry with the filter property set
            if (matched == null) {
                matched = resource.addChild(listProp);
                if (isNestedFilter) {
                    // For nested filter paths like "code.coding.code", build the structure
                    String[] filterParts = filterProp.split("\\.");
                    Base current = matched;
                    for (int i = 0; i < filterParts.length - 1; i++) {
                        current = getOrCreateChild(current, filterParts[i], -1);
                        if (current == null) return;
                    }
                    // Set the leaf filter value
                    String leafProp = filterParts[filterParts.length - 1];
                    current.setProperty(leafProp, new CodeType(filterValue));
                } else {
                    // Simple filter property (e.g., system='phone')
                    Base filterType;
                    if ("system".equals(filterProp) && "telecom".equals(listProp)) {
                        filterType = new CodeType(filterValue);
                    } else if ("system".equals(filterProp)) {
                        filterType = new UriType(filterValue);
                    } else {
                        filterType = new StringType(filterValue);
                    }
                    matched.setProperty(filterProp, filterType);
                }
            }

            // Set the target property on the matched entry (may be dotted like "valueQuantity.value" or "answer[0].valueCoding.code")
            if (!targetProp.isBlank()) {
                if (targetProp.contains(".")) {
                    String[] targetParts = targetProp.split("\\.");
                    Base current = matched;
                    for (int i = 0; i < targetParts.length - 1; i++) {
                        String seg = targetParts[i];
                        Matcher idxMatcher = INDEX_PATTERN.matcher(seg);
                        if (idxMatcher.matches()) {
                            current = getOrCreateChild(current, idxMatcher.group(1), Integer.parseInt(idxMatcher.group(2)));
                        } else {
                            current = getOrCreateChild(current, seg, -1);
                        }
                        if (current == null) return;
                    }
                    String lastSeg = targetParts[targetParts.length - 1];
                    Matcher lastIdxMatcher = INDEX_PATTERN.matcher(lastSeg);
                    if (lastIdxMatcher.matches()) {
                        setPropertyValue(current, lastIdxMatcher.group(1), Integer.parseInt(lastIdxMatcher.group(2)), value, mapping);
                    } else {
                        setPropertyValue(current, lastSeg, -1, value, mapping);
                    }
                } else {
                    Matcher idxMatcher = INDEX_PATTERN.matcher(targetProp);
                    if (idxMatcher.matches()) {
                        setPropertyValue(matched, idxMatcher.group(1), Integer.parseInt(idxMatcher.group(2)), value, mapping);
                    } else {
                        setPropertyValue(matched, targetProp, -1, value, mapping);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to set where-filtered value {}.where({}='{}').{} = {}: {}",
                    listProp, filterProp, filterValue, targetProp, value, e.getMessage());
        }
    }

    /**
     * Clear all existing values in a primitive-type list on a FHIR Base element.
     * Called when HAPI FHIR's addChild() throws for primitive list types (e.g., HumanName.given),
     * ensuring we rebuild the list from scratch at index 0 rather than accumulating duplicates.
     */
    private void clearPrimitiveList(Base parent, String propName) {
        if (parent instanceof HumanName hn) {
            switch (propName) {
                case "given" -> hn.getGiven().clear();
                case "prefix" -> hn.getPrefix().clear();
                case "suffix" -> hn.getSuffix().clear();
            }
        }
        // Other resource types with primitive lists can be added here as needed
    }

    private Base getOrCreateChild(Base parent, String propName, int index) {
        try {
            var prop = parent.getNamedProperty(propName);
            if (prop != null) {
                List<Base> values = prop.getValues();
                if (index >= 0) {
                    while (values.size() <= index) {
                        parent.addChild(propName);
                        values = parent.getNamedProperty(propName).getValues();
                    }
                    return values.get(index);
                } else {
                    if (values.isEmpty()) {
                        parent.addChild(propName);
                        values = parent.getNamedProperty(propName).getValues();
                    }
                    if (!values.isEmpty()) {
                        return values.get(0);
                    }
                }
            }
            // Fallback: try addChild directly — handles FHIR choice types like value[x]
            // e.g., addChild("valueQuantity") creates a Quantity on Observation.component
            return parent.addChild(propName);
        } catch (Exception e) {
            log.debug("Cannot navigate to child {} on {}: {}", propName, parent.fhirType(), e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void setPropertyValue(Base parent, String propName, int index, Object value, FieldMapping mapping) {
        // Handle reference type: value may be a Map {reference: "Practitioner/123"} sent from frontend
        // Extract the "reference" string to avoid String.valueOf(map) producing "{reference=...}"
        if ("reference".equals(mapping.type()) && value instanceof Map<?, ?> refMap && refMap.containsKey("reference")) {
            Object refVal = refMap.get("reference");
            if (refVal instanceof String refStr && !refStr.isBlank()) {
                value = refStr;
            }
        }

        // Handle address type: value is a map {line1, line2, city, state, zip, country}
        if ("address".equals(mapping.type()) && value instanceof Map) {
            try {
                Map<String, Object> addrMap = (Map<String, Object>) value;
                Address addr = new Address();
                if (addrMap.containsKey("line1") && addrMap.get("line1") != null) {
                    addr.addLine(String.valueOf(addrMap.get("line1")));
                }
                if (addrMap.containsKey("line2") && addrMap.get("line2") != null && !String.valueOf(addrMap.get("line2")).isBlank()) {
                    addr.addLine(String.valueOf(addrMap.get("line2")));
                }
                if (addrMap.containsKey("city") && addrMap.get("city") != null) addr.setCity(String.valueOf(addrMap.get("city")));
                if (addrMap.containsKey("state") && addrMap.get("state") != null) addr.setState(String.valueOf(addrMap.get("state")));
                if (addrMap.containsKey("zip") && addrMap.get("zip") != null) addr.setPostalCode(String.valueOf(addrMap.get("zip")));
                if (addrMap.containsKey("country") && addrMap.get("country") != null) addr.setCountry(String.valueOf(addrMap.get("country")));
                parent.setProperty(propName, addr);
                return;
            } catch (Exception e) {
                log.debug("Cannot set address property {} on {}: {}", propName, parent.fhirType(), e.getMessage());
                return;
            }
        }

        String strVal = String.valueOf(value);

        // Special case: setting 'text' on a CodeableConcept — use the direct typed setter
        // This avoids potential reflection issues with the generic setProperty path
        if ("text".equals(propName) && parent instanceof CodeableConcept cc) {
            cc.setText(strVal);
            return;
        }

        // When setting a property on a ContactPoint, apply the system from mapping
        if (parent instanceof ContactPoint cp && mapping.system() != null && !mapping.system().isBlank()) {
            switch (mapping.system().toLowerCase()) {
                case "phone" -> cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
                case "fax" -> cp.setSystem(ContactPoint.ContactPointSystem.FAX);
                case "email" -> cp.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                case "pager" -> cp.setSystem(ContactPoint.ContactPointSystem.PAGER);
                case "url" -> cp.setSystem(ContactPoint.ContactPointSystem.URL);
                case "sms" -> cp.setSystem(ContactPoint.ContactPointSystem.SMS);
            }
        }

        // Infer the correct type — when setting 'value' on a Quantity parent, use decimal
        String effectiveType = mapping.type();
        if ("value".equals(propName) && parent instanceof Quantity) {
            effectiveType = "decimal";
        }
        // Determine the target type based on mapping type
        Base typedValue = switch (effectiveType) {
                case "code" -> {
                    // When setting code on a Coding parent, also set the system
                    if (parent instanceof Coding codingParent && mapping.system() != null) {
                        codingParent.setSystem(mapping.system());
                    }
                    yield new CodeType(strVal);
                }
                case "string" -> new StringType(strVal);
                case "date" -> new DateType(strVal);
                case "dateTime", "datetime" -> {
                    // Normalize datetime-local format (YYYY-MM-DDTHH:mm) to full ISO datetime
                    String dtVal = strVal;
                    if (dtVal.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                        dtVal = dtVal + ":00";
                    }
                    yield new DateTimeType(dtVal);
                }
                case "instant" -> {
                    // Normalize datetime-local format (YYYY-MM-DDTHH:mm) to full RFC-3339 instant
                    String instVal = strVal;
                    if (instVal.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                        instVal = instVal + ":00Z";
                    } else if (instVal.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}") && !instVal.endsWith("Z")) {
                        instVal = instVal + "Z";
                    }
                    yield new InstantType(instVal);
                }
                case "boolean" -> new BooleanType(Boolean.parseBoolean(strVal));
                case "decimal" -> new DecimalType(new BigDecimal(strVal));
                case "integer", "positiveInt", "unsignedInt" -> {
                    try {
                        yield new IntegerType(Integer.parseInt(strVal));
                    } catch (NumberFormatException e) {
                        // Handle named values (e.g., priority "routine" → 5)
                        yield new IntegerType(switch (strVal.toLowerCase().trim()) {
                            case "emergency", "stat" -> 1;
                            case "urgent" -> 2;
                            case "routine", "normal" -> 5;
                            case "low" -> 9;
                            default -> 0;
                        });
                    }
                }
                case "reference" -> {
                    // Ensure reference values contain resource type prefix (e.g., "Practitioner/123")
                    // Strip absolute URLs to relative references — FHIR servers reject external references.
                    String refVal = toRelativeReference(strVal);

                    if (!refVal.contains("/")) {
                        // Bare ID - infer resource type from the path context
                        String inferredType = inferReferenceResourceType(propName, mapping);
                        if (inferredType != null) {
                            refVal = inferredType + "/" + refVal;
                        }
                    } else if (!refVal.startsWith("http")) {
                        // Relative reference like "Organization/ABC insurance" — validate the ID part
                        // If the ID portion contains spaces or looks like a name, log a warning.
                        // The FHIR server will reject "Organization/ABC insurance" as an invalid reference.
                        String[] parts = refVal.split("/", 2);
                        if (parts.length == 2 && parts[1].contains(" ")) {
                            log.warn("Reference value '{}' contains spaces in ID portion — this may be a display name " +
                                    "instead of a FHIR resource ID. The FHIR server may reject this reference.", refVal);
                        }
                    }

                    // When setting .reference property on a Reference parent, set it directly
                    if (parent instanceof Reference refParent) {
                        refParent.setReference(refVal);
                        yield null; // Already set directly
                    }
                    yield new Reference(refVal);
                }
                case "quantity" -> {
                    var qty = new Quantity();
                    qty.setValue(new BigDecimal(strVal));
                    if (mapping.unit() != null) qty.setUnit(mapping.unit());
                    yield qty;
                }
                default -> new StringType(strVal);
            };
        // If value was already set directly (e.g., reference on Reference parent), skip
        if (typedValue == null) return;
        // Check if this is a FHIR choice type (e.g., deceasedDateTime, multipleBirthBoolean, valueQuantity)
        String choiceBase = propName.replaceAll("(DateTime|Boolean|String|Integer|Quantity|Reference|Period|CodeableConcept|Coding|Range|Ratio|Attachment|Identifier|Age|Duration)$", "");
        if (!choiceBase.equals(propName) && !choiceBase.isEmpty()) {
            // Choice type: use reflection setter (e.g., setDeceased(Type), setValue(Type))
            try {
                String setterName = "set" + choiceBase.substring(0, 1).toUpperCase() + choiceBase.substring(1);
                var method = parent.getClass().getMethod(setterName, org.hl7.fhir.r4.model.Type.class);
                method.invoke(parent, typedValue);
                return;
            } catch (Exception refEx) {
                log.warn("Choice type setter failed for {} on {}: {}", propName, parent.fhirType(), refEx.getMessage());
            }
        }
        // Handle indexed list property assignment (e.g., name[0].given[1] for middleName).
        // parent.setProperty() APPENDS to list properties — we must set in-place at the target
        // index to avoid duplicating earlier elements (e.g., firstName appearing as middleName).
        if (index >= 0) {
            try {
                var namedProp = parent.getNamedProperty(propName);
                if (namedProp != null) {
                    List<Base> values = namedProp.getValues();
                    // Grow list to accommodate the target index
                    while (values.size() <= index) {
                        parent.addChild(propName);
                        values = parent.getNamedProperty(propName).getValues();
                    }
                    // Set value in place at the target index (avoids appending duplicates)
                    Base target = values.get(index);
                    if (target instanceof org.hl7.fhir.r4.model.PrimitiveType<?> pt) {
                        pt.setValueAsString(strVal);
                    } else {
                        target.setProperty("value", typedValue);
                    }
                    return;
                }
            } catch (Exception e) {
                log.debug("Indexed list set failed for {}[{}] on {}: {}", propName, index, parent.fhirType(), e.getMessage());
                // addChild failed (e.g., HAPI FHIR throws for primitive-type lists like HumanName.given).
                // For index 0: clear any existing list entries before appending so we rebuild from scratch.
                // This prevents corruption from prior setProperty()-append calls accumulating stale values.
                if (index == 0) {
                    clearPrimitiveList(parent, propName);
                }
                // Fall through to default setProperty
            }
        }
        try {
            parent.setProperty(propName, typedValue);
        } catch (Exception e) {
            // Retry with alternate date type: DateType ↔ DateTimeType
            // (e.g., Period.start expects DateTimeType but mapping says type: "date")
            if (typedValue instanceof DateType) {
                try {
                    parent.setProperty(propName, new DateTimeType(strVal));
                    return;
                } catch (Exception ignored) {}
            } else if (typedValue instanceof DateTimeType) {
                try {
                    parent.setProperty(propName, new DateType(strVal));
                    return;
                } catch (Exception ignored) {}
            }
            log.debug("Cannot set property {} = {} on {}: {}", propName, strVal, parent.fhirType(), e.getMessage());
        }
    }

    // ==================== GET value from resource ====================

    private Object getValueFromResource(Resource resource, String path, FieldMapping mapping) {
        // Handle where() filter: e.g. "identifier.where(system='http://...').value"
        Matcher whereMatcher = WHERE_PATTERN.matcher(path);
        if (whereMatcher.find()) {
            String listProp = whereMatcher.group(1);
            String filterProp = whereMatcher.group(2);
            String filterValue = whereMatcher.group(3);
            String remainder = path.substring(whereMatcher.end());
            if (remainder.startsWith(".")) remainder = remainder.substring(1);

            return getValueViaWhereFilter(resource, listProp, filterProp, filterValue, remainder);
        }

        String[] segments = splitPath(path);
        Base current = resource;

        for (int i = 0; i < segments.length; i++) {
            if (current == null) return null;
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);

            // Handle extension[url=...]
            Matcher extMatcher = EXTENSION_PATTERN.matcher(segment);
            if (extMatcher.matches()) {
                String extUrl = extMatcher.group(1);
                if (current instanceof DomainResource dr) {
                    Extension ext = dr.getExtensionByUrl(extUrl);
                    if (ext == null || !ext.hasValue()) return null;
                    if (i + 1 < segments.length) {
                        return extractPrimitiveValue(ext.getValue());
                    }
                    return extractPrimitiveValue(ext.getValue());
                }
                return null;
            }

            // Handle indexed access
            Matcher indexMatcher = INDEX_PATTERN.matcher(segment);
            if (indexMatcher.matches()) {
                String propName = indexMatcher.group(1);
                int index = Integer.parseInt(indexMatcher.group(2));

                var prop = current.getNamedProperty(propName);
                if (prop == null || prop.getValues().size() <= index) return null;

                if (isLast) {
                    return extractPrimitiveValue(prop.getValues().get(index));
                }
                current = prop.getValues().get(index);
                continue;
            }

            // Simple property
            var prop = current.getNamedProperty(segment);
            if (prop == null || prop.getValues().isEmpty()) return null;

            Base propValue = prop.getValues().get(0);

            // FHIR choice type validation: e.g., path "deceasedDateTime" but value is BooleanType
            // getNamedProperty("deceasedDateTime") returns the deceased[x] value regardless of actual type
            if (!isChoiceTypeCompatible(segment, propValue)) return null;

            if (isLast) {
                return extractPrimitiveValue(propValue);
            }
            current = propValue;
        }
        return null;
    }

    /**
     * Navigate a dotted path from a Base element, returning the final Base value.
     * E.g., "code.coding.code" navigates: base → code → coding[0] → code
     * Used for where() filter property paths and target property paths.
     */
    private Base navigateDottedPath(Base base, String dottedPath) {
        if (base == null || dottedPath == null || dottedPath.isBlank()) return base;
        String[] parts = dottedPath.split("\\.");
        Base current = base;
        for (String part : parts) {
            if (current == null) return null;
            // Handle indexed access like coding[0]
            Matcher indexMatcher = INDEX_PATTERN.matcher(part);
            if (indexMatcher.matches()) {
                String propName = indexMatcher.group(1);
                int index = Integer.parseInt(indexMatcher.group(2));
                var prop = current.getNamedProperty(propName);
                if (prop == null || prop.getValues().size() <= index) return null;
                current = prop.getValues().get(index);
            } else {
                var prop = current.getNamedProperty(part);
                if (prop == null || prop.getValues().isEmpty()) return null;
                current = prop.getValues().get(0);
            }
        }
        return current;
    }

    /**
     * Get a value from a resource list element filtered by a where() clause.
     * Supports nested filter paths: e.g., component.where(code.coding.code='29463-7').valueQuantity.value
     * And simple filter paths: e.g., telecom.where(system='phone').value
     */
    private Object getValueViaWhereFilter(Resource resource, String listProp, String filterProp,
                                           String filterValue, String targetProp) {
        try {
            var prop = resource.getNamedProperty(listProp);
            if (prop == null) return null;

            for (Base item : prop.getValues()) {
                // Navigate the filter property path (may be dotted like "code.coding.code")
                Base filterBase = navigateDottedPath(item, filterProp);
                if (filterBase != null) {
                    String existing = filterBase.primitiveValue();
                    if (filterValue.equals(existing)) {
                        // Found the matching entry — get the target property
                        if (targetProp.isBlank()) {
                            return extractPrimitiveValue(item);
                        }
                        // Navigate the target property path (may be dotted like "valueQuantity.value")
                        Base targetBase = navigateDottedPath(item, targetProp);
                        if (targetBase != null) {
                            return extractPrimitiveValue(targetBase);
                        }
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get where-filtered value {}.where({}='{}').{}: {}",
                    listProp, filterProp, filterValue, targetProp, e.getMessage());
        }
        return null;
    }

    /**
     * Infer the FHIR resource type for a bare reference ID based on the property name and context.
     */
    private String inferReferenceResourceType(String propName, FieldMapping mapping) {
        // Map common FHIR reference property names to their expected resource types
        String pathLower = (mapping != null && mapping.path() != null) ? mapping.path().toLowerCase() : "";
        String propLower = propName != null ? propName.toLowerCase() : "";

        if (pathLower.contains("requester") || pathLower.contains("prescriber")
                || pathLower.contains("provider") || pathLower.contains("performer")
                || pathLower.contains("recorder") || pathLower.contains("asserter")
                || pathLower.contains("sender") || pathLower.contains("author")
                || pathLower.contains("authenticator") || pathLower.contains("billingprovider")
                || propLower.equals("practitioner")) {
            return "Practitioner";
        }
        if (pathLower.contains("subject") || pathLower.contains("patient")
                || propLower.equals("patient") || propLower.equals("subject")) {
            return "Patient";
        }
        if (pathLower.contains("encounter") || propLower.equals("encounter")) {
            return "Encounter";
        }
        if (pathLower.contains("organization") || pathLower.contains("insurer")
                || pathLower.contains("managingorganization")
                || propLower.equals("organization") || propLower.equals("insurer")) {
            return "Organization";
        }
        if (pathLower.contains("referral") || propLower.equals("referral")) {
            return "ServiceRequest";
        }
        if (pathLower.contains("location") || propLower.equals("location")) {
            return "Location";
        }
        if (pathLower.contains("coverage") || propLower.equals("coverage")) {
            return "Coverage";
        }
        if ((pathLower.contains("request") && !pathLower.contains("servicerequest") && !pathLower.contains("requester"))
                || propLower.equals("claim")) {
            return "Claim";
        }
        log.debug("Cannot infer reference type for prop='{}', path='{}'", propName, pathLower);
        return null;
    }

    /**
     * Convert an absolute FHIR reference URL to a relative reference.
     * E.g., "https://fhir-server/orgAlias/Practitioner/12345" → "Practitioner/12345"
     */
    static String toRelativeReference(String refStr) {
        if (refStr == null) return null;
        String[] resourceTypes = {"Practitioner", "Patient", "Organization", "Encounter",
                "Location", "Coverage", "Claim", "ServiceRequest", "Observation",
                "Condition", "Procedure", "MedicationRequest", "AllergyIntolerance",
                "Appointment", "DocumentReference", "Binary", "RelatedPerson",
                "DiagnosticReport", "CarePlan", "CareTeam", "Goal", "Immunization"};
        if (!refStr.startsWith("http://") && !refStr.startsWith("https://")) {
            // Strip duplicate resource type prefix: "Practitioner/Practitioner/123" → "Practitioner/123"
            for (String rt : resourceTypes) {
                String doubled = rt + "/" + rt + "/";
                if (refStr.startsWith(doubled)) {
                    return refStr.substring(rt.length() + 1);
                }
            }
            return refStr;
        }
        // Find the resource type segment in the URL
        for (String rt : resourceTypes) {
            int idx = refStr.indexOf("/" + rt + "/");
            if (idx >= 0) {
                return refStr.substring(idx + 1);
            }
        }
        return refStr; // fallback: return as-is if no known resource type found
    }

    private Object extractPrimitiveValue(Base base) {
        if (base == null) return null;
        if (base instanceof PrimitiveType<?> pt) return pt.getValueAsString();
        if (base instanceof Quantity qty) return qty.getValue() != null ? qty.getValue().toString() : null;
        if (base instanceof Reference ref) {
            // Return the reference string; display is handled separately by GenericFhirResourceService
            // Always normalize to relative reference (strip absolute FHIR server URLs)
            if (ref.hasReference()) {
                return toRelativeReference(ref.getReference());
            }
            return ref.hasDisplay() ? ref.getDisplay() : null;
        }
        if (base instanceof Coding coding) return coding.getCode();
        if (base instanceof CodeableConcept cc) {
            if (cc.hasCoding()) return cc.getCodingFirstRep().getCode();
            return cc.getText();
        }
        if (base instanceof Money money) return money.getValue() != null ? money.getValue().toString() : null;
        // Address type: extract as a map for the AddressField component
        if (base instanceof Address addr) {
            Map<String, Object> addrMap = new LinkedHashMap<>();
            if (addr.hasLine() && !addr.getLine().isEmpty()) {
                addrMap.put("line1", addr.getLine().get(0).getValue());
                if (addr.getLine().size() > 1) {
                    addrMap.put("line2", addr.getLine().get(1).getValue());
                }
            }
            if (addr.hasCity()) addrMap.put("city", addr.getCity());
            if (addr.hasState()) addrMap.put("state", addr.getState());
            if (addr.hasPostalCode()) addrMap.put("zip", addr.getPostalCode());
            if (addr.hasCountry()) addrMap.put("country", addr.getCountry());
            return addrMap.isEmpty() ? null : addrMap;
        }
        // For complex types, return the first primitive child
        if (base.isPrimitive()) return base.primitiveValue();
        return base.toString();
    }

    /**
     * Check if a FHIR choice type value matches the expected type indicated by the path segment.
     * E.g., path "deceasedDateTime" expects DateTimeType, not BooleanType.
     * Returns true if the value is compatible, false if it's the wrong choice type.
     */
    private boolean isChoiceTypeCompatible(String segment, Base value) {
        if (value == null) return false;
        // Detect choice type suffixes: e.g., deceasedDateTime, deceasedBoolean, valueQuantity, valueString
        // Common choice type base names and their suffixes
        String lowerSeg = segment.toLowerCase();
        // DateType is compatible with DateTime choice types (date is a subset of dateTime in FHIR)
        if (lowerSeg.endsWith("datetime") && !(value instanceof DateTimeType || value instanceof DateType)) return false;
        if (lowerSeg.endsWith("boolean") && !lowerSeg.equals("boolean") && !(value instanceof BooleanType)) return false;
        if (lowerSeg.endsWith("string") && !lowerSeg.equals("string") && !(value instanceof StringType)) return false;
        if (lowerSeg.endsWith("integer") && !lowerSeg.equals("integer") && !(value instanceof IntegerType)) return false;
        if (lowerSeg.endsWith("quantity") && !lowerSeg.equals("quantity") && !(value instanceof Quantity)) return false;
        if (lowerSeg.endsWith("period") && !lowerSeg.equals("period") && !(value instanceof Period)) return false;
        if (lowerSeg.endsWith("reference") && !lowerSeg.equals("reference") && !(value instanceof Reference)) return false;
        return true;
    }
}
