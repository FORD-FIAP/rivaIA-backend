package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OffroadSpecsRequest(
        @Positive(message = "Approach angle must be positive")
        Integer approachAngle,

        @Positive(message = "Departure angle must be positive")
        Integer departureAngle,

        @Positive(message = "Breakover angle must be positive")
        Integer breakoverAngle,

        @Positive(message = "Wading depth must be positive")
        Integer wadingDepth,

        @Size(max = 200, message = "Driving modes must be at most 200 characters")
        String drivingModes,

        @NotNull(message = "Diff lock is required")
        Boolean diffLock,

        @NotNull(message = "Hill descent control is required")
        Boolean hillDescentControl
) {}
