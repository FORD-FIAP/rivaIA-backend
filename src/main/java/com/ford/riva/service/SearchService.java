package com.ford.riva.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.riva.dto.request.SearchRequest;
import com.ford.riva.dto.request.VehicleSearchRequest;
import com.ford.riva.dto.response.SearchResponse;
import com.ford.riva.exception.ResourceNotFoundException;
import com.ford.riva.model.Search;
import com.ford.riva.model.User;
import com.ford.riva.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SearchResponse create(SearchRequest request) {
        User user = userService.findEntityById(request.userId());
        return toResponse(searchRepository.save(Search.builder().user(user).filters(request.filters()).build()));
    }

    @Transactional(readOnly = true)
    public List<SearchResponse> findAll() {
        return searchRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SearchResponse findById(Integer id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<SearchResponse> findByUserId(Long userId) {
        userService.findEntityById(userId);
        return searchRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Integer id) {
        searchRepository.delete(findEntityById(id));
    }

    @Transactional
    public SearchResponse saveFromSearch(Long userId, VehicleSearchRequest request) {
        User user = userService.findEntityById(userId);
        return toResponse(searchRepository.save(
                Search.builder().user(user).filters(buildFiltersJson(request)).build()));
    }

    private String buildFiltersJson(VehicleSearchRequest request) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (request.brandId() != null) filters.put("brandId", request.brandId());
        if (request.categoryId() != null) filters.put("categoryId", request.categoryId());
        if (request.model() != null) filters.put("model", request.model());
        if (request.modelYearMin() != null) filters.put("modelYearMin", request.modelYearMin());
        if (request.modelYearMax() != null) filters.put("modelYearMax", request.modelYearMax());
        if (request.priceMin() != null) filters.put("priceMin", request.priceMin());
        if (request.priceMax() != null) filters.put("priceMax", request.priceMax());
        if (request.fuelType() != null) filters.put("fuelType", request.fuelType());
        if (request.drivetrain() != null) filters.put("drivetrain", request.drivetrain());
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Search findEntityById(Integer id) {
        return searchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Search", id));
    }

    private SearchResponse toResponse(Search s) {
        return new SearchResponse(s.getSearchId(), s.getUser().getId(), s.getFilters(), s.getCreatedAt());
    }
}
