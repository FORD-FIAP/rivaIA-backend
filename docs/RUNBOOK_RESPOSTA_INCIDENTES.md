# Runbook — Resposta a Incidentes de Segurança — RIVA

Procedimento operacional para os eventos que o monitoramento (Prometheus/Grafana/Loki)
e os logs da aplicação podem sinalizar. Complementa a seção 4.2 do
[SPRINT3_DEVSECOPS.md](SPRINT3_DEVSECOPS.md).

## Ciclo de resposta

```
Detecção → Análise → Contenção → Erradicação → Recuperação → Pós-morte
```

| Fase | Objetivo | Responsável |
|---|---|---|
| Detecção | Reconhecer que há um incidente (alerta, log, denúncia) | On-call |
| Análise | Classificar severidade, escopo e vetor | On-call + Sec |
| Contenção | Parar o dano em curso (bloquear IP, revogar token, isolar) | On-call |
| Erradicação | Remover a causa (corrigir bug, girar segredo, subir patch) | Dev + Sec |
| Recuperação | Restaurar serviço normal e confirmar integridade | Dev |
| Pós-morte | Documentar linha do tempo, causa-raiz e ações | Sec (facilita) |

## Severidades

| Sev | Definição | Prazo 1ª resposta |
|---|---|---|
| **SEV1** | Vazamento de dado pessoal, RCE, comprometimento de credencial de produção, serviço fora do ar | 15 min |
| **SEV2** | Ataque ativo contido (brute force, flooding), vulnerabilidade crítica explorável | 1 h |
| **SEV3** | Varredura, tentativa isolada, vulnerabilidade sem exploração observada | 1 dia útil |

## Contatos e canais

- Canal de guerra: `#riva-incidentes` (criar no início do incidente)
- Registro da linha do tempo: documento único por incidente em `docs/incidentes/AAAA-MM-DD-titulo.md`
- Escalonamento: On-call → Líder técnico → Scrum Master (Prof. Yan Coelho)

---

## Playbooks

### <a id="brute-force"></a>Brute force / pico de falhas de login

**Sinais:** alerta `AuthLoginFailureSpike`; log `ERROR "Possível brute force detectado: IP=<ip> com 5 falhas..."`; painel *Falhas de login /min* no Grafana.

1. **Análise** — no Grafana (painel de logs de segurança) ou Loki:
   `{job="riva-backend"} |~ "brute force"` — extrair o(s) IP(s) e o alvo (`username`).
   Verificar se é um IP único (brute force clássico) ou distribuído (credential stuffing).
2. **Contenção**
   - IP único → bloquear no WAF / security group da Azure / reverse proxy.
   - Distribuído → reduzir temporariamente `rate-limit.auth.requests-per-minute` (ex.: 10 → 3)
     e reiniciar; considerar exigir CAPTCHA no frontend de login.
   - Se alguma conta teve login bem-sucedido após a rajada → tratar como possível
     comprometimento: forçar rotação de senha e invalidar tokens (girar `JWT_SECRET`).
3. **Erradicação** — confirmar que o `LoginAttemptService` está contando por IP; se o
   ataque veio por trás de proxy, validar o header `X-Forwarded-For` e a config
   `server.tomcat.remoteip`.
4. **Recuperação** — restaurar limites normais; monitorar o painel por 24 h.
5. **Pós-morte** — quantas contas visadas, houve sucesso, tempo até detecção/contenção.

### <a id="flooding-dos"></a>Flooding / DoS (pico de 429)

**Sinais:** alerta `RateLimitRejectionSpike`; painel *Rate limit 429 /min*; latência p99 subindo.

1. **Análise** — `{job="riva-backend"} |~ "rate limit"` para ver os IPs no WARN do
   `RateLimitFilter`. Conferir se a origem é legítima (cliente mal configurado) ou abuso.
2. **Contenção** — bloquear IP/faixa no nível de rede; se distribuído, acionar proteção
   de borda (Azure Front Door / Cloudflare) e habilitar `HMAC_ENABLED=true` para exigir
   assinatura em POST/PUT.
3. **Erradicação/Recuperação** — escalar réplicas se necessário; revisar limites do Bucket4j.
4. **Pós-morte** — volume de pico, impacto em usuários legítimos, eficácia do rate limit.

### <a id="erros-5xx"></a>Surto de erros 5xx

**Sinais:** alerta `HighHttp5xxRate`; painel *Requisições por status*.

1. **Análise** — `{job="riva-backend", level="ERROR"}` no Loki, agrupar por `logger_name`
   e `message`. Correlacionar com deploy recente, indisponibilidade do Postgres (Azure)
   ou dependência externa.
2. **Contenção** — se a causa é um deploy, **rollback** para a release anterior
   (branch `production` → tag anterior). Se é o banco, acionar suporte Azure e ativar
   modo degradado (respostas em cache / read-only) se houver.
3. **Erradicação** — corrigir e subir hotfix com teste de regressão.
4. **Recuperação** — 5xx < 1% por 30 min; validar dados não corrompidos.

### <a id="surto-de-erros"></a>Surto de logs ERROR sem 5xx

**Sinais:** alerta `ErrorLogSurge` sem aumento proporcional de 5xx.

Normalmente é **segurança** (brute force detectado) ou uma dependência falhando de
forma tratada. Seguir o playbook de brute force se as mensagens forem de autenticação;
caso contrário, abrir investigação SEV3 e priorizar correção do log ruidoso.

### <a id="servico-indisponivel"></a>Serviço indisponível

**Sinais:** alerta `RivaBackendDown`; `/actuator/health` sem resposta.

1. Verificar processo/POD, `docker ps`, logs de startup.
2. Checar conectividade com o Postgres (Azure) — `sslmode=require`, credenciais, firewall.
3. Se OOM (ver `JvmHeapPressure` antes da queda) → aumentar limite de memória / investigar leak.
4. Restaurar; se persistir, rollback do último deploy.

### <a id="secret-vazado"></a>Segredo vazado (Gitleaks/alerta externo)

1. **Contenção imediata** — girar o segredo afetado:
   - `JWT_SECRET` → rotacionar (invalida todos os tokens; usuários relogam).
   - `AES_ENCRYPTION_KEY` → **não** girar direto: rodar job de re-cifragem (ver SECURITY.md §11).
   - `EMAIL_HASH_SECRET` → recomputar `email_hash` de toda a tabela.
   - `DB_PASSWORD` → trocar no Postgres Azure e no secret store.
2. **Erradicação** — remover o segredo do histórico (`git filter-repo`), forçar push,
   invalidar caches/forks. Adicionar padrão ao `.gitleaks.toml` se for novo formato.
3. **Pós-morte** — como entrou, por que o pre-commit/CI não pegou, o que mudou.

### <a id="dependencia-vulneravel"></a>Dependência vulnerável (SCA/Dependabot)

1. Triagem: CVSS, se é alcançável no nosso uso, se há exploit público.
2. CVSS ≥ 7 e alcançável → hotfix priorizado (bump de versão) na sprint atual.
3. Sem correção disponível → aplicar mitigação (config, WAF) e registrar risco aceito
   com prazo de revisão.

---

## Modelo de registro de incidente

```
# Incidente AAAA-MM-DD — <título>
- Severidade:
- Detectado por: (alerta / log / pessoa) às HH:MM
- Sistemas afetados:
- Linha do tempo:
  - HH:MM  ...
- Causa-raiz:
- Contenção aplicada:
- Correção definitiva:
- Dados pessoais afetados? (LGPD — se sim, avaliar comunicação à ANPD e titulares)
- Ações de follow-up: (dono / prazo)
```
