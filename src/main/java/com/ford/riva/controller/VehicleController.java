package com.ford.riva.controller;

import com.ford.riva.dto.request.VehicleRequest;
import com.ford.riva.dto.request.VehicleSearchRequest;
import com.ford.riva.dto.response.VehicleResponse;
import com.ford.riva.dto.response.VehicleSearchResponse;
import com.ford.riva.service.SearchService;
import com.ford.riva.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request));
    }

    @GetMapping("/search")
    public ResponseEntity<VehicleSearchResponse> search(
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer modelYearMin,
            @RequestParam(required = false) Integer modelYearMax,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) String drivetrain,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        VehicleSearchRequest searchRequest = new VehicleSearchRequest(
                brandId, categoryId, model, modelYearMin, modelYearMax,
                priceMin, priceMax, fuelType, drivetrain, page, size);
        VehicleSearchResponse response = vehicleService.search(searchRequest);
        if (userId != null) {
            searchService.saveFromSearch(userId, searchRequest);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> findAll(
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer modelYear) {
        if (brandId != null) return ResponseEntity.ok(vehicleService.findByBrandId(brandId));
        if (categoryId != null) return ResponseEntity.ok(vehicleService.findByCategoryId(categoryId));
        if (model != null) return ResponseEntity.ok(vehicleService.findByModel(model));
        if (modelYear != null) return ResponseEntity.ok(vehicleService.findByModelYear(modelYear));
        return ResponseEntity.ok(vehicleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(vehicleService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(@PathVariable Integer id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
