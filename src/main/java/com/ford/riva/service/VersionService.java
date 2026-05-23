package com.ford.riva.service;

import com.ford.riva.dto.request.VersionRequest;
import com.ford.riva.dto.response.*;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.*;
import com.ford.riva.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final VersionRepository versionRepository;
    private final VehicleService vehicleService;
    private final PowertrainService powertrainService;

    @Transactional
    public VersionResponse create(VersionRequest request) {
        Version version = Version.builder()
                .vehicle(vehicleService.findEntityById(request.vehicleId()))
                .powertrain(powertrainService.findEntityById(request.powertrainId()))
                .name(request.name()).price(request.price()).sunroof(request.sunroof())
                .build();
        return toResponse(versionRepository.save(version));
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> findAll() {
        return versionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> findByVehicleId(Integer vehicleId) {
        return versionRepository.findByVehicleVehicleId(vehicleId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> findByPowertrainId(Integer powertrainId) {
        return versionRepository.findByPowertrainPowertrainId(powertrainId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VersionResponse findById(Integer id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public VersionResponse update(Integer id, VersionRequest request) {
        Version version = findEntityById(id);
        version.setVehicle(vehicleService.findEntityById(request.vehicleId()));
        version.setPowertrain(powertrainService.findEntityById(request.powertrainId()));
        version.setName(request.name()); version.setPrice(request.price()); version.setSunroof(request.sunroof());
        return toResponse(versionRepository.save(version));
    }

    @Transactional
    public void delete(Integer id) {
        Version version = findEntityById(id);
        if (!version.getComparisons().isEmpty()) {
            throw new IllegalStateException("Cannot delete version with associated comparisons");
        }
        versionRepository.delete(version);
    }

    public Version findEntityById(Integer id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Version", id));
    }

    private VersionResponse toResponse(Version v) {
        return new VersionResponse(v.getVersionId(), v.getVehicle().getVehicleId(),
                v.getPowertrain().getPowertrainId(), v.getName(), v.getPrice(), v.getSunroof());
    }

    public VersionDetailResponse toDetailResponse(Version v) {
        Powertrain p = v.getPowertrain();
        Dimensions d = v.getDimensions();
        SafetyTech s = v.getSafetyTech();
        SportSpecs sp = v.getSportSpecs();
        OffroadSpecs o = v.getOffroadSpecs();
        CargoSpecs c = v.getCargoSpecs();

        return new VersionDetailResponse(
                v.getVersionId(),
                vehicleService.toResponse(v.getVehicle()),
                new PowertrainResponse(p.getPowertrainId(), p.getEngine(), p.getPowerHp(), p.getTorqueNm(),
                        p.getTransmission(), p.getDrivetrain(), p.getTankLiters(), p.getFuelType()),
                v.getName(), v.getPrice(), v.getSunroof(),
                d == null ? null : new DimensionsResponse(d.getVersionId(), d.getLengthMm(), d.getWidthMm(),
                        d.getHeightMm(), d.getWheelbaseMm(), d.getGroundClearanceMm()),
                s == null ? null : new SafetyTechResponse(s.getVersionId(), s.getNumAirbags(), s.getAbs(),
                        s.getStabilityControl(), s.getBrakeAssist(), s.getRearCamera(), s.getParkingSensor(),
                        s.getBlindSpotMonitor(), s.getInfotainment(), s.getPhoneConnectivity()),
                sp == null ? null : new SportSpecsResponse(sp.getVersionId(), sp.getAcceleration0100(),
                        sp.getBrakeType(), sp.getDrivingMode(), sp.getConvertible()),
                o == null ? null : new OffroadSpecsResponse(o.getVersionId(), o.getApproachAngle(),
                        o.getDepartureAngle(), o.getBreakoverAngle(), o.getWadingDepth(), o.getDrivingModes(),
                        o.getDiffLock(), o.getHillDescentControl()),
                c == null ? null : new CargoSpecsResponse(c.getVersionId(), c.getCabLength(),
                        c.getCargoCapacityKg(), c.getTowingCapacityKg())
        );
    }
}
