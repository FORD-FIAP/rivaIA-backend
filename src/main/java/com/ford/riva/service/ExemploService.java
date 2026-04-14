package com.ford.riva.service;

import com.ford.riva.model.ExemploModel;
import com.ford.riva.repository.ExemploRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExemploService {

    private final ExemploRepository exemploRepository;

    public List<ExemploModel> listarTodos() {
        return exemploRepository.findAll();
    }
}
