package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.ExternalConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/external-connections")
@RequiredArgsConstructor
public class ExternalConnectionController {

    private final ExternalConnectionService service;

    @GetMapping
    public ExternalConnectionListResponseDTO getExternalConnections(
            @RequestParam(defaultValue = "1", name = "pageNumber") Integer pageNumber,
            @RequestParam(defaultValue = "250", name = "pageSize") Integer pageSize,
            @RequestParam(required = false) String connectionNameContains,
            @RequestParam(required = false) String externalSipConnection,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String createdAt,
            @RequestParam(required = false) String phoneNumberEq
    ) {
        return service.listExternalConnections(
                pageNumber,
                pageSize,
                connectionNameContains,
                externalSipConnection,
                id,
                createdAt,
                phoneNumberEq
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalConnectionDTO create(@RequestBody ExternalConnectionDTO request) {
        return service.createExternalConnection(request);
    }

    @GetMapping("/{id}")
    public ExternalConnectionDTO getById(@PathVariable String id) {
        return service.getExternalConnectionById(id);
    }

    @PatchMapping("/{id}")
    public ExternalConnectionDTO updateConnection(
            @PathVariable String id,
            @RequestBody ExternalConnectionDTO updateRequest
    ) {
        return service.updateExternalConnection(id, updateRequest);
    }

    @DeleteMapping("/{id}")
    public ExternalConnectionDTO deleteConnection(@PathVariable String id) {
        return service.deleteExternalConnection(id);
    }

    @GetMapping("/{id}/civic-addresses")
    public CivicAddressListResponseDTO listCivicAddresses(
            @PathVariable String id,
            @RequestParam(required = false, name = "filter[country]") List<String> countries
    ) {
        return service.getCivicAddresses(id, countries);
    }

    @GetMapping("/{id}/civic-addresses/{addressId}")
    public CivicAddressListResponseDTO.CivicAddress getCivicAddressById(
            @PathVariable String id,
            @PathVariable String addressId
    ) {
        return service.getCivicAddressById(id, addressId);
    }
    @PostMapping("/{id}/uploads")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UploadResponseDTO createUpload(
            @PathVariable String id,
            @RequestBody UploadRequestDTO request
    ) {
        return service.createUploadRequest(id, request);
    }

    @GetMapping("/{id}/uploads")
    public UploadListResponseDTO listUploads(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "250") Integer pageSize,
            @RequestParam(required = false, name = "filter[status][eq]") String statusEq,
            @RequestParam(required = false, name = "filter[civic_address_id][eq]") String civicAddressIdEq,
            @RequestParam(required = false, name = "filter[location_id][eq]") String locationIdEq,
            @RequestParam(required = false, name = "filter[phone_number][eq]") String phoneNumberEq,
            @RequestParam(required = false, name = "filter[phone_number][contains]") String phoneNumberContains
    ) {
        return service.listUploadRequests(
                id, pageNumber, pageSize,
                statusEq, civicAddressIdEq,
                locationIdEq, phoneNumberEq, phoneNumberContains
        );
    }
    @PostMapping("/{id}/uploads/refresh")
    public Map<String, Boolean> refreshUploadStatus(@PathVariable String id) {
        boolean result = service.refreshUploadStatus(id);
        return Map.of("success", result);
    }
    @GetMapping("/{id}/uploads/status")
    public UploadStatusCountResponseDTO getUploadStatusCount(@PathVariable String id) {
        return service.getUploadStatusCount(id);
    }

    @GetMapping("/{id}/uploads/{ticketId}")
    public UploadRequestDetailResponseDTO getUploadRequestDetail(
            @PathVariable String id,
            @PathVariable String ticketId
    ) {
        return service.getUploadRequestDetail(id, ticketId);
    }

    @PostMapping("/{id}/uploads/{ticketId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UploadRequestDetailResponseDTO retryUploadRequest(
            @PathVariable("id") Long externalConnectionId,
            @PathVariable("ticketId") String ticketId
    ) {
        return service.retryUploadRequest(externalConnectionId, ticketId);
    }

    @GetMapping("/{id}/releases")
    public ReleaseListResponseDTO listReleases(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "250") Integer pageSize,
            @RequestParam(required = false, name = "filter[status][eq]") String statusEq,
            @RequestParam(required = false, name = "filter[civic_address_id][eq]") String civicAddressIdEq,
            @RequestParam(required = false, name = "filter[location_id][eq]") String locationIdEq,
            @RequestParam(required = false, name = "filter[phone_number][eq]") String phoneNumberEq,
            @RequestParam(required = false, name = "filter[phone_number][contains]") String phoneNumberContains
    ) {
        return service.listReleases(
                id, pageNumber, pageSize, statusEq,
                civicAddressIdEq, locationIdEq,
                phoneNumberEq, phoneNumberContains
        );
    }
    @GetMapping("/{id}/releases/{releaseId}")
    public ReleaseListResponseDTO.Release getReleaseById(
            @PathVariable String id,
            @PathVariable String releaseId
    ) {
        return service.getReleaseById(id, releaseId);
    }
    @GetMapping("/{id}/phone-numbers")
    public PhoneNumberListResponseDTO listPhoneNumbers(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "250") Integer pageSize,
            @RequestParam(required = false, name = "filter[phone_number][eq]") String phoneNumberEq,
            @RequestParam(required = false, name = "filter[phone_number][contains]") String phoneNumberContains,
            @RequestParam(required = false, name = "filter[civic_address_id][eq]") String civicAddressId,
            @RequestParam(required = false, name = "filter[location_id][eq]") String locationId
    ) {
        return service.listPhoneNumbers(id, pageNumber, pageSize, phoneNumberEq, phoneNumberContains, civicAddressId, locationId);
    }

    @GetMapping("/{id}/phone-numbers/{phoneNumberId}")
    public PhoneNumberListResponseDTO.PhoneNumber getPhoneNumberById(
            @PathVariable("id") String externalConnectionId,
            @PathVariable("phoneNumberId") String phoneNumberId
    ) {
        return service.getPhoneNumberById(externalConnectionId, phoneNumberId);
    }
    @PatchMapping("/{id}/phone_numbers/{phoneNumberId}")
    public PhoneNumberListResponseDTO.PhoneNumber updatePhoneNumber(
            @PathVariable String id,
            @PathVariable String phoneNumberId,
            @RequestBody PhoneNumberRequestDTO request
    ) {
        return service.updatePhoneNumber(id, phoneNumberId, request);
    }

    @PatchMapping("/{id}/locations/{locationId}")
    public CivicAddressListResponseDTO.Location updateLocationEmergencyAddress(
            @PathVariable String id,
            @PathVariable String locationId,
            @RequestBody Map<String, String> body
    ) {
        return service.updateStaticEmergencyAddress(id, locationId, body.get("static_emergency_address_id"));
    }

    @PostMapping("/operator-connect/actions/refresh")
    public OperatorConnectRefreshResponseDTO refreshOperatorConnect() {
        return service.refreshOperatorConnectIntegration();
    }


}
