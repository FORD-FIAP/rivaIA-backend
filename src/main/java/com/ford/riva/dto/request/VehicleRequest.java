package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotNull(message = "Brand ID is required")
        Integer brandId,

        @NotNull(message = "Category ID is required")
        Integer categoryId,

        @NotBlank(message = "Model is required")
        @Size(max = 100, message = "Model must be at most 100 characters")
        String model,

        @NotNull(message = "Model year is required")
        @Positive(message = "Model year must be positive")
        Integer modelYear,

        @NotNull(message = "Number of seats is required")
        @Positive(message = "Number of seats must be positive")
        Integer numSeats,

        @NotNull(message = "Number of doors is required")
        @Positive(message = "Number of doors must be positive")
        Integer numDoors
) {}
