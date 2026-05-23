package com.ford.riva.service;

import com.ford.riva.dto.request.VehicleRequest;
import com.ford.riva.dto.request.VehicleSearchRequest;
import com.ford.riva.dto.response.BrandResponse;
import com.ford.riva.dto.response.CategoryResponse;
import com.ford.riva.dto.response.VehicleResponse;
import com.ford.riva.dto.response.VehicleSearchResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.Brand;
import com.ford.riva.model.Category;
import com.ford.riva.model.Vehicle;
import com.ford.riva.repository.VehicleRepository;
import com.ford.riva.repository.VehicleSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BrandService brandService;
    private final CategoryService categoryService;

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        Brand brand = brandService.findEntityById(request.brandId());
        Category category = categoryService.findEntityById(request.categoryId());
        Vehicle vehicle = Vehicle.builder()
                .brand(brand).category(category).model(request.model())
                .modelYear(request.modelYear()).numSeats(request.numSeats()).numDoors(request.numDoors())
                .build();
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findAll() {
        return vehicleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByBrandId(Integer brandId) {
        return vehicleRepository.findByBrandBrandId(brandId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByCategoryId(Integer categoryId) {
        return vehicleRepository.findByCategoryCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByModel(String model) {
        return vehicleRepository.findByModelContainingIgnoreCase(model).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findByModelYear(Integer modelYear) {
        return vehicleRepository.findByModelYear(modelYear).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse findById(Integer id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public VehicleResponse update(Integer id, VehicleRequest request) {
        Vehicle vehicle = findEntityById(id);
        vehicle.setBrand(brandService.findEntityById(request.brandId()));
        vehicle.setCategory(categoryService.findEntityById(request.categoryId()));
        vehicle.setModel(request.model());
        vehicle.setModelYear(request.modelYear());
        vehicle.setNumSeats(request.numSeats());
        vehicle.setNumDoors(request.numDoors());
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Integer id) {
        Vehicle vehicle = findEntityById(id);
        if (!vehicle.getVersions().isEmpty()) {
            throw new IllegalStateException("Cannot delete vehicle with associated versions");
        }
        vehicleRepository.delete(vehicle);
    }

    @Transactional(readOnly = true)
    public VehicleSearchResponse search(VehicleSearchRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 10;
        Page<Vehicle> vehiclePage = vehicleRepository.findAll(
                VehicleSpecification.fromSearchRequest(request), PageRequest.of(page, size));
        return new VehicleSearchResponse(
                vehiclePage.getContent().stream().map(this::toResponse).toList(),
                vehiclePage.getNumber(), vehiclePage.getSize(),
                vehiclePage.getTotalElements(), vehiclePage.getTotalPages());
    }

    public Vehicle findEntityById(Integer id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    public VehicleResponse toResponse(Vehicle v) {
        return new VehicleResponse(
                v.getVehicleId(),
                new BrandResponse(v.getBrand().getBrandId(), v.getBrand().getName()),
                new CategoryResponse(v.getCategory().getCategoryId(), v.getCategory().getName()),
                v.getModel(), v.getModelYear(), v.getNumSeats(), v.getNumDoors());
    }
}
