package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PowertrainRequest(
        @NotBlank(message = "Engine is required")
        @Size(max = 100, message = "Engine must be at most 100 characters")
        String engine,

        @NotNull(message = "Power HP is required")
        @Positive(message = "Power HP must be positive")
        Integer powerHp,

        @NotNull(message = "Torque NM is required")
        @Positive(message = "Torque NM must be positive")
        Integer torqueNm,

        @NotBlank(message = "Transmission is required")
        @Size(max = 50, message = "Transmission must be at most 50 characters")
        String transmission,

        @NotBlank(message = "Drivetrain is required")
        @Size(max = 20, message = "Drivetrain must be at most 20 characters")
        String drivetrain,

        @NotNull(message = "Tank liters is required")
        @Positive(message = "Tank liters must be positive")
        Integer tankLiters,

        @NotBlank(message = "Fuel type is required")
        @Size(max = 30, message = "Fuel type must be at most 30 characters")
        String fuelType
) {}
