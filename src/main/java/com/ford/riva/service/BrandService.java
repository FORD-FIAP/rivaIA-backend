package com.ford.riva.service;

import com.ford.riva.dto.request.BrandRequest;
import com.ford.riva.dto.response.BrandResponse;
import com.ford.riva.exception.DuplicateResourceException;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.Brand;
import com.ford.riva.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandResponse create(BrandRequest request) {
        if (brandRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Brand", "name", request.name());
        }
        return toResponse(brandRepository.save(Brand.builder().name(request.name()).build()));
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> findAll() {
        return brandRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse findById(Integer id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public BrandResponse update(Integer id, BrandRequest request) {
        Brand brand = findEntityById(id);
        brandRepository.findByName(request.name())
                .filter(existing -> !existing.getBrandId().equals(id))
                .ifPresent(existing -> { throw new DuplicateResourceException("Brand", "name", request.name()); });
        brand.setName(request.name());
        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void delete(Integer id) {
        Brand brand = findEntityById(id);
        if (!brand.getVehicles().isEmpty()) {
            throw new IllegalStateException("Cannot delete brand with associated vehicles");
        }
        brandRepository.delete(brand);
    }

    public Brand findEntityById(Integer id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getBrandId(), brand.getName());
    }
}
