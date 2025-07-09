package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.LocationResponseDTO;
import com.qiaben.ciyex.service.OpenEmrFhirLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fhir")
@RequiredArgsConstructor
public class OpenEmrFhirLocationController {

    private final OpenEmrFhirLocationService locationService;

    // Endpoint to fetch a list of locations based on optional query parameters (_id, _lastUpdated)
    @GetMapping("/Location")
    public ResponseEntity<List<LocationResponseDTO>> getLocations(@RequestParam(required = false) String _id,
                                                                  @RequestParam(required = false) String _lastUpdated) {
        // Prepare the query parameters as a map for flexibility
        Map<String, String> queryParams = Map.of(
                "_id", _id,
                "_lastUpdated", _lastUpdated
        );

        // Get the list of locations from the service
        List<LocationResponseDTO> locations = locationService.getLocations(queryParams);

        return ResponseEntity.ok(locations); // Return the list of locations
    }

    // Endpoint to fetch a single location by UUID
    @GetMapping("/Location/{uuid}")
    public ResponseEntity<LocationResponseDTO> getLocation(@PathVariable String uuid) {
        try {
            // Get a single location from the service using the UUID
            LocationResponseDTO locationResponse = locationService.getLocation(uuid);

            if (locationResponse == null) {
                // If location not found, return 404
                return ResponseEntity.status(404).body(null); // or create a custom 404 response
            }

            return ResponseEntity.ok(locationResponse); // Return the single location response

        } catch (Exception e) {
            // If there's any error in fetching the location (e.g., 400 or 401)
            return ResponseEntity.badRequest().body(null); // Return 400 on failure
        }
    }
}
