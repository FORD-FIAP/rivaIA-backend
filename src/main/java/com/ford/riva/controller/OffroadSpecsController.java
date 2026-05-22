package com.ford.riva.controller;

import com.ford.riva.dto.request.OffroadSpecsRequest;
import com.ford.riva.dto.response.OffroadSpecsResponse;
import com.ford.riva.service.OffroadSpecsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/versions/{versionId}/offroad-specs")
@RequiredArgsConstructor
public class OffroadSpecsController {

    private final OffroadSpecsService offroadSpecsService;

    @PutMapping
    public ResponseEntity<OffroadSpecsResponse> createOrUpdate(
            @PathVariable Integer versionId,
            @Valid @RequestBody OffroadSpecsRequest request) {
        return ResponseEntity.ok(offroadSpecsService.createOrUpdate(versionId, request));
    }

    @GetMapping
    public ResponseEntity<OffroadSpecsResponse> findByVersionId(@PathVariable Integer versionId) {
        return ResponseEntity.ok(offroadSpecsService.findByVersionId(versionId));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Integer versionId) {
        offroadSpecsService.delete(versionId);
        return ResponseEntity.noContent().build();
    }
}
