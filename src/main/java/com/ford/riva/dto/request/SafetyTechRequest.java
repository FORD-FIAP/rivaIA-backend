package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SafetyTechRequest(
        @NotNull(message = "Number of airbags is required")
        @PositiveOrZero(message = "Number of airbags must be zero or positive")
        Integer numAirbags,

        @NotNull(message = "ABS is required")
        Boolean abs,

        @NotNull(message = "Stability control is required")
        Boolean stabilityControl,

        @NotNull(message = "Brake assist is required")
        Boolean brakeAssist,

        @NotNull(message = "Rear camera is required")
        Boolean rearCamera,

        @Size(max = 30, message = "Parking sensor must be at most 30 characters")
        String parkingSensor,

        @NotNull(message = "Blind spot monitor is required")
        Boolean blindSpotMonitor,

        @Size(max = 100, message = "Infotainment must be at most 100 characters")
        String infotainment,

        @Size(max = 100, message = "Phone connectivity must be at most 100 characters")
        String phoneConnectivity
) {}
