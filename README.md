# RIVA Backend

API REST desenvolvida em Java com Spring Boot para o projeto RIVA — Ford FIAP.

## Tecnologias

- Java 17
- Spring Boot 3.2.4
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- PostgreSQL
- Lombok

## Arquitetura

O projeto segue o padrão MVC organizado em camadas:

```
src/main/java/com/ford/riva/
├── controller/   # Recebe as requisições HTTP
├── service/      # Regras de negócio
├── repository/   # Acesso ao banco de dados
└── model/        # Entidades JPA
```

## Pré-requisitos

- Java 17+
- Maven 3.8+
- PostgreSQL com os databases `riva_dev` e `riva_prod` criados

```sql
CREATE DATABASE riva_dev;
CREATE DATABASE riva_prod;
```

## Configuração

O projeto usa profiles do Spring Boot para separar os ambientes.

| Profile | Banco | Uso |
|---------|-------|-----|
| `dev`   | `riva_dev`  | Homologação |
| `prod`  | `riva_prod` | Produção |

Configure as variáveis de ambiente antes de subir:

```env
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=sua_senha
```

## Como rodar

```bash
# Clonar o repositório
git clone https://github.com/FORD-FIAP/riva-backend.git
cd riva-backend

# Rodar em dev (homologação)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Rodar em prod
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Build

```bash
mvn clean package

java -jar target/riva-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```
