# Observabilidade — RIVA backend

Stack local de monitoramento da Sprint 3 (atividade *Observabilidade, Monitoramento
e Resposta*). Reúne métricas, logs e alertas num único `docker compose`.

| Componente | Porta | Função |
|---|---|---|
| Prometheus | http://localhost:9091 | Coleta métricas do Actuator e avalia as regras de alerta |
| Alertmanager | http://localhost:9093 | Roteia/agrupa os alertas (receiver de segurança separado) |
| Loki | http://localhost:3100 | Armazena os logs JSON estruturados |
| Promtail | — | Faz o *tail* de `logs/riva-backend.json` e envia ao Loki |
| Grafana | http://localhost:3001 | Dashboards + exploração de logs (datasources já provisionados) |

## Pré-requisitos

- A porta de gestão separada `9090` (`application-prod.properties`) só existe
  quando o profile **`prod`** está ativo — e esse profile aponta pro Postgres
  da Azure, então não dá pra usar em teste local. **Rodando localmente
  (`dev`, com ou sem `obs`), o Actuator fica na mesma porta 8080 da API.**
  Por isso o `prometheus.yml` já vem configurado para `host.docker.internal:8080`.
- Ative o profile `obs` **junto com** o `dev` (não sozinho — `obs` isolado não
  tem configuração de banco) para ligar o appender `JSON_FILE` do
  [logback-spring.xml](../src/main/resources/logback-spring.xml) e gerar o
  arquivo de log que o Promtail lê:

```bash
# na raiz do riva-backend
docker compose up -d                              # sobe o Postgres de dev
SPRING_PROFILES_ACTIVE=dev,obs ./mvnw spring-boot:run
# gera logs/riva-backend.json
```

> No PowerShell (Windows): `$env:SPRING_PROFILES_ACTIVE="dev,obs"` antes do `.\mvnw.cmd spring-boot:run`.

> Se subir só com `dev` (sem `obs`), os painéis de **métricas** continuam
> funcionando normalmente — só os painéis de **log** (Loki) ficam vazios,
> porque o appender JSON em arquivo não é ativado.

> Em produção real (profile `prod`), o Actuator vai para a porta `9090` —
> troque o alvo em [prometheus/prometheus.yml](prometheus/prometheus.yml) de
> volta para `host.docker.internal:9090` nesse cenário.

## Subir o stack

```bash
docker compose -f observability/docker-compose.observability.yml up -d
```

Primeiro acesso ao Grafana: `admin` / `admin` (troca obrigatória). O dashboard
**RIVA - Segurança & Observabilidade** já aparece na pasta *RIVA*.

## Derrubar

```bash
docker compose -f observability/docker-compose.observability.yml down
# -v para apagar também os volumes (dados históricos)
```

## O que o dashboard mostra

- **Visão geral**: serviço no ar, req/s, % de 5xx, latência p99, contagem de logs ERROR.
- **HTTP**: requisições por status, latência p50/p95/p99.
- **Segurança**: falhas de login/min (brute force), respostas 429 (rate limit / flooding),
  403/404 (scan), painel de *logs de eventos de segurança* e painel de *logs ERROR*.
- **Recursos**: heap, CPU processo/sistema, threads e conexões do pool.

## Alertas

Definidos em [prometheus/alert.rules.yml](prometheus/alert.rules.yml):

| Alerta | Disparo | Severidade |
|---|---|---|
| `RivaBackendDown` | `up == 0` por 1m | critical |
| `HighHttp5xxRate` | > 5% de 5xx por 5m | critical |
| `ErrorLogSurge` | > 20 logs ERROR em 10m | warning |
| `AuthLoginFailureSpike` | > 30 falhas de login/min por 3m | warning (segurança) |
| `RateLimitRejectionSpike` | > 120 respostas 429/min por 3m | warning (segurança) |
| `ForbiddenResponseSpike` | > 60 respostas 403/min por 5m | info (segurança) |
| `NotFoundScanSpike` | > 100 respostas 404/min por 5m | info (segurança) |
| `HighLatencyP99` | p99 > 1s por 10m | warning |
| `JvmHeapPressure` | heap > 90% por 10m | warning |
| `HighCpuUsage` | CPU > 85% por 10m | warning |

Para notificar de verdade (e-mail/Slack/Teams), preencha o receiver `seguranca`
em [alertmanager/alertmanager.yml](alertmanager/alertmanager.yml).
