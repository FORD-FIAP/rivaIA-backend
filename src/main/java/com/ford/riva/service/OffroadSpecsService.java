package com.ford.riva.service;

import com.ford.riva.dto.request.OffroadSpecsRequest;
import com.ford.riva.dto.response.OffroadSpecsResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.OffroadSpecs;
import com.ford.riva.repository.OffroadSpecsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OffroadSpecsService {

    private final OffroadSpecsRepository offroadSpecsRepository;
    private final VersionService versionService;

    @Transactional
    public OffroadSpecsResponse createOrUpdate(Integer versionId, OffroadSpecsRequest request) {
        var version = versionService.findEntityById(versionId);
        OffroadSpecs o = offroadSpecsRepository.findById(versionId)
                .orElse(OffroadSpecs.builder().version(version).build());
        o.setApproachAngle(request.approachAngle()); o.setDepartureAngle(request.departureAngle());
        o.setBreakoverAngle(request.breakoverAngle()); o.setWadingDepth(request.wadingDepth());
        o.setDrivingModes(request.drivingModes()); o.setDiffLock(request.diffLock());
        o.setHillDescentControl(request.hillDescentControl());
        return toResponse(offroadSpecsRepository.save(o));
    }

    @Transactional(readOnly = true)
    public OffroadSpecsResponse findByVersionId(Integer versionId) {
        versionService.findEntityById(versionId);
        return toResponse(offroadSpecsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("OffroadSpecs", versionId)));
    }

    @Transactional
    public void delete(Integer versionId) {
        versionService.findEntityById(versionId);
        offroadSpecsRepository.delete(offroadSpecsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("OffroadSpecs", versionId)));
    }

    private OffroadSpecsResponse toResponse(OffroadSpecs o) {
        return new OffroadSpecsResponse(o.getVersionId(), o.getApproachAngle(), o.getDepartureAngle(),
                o.getBreakoverAngle(), o.getWadingDepth(), o.getDrivingModes(), o.getDiffLock(), o.getHillDescentControl());
    }
}
