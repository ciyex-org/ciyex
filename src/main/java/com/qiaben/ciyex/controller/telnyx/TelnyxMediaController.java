package com.qiaben.ciyex.controller.telnyx;


import com.qiaben.ciyex.dto.telnyx.TelnyxMediaDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxMediaListResponse;
import com.qiaben.ciyex.dto.telnyx.TelnyxMediaUploadRequest;
import com.qiaben.ciyex.service.telnyx.TelnyxMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/media")
@RequiredArgsConstructor
public class TelnyxMediaController {

    private final TelnyxMediaService mediaService;

    @GetMapping
    public ResponseEntity<TelnyxMediaListResponse> listMedia(@RequestParam(required = false) String contentType) {
        return ResponseEntity.ok(mediaService.listMedia(contentType));
    }

    @PostMapping
    public ResponseEntity<TelnyxMediaDTO> uploadMedia(@RequestBody TelnyxMediaUploadRequest request) {
        return ResponseEntity.ok(mediaService.uploadMedia(request));
    }

    @GetMapping("/{mediaName}")
    public ResponseEntity<TelnyxMediaDTO> getMedia(@PathVariable String mediaName) {
        return ResponseEntity.ok(mediaService.retrieveMedia(mediaName));
    }

    @PutMapping("/{mediaName}")
    public ResponseEntity<TelnyxMediaDTO> updateMedia(@PathVariable String mediaName,
                                                      @RequestBody TelnyxMediaUploadRequest request) {
        return ResponseEntity.ok(mediaService.updateMedia(mediaName, request));
    }

    @DeleteMapping("/{mediaName}")
    public ResponseEntity<Void> deleteMedia(@PathVariable String mediaName) {
        mediaService.deleteMedia(mediaName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{mediaName}/download")
    public ResponseEntity<byte[]> downloadMedia(@PathVariable String mediaName) {
        byte[] data = mediaService.downloadMedia(mediaName);
        return ResponseEntity.ok().body(data);
    }
}
