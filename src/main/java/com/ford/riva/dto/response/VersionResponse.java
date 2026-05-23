package com.ford.riva.dto.response;

import java.math.BigDecimal;

public record VersionResponse(
        Integer versionId,
        Integer vehicleId,
        Integer powertrainId,
        String name,
        BigDecimal price,
        Boolean sunroof
) {}
