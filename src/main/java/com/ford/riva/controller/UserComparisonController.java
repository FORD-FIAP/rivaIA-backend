package com.ford.riva.controller;

import com.ford.riva.dto.request.ComparisonRequest;
import com.ford.riva.dto.response.ComparisonResponse;
import com.ford.riva.service.ComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/comparisons")
@RequiredArgsConstructor
public class UserComparisonController {

    private final ComparisonService comparisonService;

    @PostMapping
    public ResponseEntity<ComparisonResponse> create(
            @PathVariable Long userId,
            @Valid @RequestBody ComparisonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comparisonService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ComparisonResponse>> findByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(comparisonService.findByUserId(userId));
    }

    @DeleteMapping("/{comparisonId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long userId,
            @PathVariable Integer comparisonId) {
        comparisonService.delete(comparisonId);
        return ResponseEntity.noContent().build();
    }
}
