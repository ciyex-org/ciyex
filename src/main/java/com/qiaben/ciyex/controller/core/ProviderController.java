package com.qiaben.ciyex.controller.core;

import com.qiaben.ciyex.dto.core.ProviderDTO;
import com.qiaben.ciyex.dto.core.ServiceDTO;
import com.qiaben.ciyex.dto.core.WorkingDayDTO;
import jakarta.validation.Valid;
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
        // Handle business logic for registering a provider...
        return ResponseEntity.ok("Provider registered successfully!");
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> setSchedule(@Valid @RequestBody List<@Valid WorkingDayDTO> workingDays) {
        // handle logic
        return ResponseEntity.ok("Working days saved!");
    }

    @PostMapping("/service")
    public ResponseEntity<?> addService(@Valid @RequestBody ServiceDTO service) {
        // Save service logic here...
        return ResponseEntity.ok("Service created!");
    }
}
