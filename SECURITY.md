# Política de Segurança — RIVA Backend

Documento de referência da camada de segurança implementada no `riva-backend`.
Cobre o escopo da disciplina de **Cybersecurity** do Ford+FIAP 2026 Challenge.

> **Status:** todas as seções implementadas — autenticação/RBAC, validação,
> rate limiting, CORS, integridade de payload, criptografia em repouso, LGPD,
> logging estruturado e trilha de auditoria.

---

## 1. Visão geral

A defesa do backend é organizada em camadas independentes, aplicando o princípio de
**defense in depth**:

| Camada | Mecanismo |
|---|---|
| Transporte | TLS (configuração disponível, ativada em produção) |
| Borda HTTP | Filtros: `MdcFilter` → `RateLimitFilter` → `JwtAuthenticationFilter` → `PayloadIntegrityFilter` |
| Aplicação | Bean Validation + `InputSanitizer` + `@SafeText` |
| Autorização | RBAC via Spring Security (`ADMIN`, `ANALYST`, `USER`) |
| Dados em repouso | AES-256-GCM no campo `email`, blind index HMAC-SHA256 |
| Observabilidade | Log estruturado + trilha de auditoria (`AuditLog`) |

---

## 2. Autenticação e autorização

- **JWT HS256** com `JwtTokenProvider` ([security/jwt/JwtTokenProvider.java](src/main/java/com/ford/riva/security/jwt/JwtTokenProvider.java))
  - Access token: 30 minutos
  - Refresh token: 7 dias
  - Secret via env `JWT_SECRET` (mínimo 32 bytes)
- **BCrypt** para senhas, strength 12 ([config/SecurityConfig.java](src/main/java/com/ford/riva/config/SecurityConfig.java))
- **Sessão STATELESS** — nenhum dado de auth persiste no servidor
- **RBAC** aplicado no `SecurityFilterChain`:
  - `/api/v1/auth/**` → público
  - `/api/v1/admin/**`, `/api/v1/users/**` → `ADMIN`
  - `/api/v1/vehicles/**` → `ADMIN` ou `ANALYST`
- **CustomUserDetailsService** carrega usuário do banco e converte `Role` em `ROLE_<nome>`
- **Endpoints** (`AuthController`):
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
- **DataInitializer** cria usuário ADMIN default no primeiro start (senha via `ADMIN_DEFAULT_PASSWORD`).

---

## 3. Validação e sanitização de entradas

Quatro camadas independentes validam todo input vindo do cliente:

1. **`@NotBlank` / `@NotEmpty`** — rejeita vazio, null, só whitespace
2. **`@Size`** — limite de tamanho por campo (anti payload flooding)
3. **`@Pattern`** — whitelist de caracteres permitidos por contexto
4. **`@SafeText`** (custom) — análise semântica via `InputSanitizer` para padrões de
   XSS, SQL injection, command injection e path traversal

### InputSanitizer

[util/InputSanitizer.java](src/main/java/com/ford/riva/util/InputSanitizer.java) — detecta:

- **XSS**: `<script>`, `javascript:`, `vbscript:`, eventos JS (`onerror=`, `onload=`...),
  `<iframe>`, `<svg onload>`, `data:text/html`
- **SQL injection**: `UNION SELECT`, `DROP TABLE`, `DELETE FROM`, comentários `-- ` `/* */`,
  comandos encadeados (`; DROP`, `; DELETE`...)
- **Command injection**: `$(...)`, backticks, pipes para `sh/bash/cmd/nc/wget/curl`,
  `; rm`, `&& shutdown`
- **Path traversal**: `../`, `..\\`, encoded `%2e%2e`, `/etc/passwd`, `/proc/`, `C:\Windows\System32`
- **Control chars** e tags HTML são removidas, whitespace é normalizado

### Tratamento global de erros

[exception/GlobalExceptionHandler.java](src/main/java/com/ford/riva/exception/GlobalExceptionHandler.java)
captura **12 tipos de exceção** e converte em [ApiErrorResponse](src/main/java/com/ford/riva/dto/error/ApiErrorResponse.java)
padronizado, **sem nunca expor**:

- Stack trace
- Nome de classe interna
- Estrutura de banco
- Tecnologia subjacente

Stack trace completo só vai pro **log interno** (nível ERROR). Resposta ao cliente sempre genérica.

---

## 4. Rate limiting

Implementado com **Bucket4j** (algoritmo token bucket) em
[security/filter/RateLimitFilter.java](src/main/java/com/ford/riva/security/filter/RateLimitFilter.java).

### Limites

| Escopo | Limite | Motivo |
|---|---|---|
| Endpoints gerais | 60 req/min por IP | Proteção contra flooding/DoS leve |
| `POST /api/v1/auth/login` | 10 req/min por IP | Proteção anti brute-force de senha |

### Funcionamento

- Um `Bucket` por IP, armazenado em `ConcurrentHashMap<String, Bucket>` (buckets
  separados para tráfego geral e para login).
- IP resolvido considerando o header `X-Forwarded-For` (suporte a proxy reverso).
- Refill **greedy**: tokens reabastecem continuamente ao longo do minuto.
- Requests `OPTIONS` (preflight CORS) não são contabilizadas.
- Ao exceder o limite: resposta **429 Too Many Requests** com:
  - Body `ApiErrorResponse` padronizado
  - Header `Retry-After` com os segundos até liberar
- Header `X-Rate-Limit-Remaining` exposto nas respostas bem-sucedidas.
- **Monitoramento**: ao atingir 80% do limite, registra log `WARN` com o IP.

Configurável via `rate-limit.general.requests-per-minute` e
`rate-limit.auth.requests-per-minute`.

---

## 5. CORS

Configurado no `SecurityConfig` via `CorsConfigurationSource`.

| Parâmetro | Valor |
|---|---|
| `allowedOrigins` | `http://localhost:3000`, `http://localhost:8081` (Expo), + produção via `CORS_ALLOWED_ORIGINS` |
| `allowedMethods` | GET, POST, PUT, DELETE, OPTIONS |
| `allowedHeaders` | `Authorization`, `Content-Type`, `X-Signature` |
| `allowCredentials` | `true` |
| `maxAge` | 3600s (1h de cache do preflight) |

Requisições de origens não listadas têm o preflight **rejeitado** (403).
A lista de origens é externalizada — em produção, definir `CORS_ALLOWED_ORIGINS`
como CSV (ex: `https://riva.app,https://admin.riva.app`).

---

## 6. Integridade de payloads (HMAC)

Implementado em
[security/filter/PayloadIntegrityFilter.java](src/main/java/com/ford/riva/security/filter/PayloadIntegrityFilter.java).

### Fluxo

1. Cliente calcula `X-Signature = Base64(HMAC-SHA256(corpo_da_requisição, segredo))`.
2. Envia a assinatura no header `X-Signature` em requisições `POST` e `PUT`.
3. O filtro recalcula o HMAC sobre o corpo recebido e compara.
4. Comparação em **tempo constante** (`MessageDigest.isEqual`) — resistente a timing attack.
5. Assinatura ausente ou divergente → **403 Forbidden**.

O corpo da requisição é bufferizado por `CachedBodyHttpServletRequest`, permitindo
que o filtro leia o body para validação **e** o controller leia novamente depois.

Garante que o payload não foi adulterado em trânsito (defesa adicional ao TLS,
útil contra proxies/man-in-the-middle comprometidos).

- **Dev**: desabilitado por default (`security.hmac.enabled=false`).
- **Produção**: ativar com `HMAC_ENABLED=true` e definir `HMAC_SECRET`.

### Ordem dos filtros de segurança

```
RateLimitFilter → JwtAuthenticationFilter → PayloadIntegrityFilter
```

(O `MdcFilter` do Bloco 5 entrará antes de todos.) Os filtros são registrados
apenas dentro da `SecurityFilterChain` — o auto-registro como servlet filter
global é desativado via `FilterRegistrationBean` com `setEnabled(false)`.

---

## 7. Criptografia em repouso

### Algoritmo

**AES-256-GCM** (Galois/Counter Mode — encriptação **autenticada**, fornece
confidencialidade + integridade num único primitivo).

- Chave: 256 bits (32 bytes), via env `AES_ENCRYPTION_KEY` em Base64
- IV: 96 bits aleatório por operação (`SecureRandom`)
- Tag de autenticação: 128 bits

Formato de armazenamento na coluna:

```
Base64( IV (12 bytes) || ciphertext || GCM_TAG (16 bytes) )
```

### Campos protegidos

| Entidade | Campo | Estratégia |
|---|---|---|
| `User` | `email` | AES-256-GCM (não-determinístico) |
| `User` | `email_hash` | HMAC-SHA256 (blind index para busca) |
| `User` | `password` | BCrypt (one-way, strength 12) |

### Por que `email_hash` (blind index)?

AES-GCM usa IV aleatório por design — a mesma string vira ciphertext diferente
toda vez. Isso quebraria buscas por igualdade (`WHERE email = ?`).

A solução padrão (e o que este projeto implementa em [crypto/EmailHasher.java](src/main/java/com/ford/riva/crypto/EmailHasher.java))
é manter um **blind index**: HMAC-SHA256 do email normalizado (`trim` + `lowercase`),
armazenado numa coluna separada `email_hash`. Buscas por email se tornam buscas pelo hash.

Propriedades:
- Determinístico — `existsByEmailHash` funciona
- Não-reversível — sem o `EMAIL_HASH_SECRET`, hash não revela o email
- Resistente a comparação entre instalações — secret diferente, hash diferente

### Implementação

- [crypto/AesEncryptor.java](src/main/java/com/ford/riva/crypto/AesEncryptor.java) — `AttributeConverter<String, String>` aplicado via `@Convert` na entidade User. Hibernate chama o conversor automaticamente no insert/update/select.
- [crypto/EmailHasher.java](src/main/java/com/ford/riva/crypto/EmailHasher.java) — `@Component` injetado em `AuthService` e `DataInitializer` para popular `email_hash` ao criar/atualizar usuário.

### Validação por testes

Coberto em [AesEncryptorTest.java](src/test/java/com/ford/riva/crypto/AesEncryptorTest.java):

- Roundtrip preserva plaintext (incluindo Unicode)
- Encriptação de mesma string produz ciphertexts diferentes (IV aleatório)
- Ciphertext adulterado **falha** na decriptação (GCM tag detecta)
- Ciphertext encriptado com outra chave **não pode** ser decriptado
- Rejeita chave fora de 256 bits ou Base64 inválido

---

## 8. Retenção, descarte e LGPD

### Política de retenção

| Tipo de dado | Retenção | Mecanismo de descarte |
|---|---|---|
| Access token (JWT) | 30 minutos | Expiração automática via claim `exp` |
| Refresh token (JWT) | 7 dias | Expiração automática via claim `exp` |
| Dados de usuário (`User`) | Enquanto conta ativa | Anonimização no soft-delete |
| Logs de auditoria (`AuditLog`) | 90 dias | Job `@Scheduled` diário ([AuditLogRetentionJob](src/main/java/com/ford/riva/service/AuditLogRetentionJob.java)) |
| Dados de busca de veículos | Sem restrição | Não contém dados pessoais |

### Anonimização (LGPD Art. 18, V — direito à exclusão)

[service/AnonymizationService.java](src/main/java/com/ford/riva/service/AnonymizationService.java)
fornece `anonymizeUser(Long userId)` que substitui irrevogavelmente:

- `username` → `deleted_user_<id>`
- `email` → `deleted_user_<id>@anonymized.local` (criptografado normalmente)
- `email_hash` → hash do email anonimizado (não rastreia o original)
- `password` → marcador `{DELETED}` (não BCrypt válido — impossível autenticar)
- `enabled` → `false`

A linha não é deletada para preservar **integridade referencial** com logs de auditoria
e relacionamentos com outras entidades (compliance LGPD + rastreabilidade).

### Descarte automático da trilha de auditoria

[service/AuditLogRetentionJob.java](src/main/java/com/ford/riva/service/AuditLogRetentionJob.java)
roda diariamente às **03:00** (horário de baixo tráfego) via `@Scheduled` e
**anonimiza em bulk** todas as entradas `AuditLog` com `timestamp` anterior à
janela de retenção (90 dias por default).

A anonimização é feita por `UPDATE` em massa no nível do banco (não `DELETE`)
para preservar a **contagem histórica** de eventos por ação — relevante para
métricas agregadas e detecção de anomalias de longo prazo:

- `user_id` → `NULL`
- `ip_address` → `NULL`
- `details` → `NULL`
- `timestamp`, `action`, `resource` → **preservados** (não são dados pessoais)

Configurável via `audit.retention.days` (janela) e `audit.retention.cron`
(agendamento). Para desabilitar em ambientes de desenvolvimento, basta sobrescrever
o cron para uma data improvável ou remover `@EnableScheduling`.

### Pseudonimização para ML, BI e dashboards analíticos

O edital exige que dados usados em **machine learning, dashboards e análises
agregadas** sejam pseudonimizados — i.e., dissociados do titular real antes do
processamento analítico. As medidas:

| Uso analítico | Dado de entrada | Pseudonimização aplicada |
|---|---|---|
| Dashboards de uso (por usuário) | `User.id` | ID interno é um surrogate key — **não é PII**, não permite reidentificação fora do sistema |
| Dashboards de comportamento (por email) | `User.email_hash` | HMAC-SHA256 com `EMAIL_HASH_SECRET` — determinístico, irreversível sem o segredo |
| Modelos de ML sobre logs | `AuditLog` após retenção | `user_id` e `ip_address` já vêm `NULL` — restam timestamp, action, resource (não-pessoais) |
| Export para BI externo (CSV/parquet) | `AuditLog` ativos (<90 dias) | Antes de exportar, substituir `user_id` por HMAC-SHA256(user_id, EXPORT_SECRET) e zerar `ip_address` |
| Análise agregada (counts, taxas) | Qualquer | Operar sobre `COUNT(*) GROUP BY action, date_trunc('hour', timestamp)` — agregação remove identificabilidade |

**Princípio de design:** o `email_hash` (blind index do Bloco 7) já cumpre duplo
papel — viabiliza busca por igualdade em coluna criptografada **e** funciona como
pseudônimo estável para joins analíticos sem expor o email real. Pipelines de BI
devem consumir `email_hash`, nunca `email`.

**Segregação de chave:** o `EMAIL_HASH_SECRET` usado em produção **não deve ser
compartilhado** com o ambiente de BI/analytics. Idealmente, o export para
analytics aplica uma segunda camada de HMAC (`EXPORT_SECRET`), de forma que
mesmo um vazamento da chave de BI não permita correlacionar pseudônimos com
usuários reais do sistema operacional.

### Direitos do titular dos dados (Lei 13.709/2018)

| Direito | Endpoint / Mecanismo |
|---|---|
| Confirmação e acesso (Art. 18, I-II) | `GET /api/v1/users/{id}` (a implementar pelo time) |
| Retificação (Art. 18, III) | `PUT /api/v1/users/{id}` (a implementar pelo time) |
| Exclusão (Art. 18, V) | `AnonymizationService.anonymizeUser(id)` |
| Portabilidade (Art. 18, V) | Export JSON (a implementar pelo time) |

### Proteção contra exposição acidental

- `@JsonIgnore` no campo `User.password` — nunca serializa em response
- **DTOs separados** dos modelos — endpoints retornam `TokenResponse`, `VehicleSpecResponse`,
  `ApiErrorResponse`, nunca a entidade `User` direta
- `GlobalExceptionHandler` retorna mensagens genéricas; stack traces só em log interno
- Logs do `AuthService` registram username em logs de erro, **nunca senhas ou tokens**

---

## 9. Logging e auditoria

### Logging estruturado

Configurado em [logback-spring.xml](src/main/resources/logback-spring.xml):

- **Dev / test**: saída legível no console, com os campos do MDC visíveis.
- **Produção**: saída **JSON estruturada** via `LogstashEncoder`, pronta para
  ingestão em ELK/Datadog/CloudWatch.

Campos em cada log de produção: `timestamp`, `level`, `logger_name`, `message`,
`thread_name`, além dos campos de correlação do MDC: `trace_id`, `user_id`, `client_ip`.

### MDC (correlação de requisições)

[security/filter/MdcFilter.java](src/main/java/com/ford/riva/security/filter/MdcFilter.java)
é o **primeiro filtro** da cadeia. A cada requisição:

- Gera um `trace_id` (UUID curto) para correlacionar todos os logs do request.
- Extrai o `client_ip` (considerando `X-Forwarded-For`).
- O `user_id` é adicionado ao MDC pelo `JwtAuthenticationFilter` assim que o
  usuário é autenticado.
- Limpa o MDC no `finally` — sem vazamento entre threads do pool.

### Dados sensíveis nos logs

- **Senhas**: nunca logadas (nem em texto, nem hash).
- **Tokens JWT**: nunca logados.
- **Dados pessoais**: apenas o `username` aparece em logs de evento; e-mail e
  outros dados pessoais não são logados.

### Trilha de auditoria

[model/AuditLog.java](src/main/java/com/ford/riva/model/AuditLog.java) +
[service/AuditService.java](src/main/java/com/ford/riva/service/AuditService.java).

Cada entrada registra: `timestamp`, `userId`, `action`, `resource`, `ipAddress`, `details`.
Ações (`AuditAction`): `LOGIN`, `LOGIN_FAILED`, `LOGOUT`, `USER_CREATED`,
`USER_UPDATED`, `USER_DELETED`, `CONFIG_CHANGED`, `MASS_QUERY`.

O `AuditService.log()` roda em transação **independente** (`REQUIRES_NEW`) — a
trilha persiste mesmo que a operação de negócio falhe (ex.: `LOGIN_FAILED` é
gravado mesmo com a autenticação lançando exceção).

Pontos instrumentados atualmente:

| Evento | Ação registrada |
|---|---|
| Registro de usuário | `USER_CREATED` |
| Login bem-sucedido | `LOGIN` |
| Login falho | `LOGIN_FAILED` |
| Anonimização (LGPD) | `USER_DELETED` |

> Os controllers de veículos (responsabilidade de outros membros do time) devem
> chamar `auditService.log(AuditAction.MASS_QUERY, ...)` nas consultas.

---

## 10. Monitoramento de eventos suspeitos

| Regra | Onde | Nível |
|---|---|---|
| 5+ falhas de login do mesmo IP em 5 min | `AuthService` + `LoginAttemptService` | **ERROR** "Possível brute force detectado" |
| Cada falha de login isolada | `AuthService` | WARN (com IP) |
| IP atinge 80% do rate limit | `RateLimitFilter` | WARN |
| Rate limit excedido | `RateLimitFilter` | WARN |
| Assinatura HMAC inválida | `PayloadIntegrityFilter` | WARN |
| 404 em endpoint inexistente (possível scan) | `GlobalExceptionHandler` | WARN (método + URL) |
| Acesso não autenticado / negado | `JwtAuthenticationEntryPoint` / `JwtAccessDeniedHandler` | WARN |

### Detecção de brute force

[service/LoginAttemptService.java](src/main/java/com/ford/riva/service/LoginAttemptService.java)
mantém, por IP, uma janela deslizante de 5 minutos das falhas de login. Ao atingir
**5 falhas**, o `AuthService` emite log `ERROR`. Um login bem-sucedido reseta o
contador do IP. A estrutura é um `ConcurrentHashMap<String, Deque<Instant>>` com
expurgo automático das entradas fora da janela.

---

## 11. Variáveis de ambiente necessárias

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | **Sim** (prod) | Chave HS256 para JWT. Mínimo 32 bytes. |
| `AES_ENCRYPTION_KEY` | **Sim** (prod) | Chave AES-256 em Base64 (32 bytes decodificados). Gere com `openssl rand -base64 32`. |
| `EMAIL_HASH_SECRET` | **Sim** (prod) | Secret HMAC-SHA256 para blind index do email. Mínimo 32 chars. |
| `ADMIN_DEFAULT_PASSWORD` | **Sim** (prod) | Senha do admin default criado no primeiro start. **Trocar imediatamente após o primeiro login.** |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` | **Sim** (prod) | Conexão PostgreSQL Azure |
| `CORS_ALLOWED_ORIGINS` | Recomendada | Lista CSV de origens permitidas em CORS. |
| `HMAC_ENABLED` | Recomendada (prod) | `true` para exigir assinatura `X-Signature` em POST/PUT. |
| `HMAC_SECRET` | Se `HMAC_ENABLED=true` | Secret HMAC-SHA256 para validação de integridade de payload. Mínimo 32 chars. |
| `SSL_KEYSTORE_PASSWORD` | Apenas se HTTPS via Spring | Senha do keystore PKCS12 |

**IMPORTANTE:** os valores em [application.properties](src/main/resources/application.properties)
são **fallbacks de desenvolvimento**. Em produção, todas as secrets devem vir de
variáveis de ambiente — nunca commitar secrets reais no repositório.

### Rotação de chaves

- **`JWT_SECRET`** — pode ser rotacionada; tokens emitidos com chave antiga deixam de ser
  aceitos (usuários precisam logar de novo).
- **`AES_ENCRYPTION_KEY`** — **não pode ser rotacionada sem re-encriptação dos dados existentes**.
  Antes de trocar a chave, executar job que descriptografa com a chave antiga e re-criptografa
  com a nova.
- **`EMAIL_HASH_SECRET`** — não pode ser rotacionada sem recomputar todos os `email_hash` da
  tabela `users`.

---

## 12. HTTPS em produção

A configuração de TLS está disponível mas comentada por default em
[application-prod.properties](src/main/resources/application-prod.properties) para não
quebrar dev local.

Para ativar:

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=riva
```

Gerar keystore PKCS12 a partir de certificado válido (Let's Encrypt, ACM etc.).
Mínimo **TLS 1.2**; preferencialmente **TLS 1.3** (default do Tomcat embedded
moderno).

---

## 13. DDL recomendado da tabela `users`

Como o projeto não usa Flyway/Liquibase (decisão do time), o DDL deve ser
aplicado manualmente no Postgres da Azure antes do primeiro deploy:

```sql
CREATE TABLE users (
    user_id        BIGSERIAL PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    email          VARCHAR(500) NOT NULL,
    email_hash     VARCHAR(64)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    active         BOOLEAN      NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_users_email_hash ON users(email_hash);

CREATE TABLE audit_logs (
    audit_log_id  BIGSERIAL PRIMARY KEY,
    timestamp     TIMESTAMP    NOT NULL,
    user_id       VARCHAR(50),
    action        VARCHAR(30)  NOT NULL,
    resource      VARCHAR(200),
    ip_address    VARCHAR(45),
    details       VARCHAR(500)
);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
```

Notas:
- `email` é VARCHAR(500) porque AES-GCM + Base64 expande o tamanho do plaintext.
- `email_hash` tem `UNIQUE` para enforcement de unicidade no nível do banco.
- `password_hash` é VARCHAR(255) para acomodar BCrypt e marcador `{DELETED}` da anonimização.
- `audit_logs.ip_address` é VARCHAR(45) para comportar endereços IPv6.
- `audit_logs.user_id` é nullable — ações anônimas (ex.: login falho) não têm usuário.
