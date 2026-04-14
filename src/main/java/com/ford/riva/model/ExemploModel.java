package com.ford.riva.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "TB_EXEMPLO")
@Data
public class ExemploModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
}
