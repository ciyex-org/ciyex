package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.service.telnyx.PushCredentialDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/push-credentials")
@RequiredArgsConstructor
public class PushCredentialDeleteController {

    private final PushCredentialDeleteService deleteService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePushCredential(@PathVariable("id") String id) {
        deleteService.deletePushCredentialById(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}

