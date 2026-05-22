package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotNull;

public record SearchRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Filters are required")
        String filters
) {}
