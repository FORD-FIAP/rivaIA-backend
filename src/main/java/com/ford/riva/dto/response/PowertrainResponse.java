package com.ford.riva.dto.response;

public record PowertrainResponse(
        Integer powertrainId,
        String engine,
        Integer powerHp,
        Integer torqueNm,
        String transmission,
        String drivetrain,
        Integer tankLiters,
        String fuelType
) {}
