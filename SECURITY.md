# Política de Segurança — RIVA Backend

Documento de referência da camada de segurança implementada no `riva-backend`.
Cobre o escopo da disciplina de **Cybersecurity** do Ford+FIAP 2026 Challenge.

> **Status:** seções 2 (Auth), 3 (Validação) e 7–8 (Crypto + LGPD) implementadas.
> Rate limiting, CORS, HMAC payload e logging estruturado serão adicionados nos blocos seguintes.

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

*(A ser implementado no Bloco 3 — `RateLimitFilter` com Bucket4j)*

Plano:
- 60 req/min/IP em endpoints gerais
- 10 req/min/IP em `/api/v1/auth/login` (anti brute-force)
- 429 com header `Retry-After` quando excedido

---

## 5. CORS

*(A ser implementado no Bloco 3)*

Plano:
- `allowedOrigins`: `http://localhost:3000`, `http://localhost:8081` (Expo),
  produção via `CORS_ALLOWED_ORIGINS`
- `allowedMethods`: GET, POST, PUT, DELETE, OPTIONS
- `allowedHeaders`: `Authorization`, `Content-Type`, `X-Signature`
- `allowCredentials`: true
- `maxAge`: 3600

---

## 6. Integridade de payloads (HMAC)

*(A ser implementado no Bloco 3 — `PayloadIntegrityFilter`)*

Plano: verificação de header `X-Signature` com HMAC-SHA256 em requests POST/PUT.

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
| Logs de auditoria (`AuditLog`) | 90 dias | Job de anonimização programado (a implementar) |
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

*(A ser implementado no Bloco 5)*

Plano:
- Logback com `LogstashEncoder` (JSON) em produção
- `MdcFilter` popula `client_ip`, `user_id`, `trace_id`
- Entidade `AuditLog` registra: login sucesso/falha, registro, alteração de papel,
  consultas de veículos
- Logs **nunca** contêm senha, token JWT, dados pessoais

---

## 10. Monitoramento de eventos suspeitos

*(A ser implementado no Bloco 5)*

Plano:
- 5+ falhas de login de mesmo IP em 5 min → log ERROR "possível brute force"
- IP atinge 80% do rate limit → log WARN
- 404 em endpoints inexistentes → log WARN com método + URL (detecção de scan)

Já parcialmente implementado: o `GlobalExceptionHandler` já loga 404s e falhas de auth.

---

## 11. Variáveis de ambiente necessárias

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | **Sim** (prod) | Chave HS256 para JWT. Mínimo 32 bytes. |
| `AES_ENCRYPTION_KEY` | **Sim** (prod) | Chave AES-256 em Base64 (32 bytes decodificados). Gere com `openssl rand -base64 32`. |
| `EMAIL_HASH_SECRET` | **Sim** (prod) | Secret HMAC-SHA256 para blind index do email. Mínimo 32 chars. |
| `ADMIN_DEFAULT_PASSWORD` | **Sim** (prod) | Senha do admin default criado no primeiro start. **Trocar imediatamente após o primeiro login.** |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` | **Sim** (prod) | Conexão PostgreSQL Azure |
| `CORS_ALLOWED_ORIGINS` | Recomendada | Lista CSV de origens permitidas em CORS. *(Bloco 3)* |
| `HMAC_SECRET` | Recomendada | Secret para validação de payload com `X-Signature`. *(Bloco 3)* |
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
```

Notas:
- `email` é VARCHAR(500) porque AES-GCM + Base64 expande o tamanho do plaintext.
- `email_hash` tem `UNIQUE` para enforcement de unicidade no nível do banco.
- `password_hash` é VARCHAR(255) para acomodar BCrypt e marcador `{DELETED}` da anonimização.
