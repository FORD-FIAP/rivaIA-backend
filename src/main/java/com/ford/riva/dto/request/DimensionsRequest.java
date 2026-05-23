package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DimensionsRequest(
        @NotNull(message = "Length is required")
        @Positive(message = "Length must be positive")
        Integer lengthMm,

        @NotNull(message = "Width is required")
        @Positive(message = "Width must be positive")
        Integer widthMm,

        @NotNull(message = "Height is required")
        @Positive(message = "Height must be positive")
        Integer heightMm,

        @NotNull(message = "Wheelbase is required")
        @Positive(message = "Wheelbase must be positive")
        Integer wheelbaseMm,

        @NotNull(message = "Ground clearance is required")
        @Positive(message = "Ground clearance must be positive")
        Integer groundClearanceMm
) {}
