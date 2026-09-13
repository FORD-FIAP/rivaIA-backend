# ===========================================================================
# RIVA backend - imagem de producao endurecida (hardening).
# Multi-stage: build isolado + runtime minimo, sem ferramentas de build.
# Boas praticas aplicadas (verificadas pelo Trivy no pipeline de seguranca):
#   - imagem-base slim e com tag fixada (sem :latest)
#   - atualizacao dos pacotes do SO-base
#   - usuario nao-root, sem shell de login
#   - sem segredos embutidos (tudo por variavel de ambiente em runtime)
#   - JVM consciente de container e entropia non-blocking
#   - healthcheck no liveness probe do Actuator
# ===========================================================================

# --- Stage 1: build ---------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

# Cache de dependencias: resolve com base so no pom antes de copiar o codigo
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Codigo-fonte e empacotamento (testes rodam no pipeline de CI, nao aqui)
COPY src/ src/
RUN mvn -B -ntp clean package -DskipTests \
    && cp target/*.jar /build/app.jar

# --- Stage 2: runtime -------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

LABEL org.opencontainers.image.title="riva-backend" \
      org.opencontainers.image.description="RIVA - Inteligencia Competitiva Automotiva (Ford + FIAP)" \
      org.opencontainers.image.source="https://github.com/FORD-FIAP/rivaIA-backend"

# Atualiza o SO-base, adiciona curl para o healthcheck e cria usuario sem privilegios
RUN apk upgrade --no-cache \
    && apk add --no-cache curl \
    && addgroup -S riva \
    && adduser -S -G riva -H -s /sbin/nologin riva

WORKDIR /app
COPY --from=build --chown=riva:riva /build/app.jar /app/app.jar

USER riva:riva

EXPOSE 8080
# Porta de gestao (Actuator) separada em producao - ver application-prod.properties
EXPOSE 9090

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE="prod"

HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
    CMD curl -fsS http://localhost:9090/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
