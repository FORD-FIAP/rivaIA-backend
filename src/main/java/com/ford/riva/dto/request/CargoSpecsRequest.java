package com.ford.riva.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CargoSpecsRequest(
        @Size(max = 30, message = "Cab length must be at most 30 characters")
        String cabLength,

        @Positive(message = "Cargo capacity must be positive")
        Integer cargoCapacityKg,

        @Positive(message = "Towing capacity must be positive")
        Integer towingCapacityKg
) {}
