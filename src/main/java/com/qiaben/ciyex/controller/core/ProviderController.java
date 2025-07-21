package com.qiaben.ciyex.controller.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.ProviderDTO;
import com.qiaben.ciyex.dto.core.ServiceDTO;
import com.qiaben.ciyex.dto.core.WorkingDayDTO;
import com.qiaben.ciyex.service.core.ProviderService;
import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/provider")
public class ProviderController {

    @PostMapping("/register")
    public ResponseEntity<?> registerProvider(@Valid @RequestBody ProviderDTO providerDTO) {
        ApiResponse<?> response = ProviderService.registerProvider(providerDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> setSchedule(@Valid @RequestBody List<@Valid WorkingDayDTO> workingDays) {
        ApiResponse<?> response = ProviderService.saveSchedule(workingDays);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/service")
    public ResponseEntity<?> addService(@Valid @RequestBody ServiceDTO service) {
        com.qiaben.ciyex.entity.ProviderService saved = ProviderService.ProviderServiceService.saveService(service);
        return ResponseEntity.ok(saved);
    }
}
