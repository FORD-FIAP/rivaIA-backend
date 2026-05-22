package com.ford.riva.dto.response;

public record VehicleResponse(
        Integer vehicleId,
        BrandResponse brand,
        CategoryResponse category,
        String model,
        Integer modelYear,
        Integer numSeats,
        Integer numDoors
) {}
