package com.ford.riva.service;

import com.ford.riva.dto.request.SafetyTechRequest;
import com.ford.riva.dto.response.SafetyTechResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.SafetyTech;
import com.ford.riva.repository.SafetyTechRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SafetyTechService {

    private final SafetyTechRepository safetyTechRepository;
    private final VersionService versionService;

    @Transactional
    public SafetyTechResponse createOrUpdate(Integer versionId, SafetyTechRequest request) {
        var version = versionService.findEntityById(versionId);
        SafetyTech s = safetyTechRepository.findById(versionId)
                .orElse(SafetyTech.builder().version(version).build());
        s.setNumAirbags(request.numAirbags()); s.setAbs(request.abs()); s.setStabilityControl(request.stabilityControl());
        s.setBrakeAssist(request.brakeAssist()); s.setRearCamera(request.rearCamera());
        s.setParkingSensor(request.parkingSensor()); s.setBlindSpotMonitor(request.blindSpotMonitor());
        s.setInfotainment(request.infotainment()); s.setPhoneConnectivity(request.phoneConnectivity());
        return toResponse(safetyTechRepository.save(s));
    }

    @Transactional(readOnly = true)
    public SafetyTechResponse findByVersionId(Integer versionId) {
        versionService.findEntityById(versionId);
        return toResponse(safetyTechRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("SafetyTech", versionId)));
    }

    @Transactional
    public void delete(Integer versionId) {
        versionService.findEntityById(versionId);
        safetyTechRepository.delete(safetyTechRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("SafetyTech", versionId)));
    }

    private SafetyTechResponse toResponse(SafetyTech s) {
        return new SafetyTechResponse(s.getVersionId(), s.getNumAirbags(), s.getAbs(), s.getStabilityControl(),
                s.getBrakeAssist(), s.getRearCamera(), s.getParkingSensor(), s.getBlindSpotMonitor(),
                s.getInfotainment(), s.getPhoneConnectivity());
    }
}
