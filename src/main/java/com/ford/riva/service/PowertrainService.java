package com.ford.riva.service;

import com.ford.riva.dto.request.PowertrainRequest;
import com.ford.riva.dto.response.PowertrainResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.Powertrain;
import com.ford.riva.repository.PowertrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PowertrainService {

    private final PowertrainRepository powertrainRepository;

    @Transactional
    public PowertrainResponse create(PowertrainRequest request) {
        Powertrain p = Powertrain.builder()
                .engine(request.engine()).powerHp(request.powerHp()).torqueNm(request.torqueNm())
                .transmission(request.transmission()).drivetrain(request.drivetrain())
                .tankLiters(request.tankLiters()).fuelType(request.fuelType()).build();
        return toResponse(powertrainRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PowertrainResponse> findAll() {
        return powertrainRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PowertrainResponse> findByFuelType(String fuelType) {
        return powertrainRepository.findByFuelType(fuelType).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PowertrainResponse> findByDrivetrain(String drivetrain) {
        return powertrainRepository.findByDrivetrain(drivetrain).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PowertrainResponse> findByTransmission(String transmission) {
        return powertrainRepository.findByTransmission(transmission).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PowertrainResponse findById(Integer id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public PowertrainResponse update(Integer id, PowertrainRequest request) {
        Powertrain p = findEntityById(id);
        p.setEngine(request.engine()); p.setPowerHp(request.powerHp()); p.setTorqueNm(request.torqueNm());
        p.setTransmission(request.transmission()); p.setDrivetrain(request.drivetrain());
        p.setTankLiters(request.tankLiters()); p.setFuelType(request.fuelType());
        return toResponse(powertrainRepository.save(p));
    }

    @Transactional
    public void delete(Integer id) {
        Powertrain p = findEntityById(id);
        if (!p.getVersions().isEmpty()) {
            throw new IllegalStateException("Cannot delete powertrain with associated versions");
        }
        powertrainRepository.delete(p);
    }

    public Powertrain findEntityById(Integer id) {
        return powertrainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Powertrain", id));
    }

    private PowertrainResponse toResponse(Powertrain p) {
        return new PowertrainResponse(p.getPowertrainId(), p.getEngine(), p.getPowerHp(), p.getTorqueNm(),
                p.getTransmission(), p.getDrivetrain(), p.getTankLiters(), p.getFuelType());
    }
}
