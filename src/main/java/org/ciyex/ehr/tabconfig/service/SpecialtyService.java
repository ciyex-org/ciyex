package org.ciyex.ehr.tabconfig.service;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.tabconfig.entity.Specialty;
import org.ciyex.ehr.tabconfig.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepo;

    public List<Specialty> listSpecialties(String orgId) {
        return specialtyRepo.findAllForOrg(orgId);
    }

    public Optional<Specialty> getSpecialty(String code, String orgId) {
        return specialtyRepo.findByCodeForOrg(code, orgId);
    }

    @Transactional
    public Specialty createSpecialty(Specialty specialty) {
        return specialtyRepo.save(specialty);
    }

    @Transactional
    public Specialty updateSpecialty(String code, String orgId, Specialty update) {
        Specialty spec = specialtyRepo.findByCodeForOrg(code, orgId)
                .orElseThrow(() -> new NoSuchElementException("Specialty not found: " + code));
        spec.setName(update.getName());
        spec.setDescription(update.getDescription());
        spec.setIcon(update.getIcon());
        spec.setParentCode(update.getParentCode());
        return specialtyRepo.save(spec);
    }

    @Transactional
    public void deleteSpecialty(String code, String orgId) {
        Specialty spec = specialtyRepo.findByCodeForOrg(code, orgId)
                .orElseThrow(() -> new NoSuchElementException("Specialty not found: " + code));
        spec.setActive(false);
        specialtyRepo.save(spec);
    }
}
