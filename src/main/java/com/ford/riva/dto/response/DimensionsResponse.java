package com.ford.riva.dto.response;

public record DimensionsResponse(
        Integer versionId,
        Integer lengthMm,
        Integer widthMm,
        Integer heightMm,
        Integer wheelbaseMm,
        Integer groundClearanceMm
) {}
