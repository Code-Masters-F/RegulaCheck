# Implementation Plan: Perfis e isolamento de dados (RC-05)

**Branch**: `005-perfis-isolamento` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-perfis-isolamento/spec.md`

## Summary

Entrega a **regra de autorização reutilizável** sobre a autenticação da RC-04, sem criar rotas de negócio. Quatro peças: (1) um **filtro de perfil** (`AutorizacaoFiltro`) que, depois do `AutenticacaoFiltro`, recusa com 403 o perfil não permitido na rota; (2) um **registro de rotas privadas** (`RotasPrivadas`) pelo qual toda rota privada é declarada com seus perfis; não se registra rota sem perfil, e um teste de omissão falha o build se houver rota privada publicada fora do registro; (3) um auxiliar de **dona do recurso** (`Propriedade`) que transforma "não achei ou não é seu" no mesmo 404 e obriga a busca já filtrada pela dona; (4) a **credencial do simulador** (chave `IOT_API_KEY`, cabeçalho próprio) com um `SimuladorFiltro` que só serve à rota de telemetria. Nenhuma tabela nova, nenhuma alteração no Oracle. A prova é uma matriz de testes de servlet com rotas de exemplo existentes só no ambiente de teste, mais a documentação do contrato (perfil por rota, 401/403/404 e ordem de verificação) e um guia para as próximas histórias.

## Technical Context

**Language/Version**: Java 21 (JDK 25 funciona), como nas histórias anteriores.

**Primary Dependencies**: nenhuma nova. Reaproveita Jetty 12 EE10 Servlet, Jackson, SLF4J e JUnit já no `pom.xml`.

**Storage**: nenhuma mudança. O perfil já vem de `RC_USUARIO.PERFIL` (lido a cada requisição pela RC-04). A dona dos recursos futuros é `RC_SOLICITACAO.PACIENTE_ID`; documento, análise, resumo, submissão e sessão de captura chegam a ela pela solicitação (cadeia de FK da RC-03). A chave do simulador é configuração (`IOT_API_KEY`), fora do banco.

**Testing**: JUnit 5, servlets em Jetty real em porta livre, repositórios em memória e relógio controlável (padrão da RC-04), sem banco no `mvnw clean verify`. Sem teste novo contra o Oracle: a história não toca o banco.

**Target Platform**: JVM 21+ em Windows/Linux.

**Project Type**: Web service (monólito modular, módulo Maven único `backend/`).

**Performance Goals**: sem meta nova. O filtro de perfil não consulta o banco (o perfil já veio com a sessão); a chave do simulador é comparada em tempo constante. Matriz de testes < 1 min (SC-008).

**Constraints**: sem Spring/JPA; negar por omissão; nenhuma resposta ou log com token, chave do simulador, senha ou conteúdo de recurso; respostas no formato padrão de erro; a API continua subindo sem `DB_*` e sem `IOT_API_KEY`.

**Scale/Scope**: ~6 classes novas em `auth/autorizacao`, 1 variável de configuração ativada, ~8 classes de teste, documentação de contrato e um guia. 3 contas sintéticas da RC-03 + 1 chave de teste fictícia.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: contas `*.invalid` da RC-03; chave do simulador fictícia só em teste. |
| II. Limites clínicos | Atende: nenhuma leitura de conteúdo clínico. |
| III. Monólito modular Java sem Spring/JPA | Atende: filtros Servlet e classes simples em `auth`; Controller não acessa Repository; nenhuma interface criada sem necessidade (só as de fronteira já existentes). |
| IV. Segurança e autorização por recurso | É a história que a implementa: paciente pela sessão, nunca por id no corpo; 401/403/404 padronizados; negar por omissão; comparação da chave em tempo constante; administrador sem acesso a documentos. |
| V. Regras determinísticas / IA | N/A. |
| VI. Persistência e confiabilidade | Atende: nenhuma alteração de esquema. |
| VII. Verificação honesta | Atende: matriz perfil × recurso × dona com execução real; limites registrados (rotas de negócio ainda inexistentes; vínculo da chave ao paciente fora de escopo). |
| VIII. Código documentado, mudanças mínimas | Atende: JavaDoc em português; só o necessário. |
| Restrições tecnológicas | Atende: nenhuma dependência nova; `IOT_API_KEY` opcional para iniciar. |

**Re-avaliação após o desenho (Phase 1)**: sem violações. Sem itens em *Complexity Tracking*.

## Project Structure

### Documentation (this feature)

```text
specs/005-perfis-isolamento/
├── plan.md              # Este arquivo
├── research.md          # Decisões D1–D10
├── data-model.md        # Modelo de autorização (sem tabelas novas)
├── quickstart.md        # Como verificar e como proteger uma rota nova
├── contracts/
│   └── api-autorizacao.md   # 401/403/404, ordem de verificação, matriz perfil × rota
├── checklists/
│   └── requirements.md
└── tasks.md             # Gerado por /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── auth/
│   ├── autorizacao/                       # novo
│   │   ├── AutorizacaoFiltro.java         # 403 se o perfil da sessão não está na lista da rota
│   │   ├── RotasPrivadas.java             # declara rota + perfis; registra os filtros; lista para detectar omissão
│   │   ├── Propriedade.java               # Optional da busca já filtrada pela dona -> recurso ou 404 idêntico
│   │   ├── SimuladorFiltro.java           # credencial do simulador (só telemetria); 401/403
│   │   └── Erros.java                     # fábricas de ExcecaoNegocio: acessoNegado(), naoEncontrado()
│   └── controller/AutenticacaoFiltro.java # + reconhece a chave do simulador em rota de conta (403)
├── usuario/entity/Perfil.java             # javadoc atualizado (regras agora existem)
├── config/
│   ├── Configuracao.java                  # + chaveSimulador (IOT_API_KEY), oculta no toString
│   └── Composicao.java                    # usa RotasPrivadas p/ /auth/sessao e /auth/logout
backend/src/test/java/br/com/regulacheck/auth/autorizacao/
├── ErrosTest, PropriedadeTest, AutorizacaoFiltroTest, RotasPrivadasTest   # unidades do mecanismo
├── MatrizPacienteTest.java                # 5 tipos × operações × dona (própria, alheia, inexistente)
├── MatrizAdministradorTest.java           # 403 em recursos individuais; indicadores agregados
├── OrdemDeVerificacaoTest.java            # 401 → 403 → 404 → 400; rotas públicas
├── OmissaoDeRotaTest.java                 # varre o contexto: rota fora do mecanismo é detectada
├── SimuladorFiltroTest / SimuladorEndpointTest   # chave: só telemetria; 403 nas demais; contas: 403 na telemetria
├── HigieneDeLogAutorizacaoTest.java       # nenhuma recusa registra token, chave ou conteúdo
└── RecursosDeExemplo, RotasDeExemplo      # recursos e rotas de exemplo só de teste
docs/
├── api/{erros.md,openapi.yaml,contratos-dados.md,README.md,autorizacao.md (guia),RegulaCheck.postman_collection.json}
├── arquitetura.md, decisoes.md, ambiente.md
CONTRATO_API.md, .env.example, mobile/README.md
```

**Structure Decision**: segue `CLAUDE.md` §6 (`auth` contém controller, service, dto) e acrescenta o subpacote `auth/autorizacao`, porque as quatro peças são transversais a todas as funcionalidades e não pertencem a nenhuma delas. Nenhum módulo Maven novo; sem `utils`.

## Complexity Tracking

Sem desvios: nenhuma dependência, tabela ou módulo novo.
