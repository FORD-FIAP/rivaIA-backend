package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SportSpecsRequest(
        @Positive(message = "Acceleration must be positive")
        BigDecimal acceleration0100,

        @Size(max = 50, message = "Brake type must be at most 50 characters")
        String brakeType,

        @Size(max = 100, message = "Driving mode must be at most 100 characters")
        String drivingMode,

        @NotNull(message = "Convertible is required")
        Boolean convertible
) {}
