package com.ford.riva.controller;

import com.ford.riva.service.AnonymizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administração de usuários. Protegido por RBAC (ADMIN) via
 * {@code /api/v1/users/**} no SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AnonymizationService anonymizationService;

    /**
     * Exerce o direito à exclusão (LGPD Art. 18, V): anonimiza
     * irreversivelmente os dados pessoais do usuário, preservando a linha
     * para integridade referencial com a trilha de auditoria.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        anonymizationService.anonymizeUser(id);
        return ResponseEntity.noContent().build();
    }
}
