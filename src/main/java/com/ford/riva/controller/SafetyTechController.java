package com.ford.riva.controller;

import com.ford.riva.dto.request.SafetyTechRequest;
import com.ford.riva.dto.response.SafetyTechResponse;
import com.ford.riva.service.SafetyTechService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/versions/{versionId}/safety-tech")
@RequiredArgsConstructor
public class SafetyTechController {

    private final SafetyTechService safetyTechService;

    @PutMapping
    public ResponseEntity<SafetyTechResponse> createOrUpdate(
            @PathVariable Integer versionId,
            @Valid @RequestBody SafetyTechRequest request) {
        return ResponseEntity.ok(safetyTechService.createOrUpdate(versionId, request));
    }

    @GetMapping
    public ResponseEntity<SafetyTechResponse> findByVersionId(@PathVariable Integer versionId) {
        return ResponseEntity.ok(safetyTechService.findByVersionId(versionId));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Integer versionId) {
        safetyTechService.delete(versionId);
        return ResponseEntity.noContent().build();
    }
}
