package com.ford.riva.dto.response;

import java.math.BigDecimal;

public record SportSpecsResponse(
        Integer versionId,
        BigDecimal acceleration0100,
        String brakeType,
        String drivingMode,
        Boolean convertible
) {}
