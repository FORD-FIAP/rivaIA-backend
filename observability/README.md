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

- A aplicação precisa expor o Actuator em Prometheus. Isso já está configurado:
  - **dev**: `http://localhost:8080/actuator/prometheus`
  - **prod/obs**: porta de gestão separada `9090` → `http://localhost:9090/actuator/prometheus`
- Logs JSON em arquivo: rode a app com o profile `obs` (ou `prod`), que ativa o
  appender `JSON_FILE` do [logback-spring.xml](../src/main/resources/logback-spring.xml):

```bash
# na raiz do riva-backend
SPRING_PROFILES_ACTIVE=obs ./mvnw spring-boot:run
# gera logs/riva-backend.json
```

> Se rodar só no profile `dev`, ajuste o alvo do Prometheus em
> [prometheus/prometheus.yml](prometheus/prometheus.yml) para a porta `8080` e
> note que não haverá arquivo de log para o Promtail (os painéis de métricas
> continuam funcionando; os painéis de log ficam vazios).

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
