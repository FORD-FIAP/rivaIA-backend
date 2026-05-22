package com.ford.riva.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ComparisonRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotNull(message = "Version IDs are required")
        @Size(min = 2, max = 10, message = "Must provide between 2 and 10 version IDs")
        List<Integer> versionIds
) {}
