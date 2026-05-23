package com.ford.riva.controller;

import com.ford.riva.dto.request.VersionRequest;
import com.ford.riva.dto.response.ComparisonDetailResponse;
import com.ford.riva.dto.response.VersionResponse;
import com.ford.riva.service.ComparisonService;
import com.ford.riva.service.VersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;
    private final ComparisonService comparisonService;

    @GetMapping("/compare")
    public ResponseEntity<ComparisonDetailResponse> compare(
            @RequestParam List<Integer> ids,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(comparisonService.compareVersions(ids, userId));
    }

    @PostMapping
    public ResponseEntity<VersionResponse> create(@Valid @RequestBody VersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<VersionResponse>> findAll(
            @RequestParam(required = false) Integer vehicleId,
            @RequestParam(required = false) Integer powertrainId) {
        if (vehicleId != null) return ResponseEntity.ok(versionService.findByVehicleId(vehicleId));
        if (powertrainId != null) return ResponseEntity.ok(versionService.findByPowertrainId(powertrainId));
        return ResponseEntity.ok(versionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VersionResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(versionService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VersionResponse> update(@PathVariable Integer id, @Valid @RequestBody VersionRequest request) {
        return ResponseEntity.ok(versionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        versionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
