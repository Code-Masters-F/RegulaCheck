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

## Estado real
A base técnica de `backend/` já está em Java puro (Jetty + Servlet, sem Spring/JPA) e expõe `GET /health`, a autenticação (`/auth/*`), o catálogo de serviços (`GET /protocolos`), o rascunho da solicitação (`/solicitacoes`, RC-07), o envio, a lista e o download dos documentos do pedido (`/solicitacoes/{id}/documentos`, RC-08) e, na RC-09, remover e substituir documento (`DELETE`/`PUT …/documentos/{docId}`) e excluir o rascunho com confirmação (`DELETE /solicitacoes/{id}` com o cabeçalho `X-Confirmar-Exclusao: EXCLUIR`); a RC-09 não tem script SQL novo e seu teste contra o Oracle (`AlteracaoDocumentoOracleTest`) é opt-in e **ainda não foi executado**. Dentro do código estão implementados e verificados com repositórios em memória; a RC-07 também foi verificada em 13/11/2026 contra o Oracle da faculdade, na conta usada pelo grupo, com o script `database/scripts/06_motivo_revisao_protocolo.sql` aplicado (ver [docs/decisoes.md](docs/decisoes.md)). Ainda **não há** extração, análise, substituição ou remoção de documento, mobile nem simulador implementados. A RC-08 ainda **não foi verificada contra o Oracle real**: depende do script `database/scripts/07_documento_tipo_e_tentativa.sql` e do `DocumentoOracleTest`. Os contratos de dados, erros e estados estão em [docs/api](docs/api/README.md). Desde a RC-10, `POST/GET /solicitacoes/{id}/extracoes` fazem a **leitura simulada** (dados sintéticos predefinidos, sem leitura real) dos documentos do pedido.

## Executar (verificado em 12/11/2026)
Pré-requisito: JDK 21+. A partir de `backend/` (sem banco e sem variáveis):
- Windows: `.\mvnw.cmd clean verify` e depois `java -jar target\regulacheck-backend.jar`
- Linux/macOS: `./mvnw clean verify` e depois `java -jar target/regulacheck-backend.jar`
- Conferir: `curl http://localhost:8080/health` -> `{"status":"UP"}` (UP não prova que o MVP está completo nem que o banco está acessível)

Detalhes, acesso do emulador (`10.0.2.2`) e problemas comuns em [docs/ambiente.md](docs/ambiente.md).

## Estrutura
`backend/` API | `mobile/` | `simulador-iot/` | `database/` | `docs/` | `samples/` | `entrega/`. Arquitetura em [docs/arquitetura.md](docs/arquitetura.md).
