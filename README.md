# RegulaCheck

Verificador de completude documental de encaminhamentos (MVP acadêmico, **dados sintéticos**, submissão **simulada**). O sistema não diagnostica, não define urgência e não autoriza atendimento.

## Estado real da preparação
Somente a **fundação do backend** foi criada e verificada (Java 21 / Spring Boot 4.1.2 / Maven Wrapper): build, testes e health OK. Banco (PostgreSQL) e mobile (Android/Kotlin) estão decididos, mas ainda não implementados; IA/extração, sensores e segurança **não** estão implementados nem decididos (ver [docs/decisoes.md](docs/decisoes.md)).

## Executar (a partir de `backend/`)
- Windows: `.\mvnw.cmd clean verify` e `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`
- Linux/macOS: `./mvnw clean verify` e `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

Pré-requisito: JDK 21+. Health: http://localhost:8080/actuator/health. Detalhes e solução de problemas em [docs/ambiente.md](docs/ambiente.md).

## Estrutura
`backend/` API | `mobile/` | `simulador-iot/` | `database/` | `docs/` | `samples/` | `entrega/`. Arquitetura em [docs/arquitetura.md](docs/arquitetura.md).
