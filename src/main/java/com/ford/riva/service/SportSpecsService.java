package com.ford.riva.service;

import com.ford.riva.dto.request.SportSpecsRequest;
import com.ford.riva.dto.response.SportSpecsResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.SportSpecs;
import com.ford.riva.repository.SportSpecsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SportSpecsService {

    private final SportSpecsRepository sportSpecsRepository;
    private final VersionService versionService;

    @Transactional
    public SportSpecsResponse createOrUpdate(Integer versionId, SportSpecsRequest request) {
        var version = versionService.findEntityById(versionId);
        SportSpecs s = sportSpecsRepository.findById(versionId)
                .orElse(SportSpecs.builder().version(version).build());
        s.setAcceleration0100(request.acceleration0100()); s.setBrakeType(request.brakeType());
        s.setDrivingMode(request.drivingMode()); s.setConvertible(request.convertible());
        return toResponse(sportSpecsRepository.save(s));
    }

    @Transactional(readOnly = true)
    public SportSpecsResponse findByVersionId(Integer versionId) {
        versionService.findEntityById(versionId);
        return toResponse(sportSpecsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("SportSpecs", versionId)));
    }

    @Transactional
    public void delete(Integer versionId) {
        versionService.findEntityById(versionId);
        sportSpecsRepository.delete(sportSpecsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("SportSpecs", versionId)));
    }

    private SportSpecsResponse toResponse(SportSpecs s) {
        return new SportSpecsResponse(s.getVersionId(), s.getAcceleration0100(), s.getBrakeType(),
                s.getDrivingMode(), s.getConvertible());
    }
}
