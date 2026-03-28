package org.ciyex.ehr.tabconfig.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.tabconfig.entity.CustomTab;
import org.ciyex.ehr.tabconfig.entity.PracticeType;
import org.ciyex.ehr.tabconfig.entity.TabConfig;
import org.ciyex.ehr.tabconfig.repository.CustomTabRepository;
import org.ciyex.ehr.tabconfig.repository.PracticeTypeRepository;
import org.ciyex.ehr.tabconfig.repository.TabConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TabConfigService {

    private final PracticeTypeRepository practiceTypeRepo;
    private final TabConfigRepository tabConfigRepo;
    private final CustomTabRepository customTabRepo;
    private final ObjectMapper objectMapper;

    // ---- Practice Types ----

    public List<PracticeType> listPracticeTypes(String orgId) {
        return practiceTypeRepo.findAllForOrg(orgId);
    }

    public Optional<PracticeType> getPracticeType(String code, String orgId) {
        return practiceTypeRepo.findByCodeAndOrgId(code, orgId)
                .or(() -> practiceTypeRepo.findByCodeAndOrgId(code, "*"));
    }

    @Transactional
    public PracticeType createPracticeType(PracticeType pt) {
        return practiceTypeRepo.save(pt);
    }

    @Transactional
    public PracticeType updatePracticeType(String code, String orgId, PracticeType update) {
        PracticeType pt = practiceTypeRepo.findByCodeAndOrgId(code, orgId)
                .or(() -> practiceTypeRepo.findByCodeAndOrgId(code, "*"))
                .orElseThrow(() -> new NoSuchElementException("Practice type not found: " + code));
        pt.setName(update.getName());
        pt.setDescription(update.getDescription());
        pt.setIcon(update.getIcon());
        pt.setCategory(update.getCategory());
        if (update.getDefaultTabConfig() != null) {
            pt.setDefaultTabConfig(update.getDefaultTabConfig());
        }
        return practiceTypeRepo.save(pt);
    }

    @Transactional
    public void deletePracticeType(String code, String orgId) {
        PracticeType pt = practiceTypeRepo.findByCodeAndOrgId(code, orgId)
                .or(() -> practiceTypeRepo.findByCodeAndOrgId(code, "*"))
                .orElseThrow(() -> new NoSuchElementException("Practice type not found: " + code));
        pt.setActive(false);
        practiceTypeRepo.save(pt);
    }

    // ---- Practice Type Defaults ----

    public Map<String, Object> getPracticeTypeDefaults(String code, String orgId) {
        PracticeType pt = getPracticeType(code, orgId)
                .orElseThrow(() -> new NoSuchElementException("Practice type not found: " + code));

        List<Object> tabConfig = parseTabConfig(pt.getDefaultTabConfig());
        return Map.of(
                "practiceTypeCode", pt.getCode(),
                "practiceTypeName", pt.getName(),
                "tabConfig", tabConfig,
                "source", "PRACTICE_TYPE_DEFAULT"
        );
    }

    public List<String> getPracticeTypeSpecialties(String code) {
        // Query the mapping table directly via native query
        return practiceTypeRepo.findByCodeAndOrgId(code, "*")
                .map(pt -> {
                    // For now return from the mapping table - we'll use a native approach
                    return List.<String>of();
                })
                .orElse(List.of());
    }

    // ---- Effective Tab Config ----
    // DEPRECATED: Use TabFieldConfigService.getLayout() instead.
    // This method is kept for backward compatibility with /api/tab-config/effective endpoint.

    public Map<String, Object> getEffectiveConfig(String orgId) {
        // Delegate to TabFieldConfigService for unified layout
        // (falls through to tab_config legacy if field config has no layout data)
        Optional<TabConfig> orgConfig = tabConfigRepo.findByOrgId(orgId);
        if (orgConfig.isPresent()) {
            TabConfig tc = orgConfig.get();
            return Map.of(
                    "tabConfig", parseTabConfig(tc.getTabConfig()),
                    "source", tc.getSource(),
                    "practiceTypeCode", tc.getPracticeTypeCode() != null ? tc.getPracticeTypeCode() : "universal"
            );
        }

        // Return universal default
        return Map.of(
                "tabConfig", getUniversalDefault(),
                "source", "UNIVERSAL_DEFAULT",
                "practiceTypeCode", "universal"
        );
    }

    @Transactional
    public Map<String, Object> cloneFromDefault(String orgId, String practiceTypeCode) {
        PracticeType pt = getPracticeType(practiceTypeCode, orgId)
                .orElseThrow(() -> new NoSuchElementException("Practice type not found: " + practiceTypeCode));

        String configJson = pt.getDefaultTabConfig();
        if (configJson == null || configJson.isBlank()) {
            configJson = toJson(getUniversalDefault());
        }

        // Upsert org config
        TabConfig tc = tabConfigRepo.findByOrgId(orgId).orElse(new TabConfig());
        tc.setOrgId(orgId);
        tc.setPracticeTypeCode(practiceTypeCode);
        tc.setTabConfig(configJson);
        tc.setSource("CLONED_FROM_DEFAULT");
        tabConfigRepo.save(tc);

        return Map.of(
                "tabConfig", parseTabConfig(configJson),
                "source", "CLONED_FROM_DEFAULT",
                "practiceTypeCode", practiceTypeCode
        );
    }

    @Transactional
    public void saveOrgConfig(String orgId, String tabConfigJson) {
        TabConfig tc = tabConfigRepo.findByOrgId(orgId).orElse(new TabConfig());
        tc.setOrgId(orgId);
        tc.setTabConfig(tabConfigJson);
        tc.setSource("ORG_CUSTOM");
        tabConfigRepo.save(tc);
    }

    @Transactional
    public void deleteOrgConfig(String orgId) {
        tabConfigRepo.deleteByOrgId(orgId);
    }

    // ---- Custom Tabs ----

    public List<CustomTab> listCustomTabs(String orgId) {
        return customTabRepo.findByOrgIdOrderByPosition(orgId);
    }

    @Transactional
    public CustomTab createCustomTab(CustomTab tab) {
        return customTabRepo.save(tab);
    }

    @Transactional
    public CustomTab updateCustomTab(UUID id, CustomTab update) {
        CustomTab tab = customTabRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Custom tab not found"));
        tab.setTabKey(update.getTabKey());
        tab.setLabel(update.getLabel());
        tab.setIcon(update.getIcon());
        tab.setCategory(update.getCategory());
        tab.setFormSchema(update.getFormSchema());
        tab.setPosition(update.getPosition());
        tab.setActive(update.isActive());
        return customTabRepo.save(tab);
    }

    @Transactional
    public void deleteCustomTab(UUID id) {
        customTabRepo.deleteById(id);
    }

    // ---- Helpers ----

    private List<Object> parseTabConfig(String json) {
        if (json == null || json.isBlank()) return getUniversalDefault();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tab config JSON", e);
            return getUniversalDefault();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> getUniversalDefault() {
        String json = """
            [
              {"label":"Overview","position":0,"tabs":[
                {"key":"demographics","label":"Demographics","icon":"User","visible":true,"position":0},
                {"key":"insurance","label":"Insurance","icon":"Shield","visible":true,"position":1},
                {"key":"vitals","label":"Vitals","icon":"Activity","visible":true,"position":2},
                {"key":"allergies","label":"Allergies","icon":"AlertTriangle","visible":true,"position":3},
                {"key":"problem-list","label":"Problem List","icon":"ClipboardList","visible":true,"position":4}
              ]},
              {"label":"Encounters","position":1,"tabs":[
                {"key":"encounters","label":"Encounters","icon":"Calendar","visible":true,"position":0},
                {"key":"appointments","label":"Appointments","icon":"Clock","visible":true,"position":1},
                {"key":"visit-notes","label":"Visit Notes","icon":"FileText","visible":true,"position":2},
                {"key":"referrals","label":"Referrals","icon":"Share2","visible":true,"position":3}
              ]},
              {"label":"Clinical","position":2,"tabs":[
                {"key":"medications","label":"Medications","icon":"Pill","visible":true,"position":0},
                {"key":"lab-results","label":"Lab Results","icon":"TestTube","visible":true,"position":1},
                {"key":"immunizations","label":"Immunizations","icon":"Syringe","visible":true,"position":2},
                {"key":"procedures","label":"Procedures","icon":"Scissors","visible":true,"position":3}
              ]},
              {"label":"Claims","position":3,"tabs":[
                {"key":"claims","label":"Claims","icon":"Receipt","visible":true,"position":0},
                {"key":"claim-submissions","label":"Submissions","icon":"Send","visible":true,"position":1},
                {"key":"claim-denials","label":"Denials","icon":"XCircle","visible":true,"position":2},
                {"key":"era-remittance","label":"ERA / Remittance","icon":"FileCheck","visible":true,"position":3}
              ]},
              {"label":"General","position":4,"tabs":[
                {"key":"documents","label":"Documents","icon":"FileText","visible":true,"position":0},
                {"key":"education","label":"Education","icon":"GraduationCap","visible":true,"position":1},
                {"key":"messaging","label":"Messaging","icon":"MessageSquare","visible":true,"position":2}
              ]},
              {"label":"Financial","position":5,"tabs":[
                {"key":"billing","label":"Billing","icon":"CreditCard","visible":true,"position":0},
                {"key":"payments","label":"Payments","icon":"DollarSign","visible":true,"position":1},
                {"key":"statements","label":"Statements","icon":"FileText","visible":true,"position":2}
              ]}
            ]
            """;
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
