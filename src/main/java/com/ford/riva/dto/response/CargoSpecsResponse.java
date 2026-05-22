package com.ford.riva.dto.response;

public record CargoSpecsResponse(
        Integer versionId,
        String cabLength,
        Integer cargoCapacityKg,
        Integer towingCapacityKg
) {}
