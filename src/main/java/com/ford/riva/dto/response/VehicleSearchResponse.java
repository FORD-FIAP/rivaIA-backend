package com.ford.riva.dto.response;

import java.util.List;

public record VehicleSearchResponse(
        List<VehicleResponse> content,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
) {}
