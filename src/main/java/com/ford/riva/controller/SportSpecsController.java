package com.ford.riva.controller;

import com.ford.riva.dto.request.SportSpecsRequest;
import com.ford.riva.dto.response.SportSpecsResponse;
import com.ford.riva.service.SportSpecsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/versions/{versionId}/sport-specs")
@RequiredArgsConstructor
public class SportSpecsController {

    private final SportSpecsService sportSpecsService;

    @PutMapping
    public ResponseEntity<SportSpecsResponse> createOrUpdate(
            @PathVariable Integer versionId,
            @Valid @RequestBody SportSpecsRequest request) {
        return ResponseEntity.ok(sportSpecsService.createOrUpdate(versionId, request));
    }

    @GetMapping
    public ResponseEntity<SportSpecsResponse> findByVersionId(@PathVariable Integer versionId) {
        return ResponseEntity.ok(sportSpecsService.findByVersionId(versionId));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Integer versionId) {
        sportSpecsService.delete(versionId);
        return ResponseEntity.noContent().build();
    }
}
