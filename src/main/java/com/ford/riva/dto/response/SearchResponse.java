package com.ford.riva.dto.response;

import java.time.LocalDateTime;

public record SearchResponse(
        Integer searchId,
        Long userId,
        String filters,
        LocalDateTime createdAt
) {}
