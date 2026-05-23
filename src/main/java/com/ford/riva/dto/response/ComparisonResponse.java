package com.ford.riva.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ComparisonResponse(
        Integer comparisonId,
        Long userId,
        String title,
        LocalDateTime createdAt,
        List<VersionDetailResponse> versions
) {}
