## O que muda

<!-- Descreva objetivamente a mudança e o motivo. -->

## Checklist de segurança (DevSecOps — Sprint 3)

- [ ] Entrada nova validada (`@Valid` + `@SafeText` onde aplicável)
- [ ] Endpoint novo coberto por regra de RBAC explícita no `SecurityConfig`
- [ ] Nenhum dado sensível em log / resposta / URL (senha, token, e-mail, PII)
- [ ] Segredo novo via variável de ambiente e documentado no `SECURITY.md` §11
- [ ] Teste automatizado cobrindo o caminho de erro / negação de acesso
- [ ] Pipeline `DevSecOps - Security Pipeline` verde (ou achados triados e justificados)
- [ ] Sem dependência nova com vulnerabilidade `high`/`critical` (Dependency Review)

## Evidências

<!-- Prints de teste, saída do pipeline, etc. -->
