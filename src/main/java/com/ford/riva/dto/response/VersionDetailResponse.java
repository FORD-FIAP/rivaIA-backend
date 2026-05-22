package com.ford.riva.dto.response;

import java.math.BigDecimal;

public record VersionDetailResponse(
        Integer versionId,
        VehicleResponse vehicle,
        PowertrainResponse powertrain,
        String name,
        BigDecimal price,
        Boolean sunroof,
        DimensionsResponse dimensions,
        SafetyTechResponse safetyTech,
        SportSpecsResponse sportSpecs,
        OffroadSpecsResponse offroadSpecs,
        CargoSpecsResponse cargoSpecs
) {}
