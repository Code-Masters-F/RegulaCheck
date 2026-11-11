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
| Banco | **PostgreSQL confirmado**. Spring Data JPA + driver entram na etapa de persistência |
| Mobile | **Android nativo com Kotlin** (confirmado) |
| Spring Boot | Versão recomendada pelo Spring Initializr em 11/11/2026 (4.1.2) |

## Pendências
- Versão do PostgreSQL e uso de Docker para o banco local.
- Provedores de IA e extração (OCR).
- Autenticação/Security e documentação OpenAPI.
