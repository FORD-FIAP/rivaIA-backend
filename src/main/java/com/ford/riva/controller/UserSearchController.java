package com.ford.riva.controller;

import com.ford.riva.dto.response.SearchResponse;
import com.ford.riva.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/searches")
@RequiredArgsConstructor
public class UserSearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<List<SearchResponse>> findByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(searchService.findByUserId(userId));
    }

    @DeleteMapping("/{searchId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long userId,
            @PathVariable Integer searchId) {
        searchService.delete(searchId);
        return ResponseEntity.noContent().build();
    }
}
