package com.ford.riva.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSpecResponse {

    private String marca;
    private String modelo;
    private String versao;
    private Map<String, String> especificacoes;
}
