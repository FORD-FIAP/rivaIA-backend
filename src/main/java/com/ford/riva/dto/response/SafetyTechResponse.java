package com.ford.riva.dto.response;

public record SafetyTechResponse(
        Integer versionId,
        Integer numAirbags,
        Boolean abs,
        Boolean stabilityControl,
        Boolean brakeAssist,
        Boolean rearCamera,
        String parkingSensor,
        Boolean blindSpotMonitor,
        String infotainment,
        String phoneConnectivity
) {}
