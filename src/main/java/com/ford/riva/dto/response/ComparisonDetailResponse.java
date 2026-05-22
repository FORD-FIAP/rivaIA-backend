package com.ford.riva.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ComparisonDetailResponse(
        Integer comparisonId,
        Long userId,
        String title,
        LocalDateTime createdAt,
        List<VersionDetailResponse> versions,
        Map<String, List<Object>> differences,
        Map<String, HighlightInfo> highlights
) {
    public record HighlightInfo(Object bestValue, Integer bestVersionId) {}
}
