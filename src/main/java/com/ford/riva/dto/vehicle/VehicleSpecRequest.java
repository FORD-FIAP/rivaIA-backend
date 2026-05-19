package com.ford.riva.dto.vehicle;

import com.ford.riva.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSpecRequest {

    private static final String SAFE_TEXT_REGEX = "^[\\p{L}\\p{N} .,\\-/()]+$";
    private static final String SAFE_TEXT_MESSAGE =
            "Permitido apenas letras, números, espaços, pontos, vírgulas, hífens, barras e parênteses";

    @NotBlank(message = "Marca é obrigatória")
    @Size(min = 1, max = 100, message = "Marca deve ter entre 1 e 100 caracteres")
    @Pattern(regexp = SAFE_TEXT_REGEX, message = SAFE_TEXT_MESSAGE)
    @SafeText
    private String marca;

    @NotBlank(message = "Modelo é obrigatório")
    @Size(min = 1, max = 100, message = "Modelo deve ter entre 1 e 100 caracteres")
    @Pattern(regexp = SAFE_TEXT_REGEX, message = SAFE_TEXT_MESSAGE)
    @SafeText
    private String modelo;

    @NotBlank(message = "Versão é obrigatória")
    @Size(min = 1, max = 100, message = "Versão deve ter entre 1 e 100 caracteres")
    @Pattern(regexp = SAFE_TEXT_REGEX, message = SAFE_TEXT_MESSAGE)
    @SafeText
    private String versao;

    @NotEmpty(message = "Lista de atributos não pode ser vazia")
    @Size(max = 50, message = "Máximo de 50 atributos por requisição")
    private List<
            @NotBlank(message = "Atributo não pode ser vazio")
            @Size(min = 1, max = 200, message = "Atributo deve ter entre 1 e 200 caracteres")
            @Pattern(regexp = SAFE_TEXT_REGEX, message = SAFE_TEXT_MESSAGE)
            @SafeText
            String
            > atributos;
}
