package com.ford.riva.controller;

import com.ford.riva.dto.request.DimensionsRequest;
import com.ford.riva.dto.response.DimensionsResponse;
import com.ford.riva.service.DimensionsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/versions/{versionId}/dimensions")
@RequiredArgsConstructor
public class DimensionsController {

    private final DimensionsService dimensionsService;

    @PutMapping
    public ResponseEntity<DimensionsResponse> createOrUpdate(
            @PathVariable Integer versionId,
            @Valid @RequestBody DimensionsRequest request) {
        return ResponseEntity.ok(dimensionsService.createOrUpdate(versionId, request));
    }

    @GetMapping
    public ResponseEntity<DimensionsResponse> findByVersionId(@PathVariable Integer versionId) {
        return ResponseEntity.ok(dimensionsService.findByVersionId(versionId));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Integer versionId) {
        dimensionsService.delete(versionId);
        return ResponseEntity.noContent().build();
    }
}
