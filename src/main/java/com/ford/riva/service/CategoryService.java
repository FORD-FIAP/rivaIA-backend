package com.ford.riva.service;

import com.ford.riva.dto.request.CategoryRequest;
import com.ford.riva.dto.response.CategoryResponse;
import com.ford.riva.exception.DuplicateResourceException;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.Category;
import com.ford.riva.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category", "name", request.name());
        }
        return toResponse(categoryRepository.save(Category.builder().name(request.name()).build()));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Integer id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = findEntityById(id);
        categoryRepository.findByName(request.name())
                .filter(existing -> !existing.getCategoryId().equals(id))
                .ifPresent(existing -> { throw new DuplicateResourceException("Category", "name", request.name()); });
        category.setName(request.name());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Integer id) {
        Category category = findEntityById(id);
        if (!category.getVehicles().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated vehicles");
        }
        categoryRepository.delete(category);
    }

    public Category findEntityById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getCategoryId(), category.getName());
    }
}
