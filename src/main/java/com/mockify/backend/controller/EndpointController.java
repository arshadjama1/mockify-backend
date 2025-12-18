package com.mockify.backend.controller;

import com.mockify.backend.model.Endpoint;
import com.mockify.backend.repository.EndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/endpoints")
public class EndpointController {

    private final EndpointRepository endpointRepository;

    @GetMapping("/lookup/{slug}")
    public ResponseEntity<Map<String, Object>> lookupSlug(@PathVariable String slug) {
        return endpointRepository.findBySlug(slug)
                .map(endpoint -> {
                    String type;
                    Object resourceId;

                    if (endpoint.getOrganization() != null) {
                        type = "organization";
                        resourceId = endpoint.getOrganization().getId();
                    } else if (endpoint.getProject() != null) {
                        type = "project";
                        resourceId = endpoint.getProject().getId();
                    } else if (endpoint.getSchema() != null) {
                        type = "schema";
                        resourceId = endpoint.getSchema().getId();
                    } else {
                        type = "unknown";
                        resourceId = null;
                    }

                    return ResponseEntity.ok(Map.of(
                            "slug", slug,
                            "type", type,
                            "resourceId", resourceId,
                            "exists", true
                    ));
                })
                .orElse(ResponseEntity.ok(Map.of(
                        "slug", slug,
                        "exists", false
                )));
    }
}