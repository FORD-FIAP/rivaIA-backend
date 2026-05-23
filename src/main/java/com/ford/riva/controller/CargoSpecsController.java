package com.ford.riva.controller;

import com.ford.riva.dto.request.CargoSpecsRequest;
import com.ford.riva.dto.response.CargoSpecsResponse;
import com.ford.riva.service.CargoSpecsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/versions/{versionId}/cargo-specs")
@RequiredArgsConstructor
public class CargoSpecsController {

    private final CargoSpecsService cargoSpecsService;

    @PutMapping
    public ResponseEntity<CargoSpecsResponse> createOrUpdate(
            @PathVariable Integer versionId,
            @Valid @RequestBody CargoSpecsRequest request) {
        return ResponseEntity.ok(cargoSpecsService.createOrUpdate(versionId, request));
    }

    @GetMapping
    public ResponseEntity<CargoSpecsResponse> findByVersionId(@PathVariable Integer versionId) {
        return ResponseEntity.ok(cargoSpecsService.findByVersionId(versionId));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Integer versionId) {
        cargoSpecsService.delete(versionId);
        return ResponseEntity.noContent().build();
    }
}
