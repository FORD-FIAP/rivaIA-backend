package com.ford.riva.dto.request;

import java.math.BigDecimal;

public record VehicleSearchRequest(
        Integer brandId,
        Integer categoryId,
        String model,
        Integer modelYearMin,
        Integer modelYearMax,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String fuelType,
        String drivetrain,
        Integer page,
        Integer size
) {}
