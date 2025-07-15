package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.GroupResponseDTO;
import com.qiaben.ciyex.service.fhir.OpenEmrFhirGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fhir")
@RequiredArgsConstructor
public class OpenEmrFhirGroupController {

    private final OpenEmrFhirGroupService groupService;

    @GetMapping("/Group")
    public ResponseEntity<GroupResponseDTO> getGroup(@RequestParam Map<String, String> queryParams) {
        GroupResponseDTO response = groupService.getGroup(queryParams);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/Group/{uuid}")
    public ResponseEntity<GroupResponseDTO> getGroupByUuid(@PathVariable String uuid) {
        GroupResponseDTO response = groupService.getGroupByUuid(uuid);
        return ResponseEntity.ok(response);
    }

    // Endpoint to handle Group export request
    @GetMapping("/Group/{id}/$export")
    public ResponseEntity<String> exportGroup(@PathVariable String id) {
        ResponseEntity<String> response = groupService.exportGroup(id);
        return response;
    }
}
