package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VersionRequest(
        @NotNull(message = "Vehicle ID is required")
        Integer vehicleId,

        @NotNull(message = "Powertrain ID is required")
        Integer powertrainId,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        BigDecimal price,

        @NotNull(message = "Sunroof is required")
        Boolean sunroof
) {}
