package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.PractitionerRoleDto;
import com.qiaben.ciyex.service.PractitionerRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practitionerRoles")
public class PractitionerRoleController {

    @Autowired
    private PractitionerRoleService service;

    // Endpoint to create a new PractitionerRole
    @PostMapping
    public PractitionerRoleDto createPractitionerRole(@RequestBody PractitionerRoleDto dto) {
        return service.createPractitionerRole(dto);
    }

    // Endpoint to get all PractitionerRoles
    @GetMapping
    public List<PractitionerRoleDto> getAllPractitionerRoles() {
        return service.getAllPractitionerRoles();
    }
}
