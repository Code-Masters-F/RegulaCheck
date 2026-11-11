# Decisões

## Adotadas
| Item | Decisão |
|---|---|
| Linguagem/framework | Java + Spring Boot **4.1.2** (fixo, sem SNAPSHOT) |
| Java | `java.version=21` (proposta do documento); compila e roda no JDK 25.0.2 (Temurin) instalado |
| Build | Maven Wrapper 3.3.4 -> Maven 3.9.14 |
| Dependências | web, validation, actuator, webmvc-test (Boot 4 separou os módulos de teste) |
| Health | Apenas `/actuator/health`, sem detalhes |
| Spring Security | **Adiado**. Não há endpoints de negócio; base apenas local. |
| Banco | **PostgreSQL 18.6 em Docker** (`database/docker-compose.yml`, porta 5440). Spring Data JPA + driver PostgreSQL adicionados; esquema por scripts SQL, `ddl-auto: none` |
| IA | **Gemini provável** (não confirmado). Nenhum SDK instalado; variáveis `AI_*` reservadas |
| Mobile | **Android nativo com Kotlin** (confirmado) |
| Spring Boot | Versão recomendada pelo Spring Initializr em 11/11/2026 (4.1.2) |

## Pendências
- Confirmar Gemini e o modelo; provedor/biblioteca de extração (OCR).
- Autenticação/Security e documentação OpenAPI.
