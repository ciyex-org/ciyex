package org.ciyex.ehr.portal.service;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.portal.entity.PortalForm;
import org.ciyex.ehr.portal.repository.PortalFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Handles portal forms (onboarding/consent/intake).
 * Portal config (enabled, branding, features) is handled by PortalService.
 */
@Service
@RequiredArgsConstructor
public class PortalConfigService {

    private final PortalFormRepository formRepo;

    // ─── Portal Forms (Onboarding/Consent/Intake) ───

    public List<PortalForm> getAllForms(String orgAlias) {
        var orgForms = formRepo.findByOrgAliasOrderByPosition(orgAlias);
        if (!orgForms.isEmpty()) return orgForms;
        return formRepo.findByOrgAliasOrderByPosition("__DEFAULT__");
    }

    public List<PortalForm> getFormsByType(String orgAlias, String formType) {
        var orgForms = formRepo.findByOrgAliasAndFormTypeOrderByPosition(orgAlias, formType);
        if (!orgForms.isEmpty()) return orgForms;
        return formRepo.findByOrgAliasAndFormTypeOrderByPosition("__DEFAULT__", formType);
    }

    public List<PortalForm> getActiveForms(String orgAlias) {
        var orgForms = formRepo.findByOrgAliasAndActiveOrderByPosition(orgAlias, true);
        if (!orgForms.isEmpty()) return orgForms;
        return formRepo.findByOrgAliasAndActiveOrderByPosition("__DEFAULT__", true);
    }

    public List<PortalForm> getActiveFormsByType(String orgAlias, String formType) {
        var orgForms = formRepo.findByOrgAliasAndFormTypeAndActiveOrderByPosition(orgAlias, formType, true);
        if (!orgForms.isEmpty()) return orgForms;
        return formRepo.findByOrgAliasAndFormTypeAndActiveOrderByPosition("__DEFAULT__", formType, true);
    }

    public Optional<PortalForm> getFormByKey(String orgAlias, String formKey) {
        return formRepo.findByOrgAliasAndFormKey(orgAlias, formKey)
                .or(() -> formRepo.findByOrgAliasAndFormKey("__DEFAULT__", formKey));
    }

    @Transactional
    public PortalForm saveForm(String orgAlias, PortalForm form) {
        var existing = formRepo.findByOrgAliasAndFormKey(orgAlias, form.getFormKey());
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setTitle(form.getTitle());
            entity.setDescription(form.getDescription());
            entity.setFormType(form.getFormType());
            entity.setFieldConfig(form.getFieldConfig());
            entity.setSettings(form.getSettings());
            entity.setActive(form.isActive());
            entity.setPosition(form.getPosition());
            return formRepo.save(entity);
        }
        form.setOrgAlias(orgAlias);
        return formRepo.save(form);
    }

    @Transactional
    public void deleteForm(String orgAlias, Long formId) {
        formRepo.findById(formId).ifPresent(f -> {
            if (f.getOrgAlias().equals(orgAlias)) {
                formRepo.delete(f);
            }
        });
    }

    @Transactional
    public PortalForm toggleForm(String orgAlias, Long formId, boolean active) {
        var form = formRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));
        if ("__DEFAULT__".equals(form.getOrgAlias())) {
            var clone = PortalForm.builder()
                    .orgAlias(orgAlias)
                    .formKey(form.getFormKey())
                    .formType(form.getFormType())
                    .title(form.getTitle())
                    .description(form.getDescription())
                    .fieldConfig(form.getFieldConfig())
                    .settings(form.getSettings())
                    .active(active)
                    .position(form.getPosition())
                    .build();
            return formRepo.save(clone);
        }
        form.setActive(active);
        return formRepo.save(form);
    }
}
