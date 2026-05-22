package com.ford.riva.controller;

import com.ford.riva.dto.request.PowertrainRequest;
import com.ford.riva.dto.response.PowertrainResponse;
import com.ford.riva.service.PowertrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/powertrains")
@RequiredArgsConstructor
public class PowertrainController {

    private final PowertrainService powertrainService;

    @PostMapping
    public ResponseEntity<PowertrainResponse> create(@Valid @RequestBody PowertrainRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(powertrainService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PowertrainResponse>> findAll(
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) String drivetrain,
            @RequestParam(required = false) String transmission) {
        if (fuelType != null) return ResponseEntity.ok(powertrainService.findByFuelType(fuelType));
        if (drivetrain != null) return ResponseEntity.ok(powertrainService.findByDrivetrain(drivetrain));
        if (transmission != null) return ResponseEntity.ok(powertrainService.findByTransmission(transmission));
        return ResponseEntity.ok(powertrainService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PowertrainResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(powertrainService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PowertrainResponse> update(@PathVariable Integer id, @Valid @RequestBody PowertrainRequest request) {
        return ResponseEntity.ok(powertrainService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        powertrainService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
