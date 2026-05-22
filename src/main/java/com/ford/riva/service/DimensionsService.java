package com.ford.riva.service;

import com.ford.riva.dto.request.DimensionsRequest;
import com.ford.riva.dto.response.DimensionsResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.Dimensions;
import com.ford.riva.repository.DimensionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DimensionsService {

    private final DimensionsRepository dimensionsRepository;
    private final VersionService versionService;

    @Transactional
    public DimensionsResponse createOrUpdate(Integer versionId, DimensionsRequest request) {
        var version = versionService.findEntityById(versionId);
        Dimensions d = dimensionsRepository.findById(versionId)
                .orElse(Dimensions.builder().version(version).build());
        d.setLengthMm(request.lengthMm()); d.setWidthMm(request.widthMm()); d.setHeightMm(request.heightMm());
        d.setWheelbaseMm(request.wheelbaseMm()); d.setGroundClearanceMm(request.groundClearanceMm());
        return toResponse(dimensionsRepository.save(d));
    }

    @Transactional(readOnly = true)
    public DimensionsResponse findByVersionId(Integer versionId) {
        versionService.findEntityById(versionId);
        return toResponse(dimensionsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dimensions", versionId)));
    }

    @Transactional
    public void delete(Integer versionId) {
        versionService.findEntityById(versionId);
        dimensionsRepository.delete(dimensionsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dimensions", versionId)));
    }

    private DimensionsResponse toResponse(Dimensions d) {
        return new DimensionsResponse(d.getVersionId(), d.getLengthMm(), d.getWidthMm(),
                d.getHeightMm(), d.getWheelbaseMm(), d.getGroundClearanceMm());
    }
}
