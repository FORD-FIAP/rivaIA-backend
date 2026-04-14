package com.ford.riva.controller;

import com.ford.riva.model.ExemploModel;
import com.ford.riva.service.ExemploService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exemplo")
@RequiredArgsConstructor
public class ExemploController {

    private final ExemploService exemploService;

    @GetMapping
    public ResponseEntity<List<ExemploModel>> listarTodos() {
        return ResponseEntity.ok(exemploService.listarTodos());
    }
}
