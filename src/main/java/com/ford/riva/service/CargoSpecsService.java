package com.ford.riva.service;

import com.ford.riva.dto.request.CargoSpecsRequest;
import com.ford.riva.dto.response.CargoSpecsResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.CargoSpecs;
import com.ford.riva.repository.CargoSpecsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CargoSpecsService {

    private final CargoSpecsRepository cargoSpecsRepository;
    private final VersionService versionService;

    @Transactional
    public CargoSpecsResponse createOrUpdate(Integer versionId, CargoSpecsRequest request) {
        var version = versionService.findEntityById(versionId);
        CargoSpecs c = cargoSpecsRepository.findById(versionId)
                .orElse(CargoSpecs.builder().version(version).build());
        c.setCabLength(request.cabLength()); c.setCargoCapacityKg(request.cargoCapacityKg());
        c.setTowingCapacityKg(request.towingCapacityKg());
        return toResponse(cargoSpecsRepository.save(c));
    }

    @Transactional(readOnly = true)
    public CargoSpecsResponse findByVersionId(Integer versionId) {
        versionService.findEntityById(versionId);
        return toResponse(cargoSpecsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("CargoSpecs", versionId)));
    }

    @Transactional
    public void delete(Integer versionId) {
        versionService.findEntityById(versionId);
        cargoSpecsRepository.delete(cargoSpecsRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("CargoSpecs", versionId)));
    }

    private CargoSpecsResponse toResponse(CargoSpecs c) {
        return new CargoSpecsResponse(c.getVersionId(), c.getCabLength(), c.getCargoCapacityKg(), c.getTowingCapacityKg());
    }
}
