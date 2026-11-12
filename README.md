# RegulaCheck

Verificador de completude documental de encaminhamentos (MVP acadêmico, **dados sintéticos**, submissão **simulada**). O sistema não diagnostica, não define urgência e não autoriza atendimento.

## Decisões de tecnologia (12/11/2026)
- Backend em **Java puro** (Jakarta Servlet em servidor embutido), **sem Spring e sem JPA** em produção; Spring Boot só em testes automatizados.
- Banco **Oracle fornecido pela faculdade**, acesso por JDBC.
- **2 sensores simulados por script Java**, enviando leituras em JSON por HTTP.
- **IA e extração/OCR mockadas**, sempre identificadas como simuladas.
- Mobile: Android nativo com Kotlin (decidido, não criado).
- Desenvolvimento conduzido com o GitHub Spec Kit (ver `CLAUDE.md` e `.specify/`).

Detalhes e pendências em [docs/decisoes.md](docs/decisoes.md). Recorte e regras do demonstrador em [docs/protocolo-academico.md](docs/protocolo-academico.md); tecnologias e limitações em [docs/escopo-e-limitacoes.md](docs/escopo-e-limitacoes.md).

## Estado real da preparação
A **fundação existente em `backend/` ainda é a versão antiga** (Spring Boot 4.1.2, JPA, PostgreSQL) e **precisa ser migrada** para a stack acima; ela não representa a stack final. Não há entidades de negócio, mobile, simulador, segurança nem integração com o Oracle implementados. A migração será feita pelo fluxo do Spec Kit.

## Executar (fundação antiga, até a migração)
Pré-requisito: JDK 21+. A partir de `backend/`:
- Windows: `.\mvnw.cmd clean verify`
- Linux/macOS: `./mvnw clean verify`

Os comandos de execução da stack nova serão documentados após a migração. Detalhes em [docs/ambiente.md](docs/ambiente.md).

## Estrutura
`backend/` API | `mobile/` | `simulador-iot/` | `database/` | `docs/` | `samples/` | `entrega/`. Arquitetura em [docs/arquitetura.md](docs/arquitetura.md).
