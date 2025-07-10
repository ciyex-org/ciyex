package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirDeviceDTO;
import com.qiaben.ciyex.service.FhirDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fhir/Device")
@RequiredArgsConstructor
public class FhirDeviceController {

    private final FhirDeviceService deviceService;

    @GetMapping
    public List<FhirDeviceDTO> getDevices(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient) {
        return deviceService.getDevices(_id, _lastUpdated, patient);
    }

    @GetMapping("/{uuid}")
    public FhirDeviceDTO getDeviceById(@PathVariable String uuid) {
        return deviceService.getDeviceById(uuid);
    }
}

