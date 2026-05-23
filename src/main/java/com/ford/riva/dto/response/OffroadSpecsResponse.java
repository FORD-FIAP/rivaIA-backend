package com.ford.riva.dto.response;

public record OffroadSpecsResponse(
        Integer versionId,
        Integer approachAngle,
        Integer departureAngle,
        Integer breakoverAngle,
        Integer wadingDepth,
        String drivingModes,
        Boolean diffLock,
        Boolean hillDescentControl
) {}
