# Implementation Plan: Criar rascunho e selecionar serviço (RC-07)

**Branch**: `008-rascunho-selecionar-servico` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-rascunho-selecionar-servico/spec.md`

## Summary

Primeira história de negócio da API: a paciente consulta o serviço (protocolo acadêmico) disponível, cria um **rascunho** de solicitação ligado a ela e a esse serviço, troca o serviço enquanto for rascunho, lista os próprios pedidos e reabre um deles. Quatro rotas novas, todas privadas e só para `PACIENTE`:

| Rota | Efeito |
|---|---|
| `GET /protocolos` | lista os serviços ativos (localidade, especialidade, versão, aviso fictício) |
| `POST /solicitacoes` | cria o rascunho (201), revisão 1 |
| `GET /solicitacoes` e `GET /solicitacoes/{id}` | lista e reabre os pedidos da paciente da sessão |
| `PUT /solicitacoes/{id}` | troca o serviço de um rascunho; revisão +1 só se o serviço mudou |

Abordagem técnica, por camadas, reaproveitando o que as RC-04/05/06 entregaram:

1. **Pacotes `protocolo` e `solicitacao`** (controller → service → repository → entity/dto), registrados em `Composicao` por `RotasPrivadas` com `Perfil.PACIENTE`; o mecanismo da RC-05 já dá 401/403 e a regra do 404 padrão (`Propriedade.exigir`).
2. **Repositórios JDBC** com SQL parametrizado e a busca de pedido **já filtrada pela dona** (`PACIENTE_ID = ?` na mesma instrução). Interfaces nas fronteiras com o banco (como `UsuarioRepository`), com implementação em memória só nos testes.
3. **Duas operações atômicas no `SolicitacaoRepository`**: criar (pedido + revisão 1 numa transação) e trocar o serviço (bloqueio da linha com `SELECT … FOR UPDATE`, conferência do estado, `UPDATE` da revisão e `INSERT` da nova revisão na mesma transação). O bloqueio de linha é o que impede salto ou repetição de revisão sob PUTs simultâneos (FR-012).
4. **Um ajuste de banco, aditivo**: o motivo de revisão `PROTOCOLO_ALTERADO` ainda não existe em `RC_REVISAO_CK_MOTIVO`. Novo script `06_motivo_revisao_protocolo.sql` (recria a restrição com o valor a mais; não apaga dados) e a mesma alteração no `01_criar_esquema.sql` para instalações novas.
5. **Validação no servidor** em dois níveis: contrato (corpo, tipo, tamanho → 400 `REQUISICAO_INVALIDA`) e regra (serviço inexistente ou inativo → 422 `REGRA_DE_NEGOCIO` com o campo), mais 409 `CONFLITO_ESTADO` quando o pedido já saiu de rascunho.
6. **Documentação e contratos** (`CONTRATO_API.md`, `docs/api/*`, `openapi.yaml`, Postman, modelo de dados, scripts SQL) atualizados na mesma entrega.

Nada de tela, documento, extração, análise, IA, exclusão de rascunho ou paginação rica nesta história.

## Technical Context

**Language/Version**: Java 21 (JDK 25 funciona), como nas histórias anteriores.

**Primary Dependencies**: nenhuma nova. Reaproveita Jetty 12 EE10 Servlet, HikariCP (`BancoDados`), driver Oracle, Jackson (`Json`), SLF4J e JUnit já no `pom.xml`. Sem Spring/JPA.

**Storage**: Oracle por JDBC. Tabelas já existentes `RC_PROTOCOLO`, `RC_SOLICITACAO` e `RC_REVISAO` (RC-03). Única mudança de esquema: nova restrição `RC_REVISAO_CK_MOTIVO` aceitando `PROTOCOLO_ALTERADO` (script 06; ver data-model.md). Nenhum dado é apagado.

**Testing**: JUnit; Jetty real em porta livre com repositórios em memória (`ServidorDeTeste`, `CenarioAuth`); testes unitários do Service; teste de concorrência sobre o repositório em memória; um teste **opt-in** contra o Oracle real (`RC_TESTE_ORACLE=SIM`), que valida o bloqueio de linha de verdade e remove só as linhas que criou. `mvnw clean verify` roda sem banco.

**Target Platform**: JVM 21+ em Windows/Linux.

**Project Type**: Web service (monólito modular, módulo Maven único `backend/`).

**Performance Goals**: criar, trocar, listar e reabrir respondem em menos de 1 s com o Oracle da faculdade; lista limitada a 50 itens por resposta.

**Constraints**: pool de no máximo 3 conexões (banco compartilhado); transação curta e sem chamada externa; nenhum log com dado pessoal; API continua subindo sem `DB_*` (as rotas novas respondem 503 genérico sem banco); script SQL só aditivo.

**Scale/Scope**: ~16 classes novas de produção (2 pacotes), 1 script SQL, 4 rotas, ~10 classes de teste, atualização de ~10 documentos. Dados 100% sintéticos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: toda resposta do serviço e do pedido carrega o aviso de protocolo fictício e `simulacao: true`; dados de teste sintéticos. |
| II. Limites clínicos e integridade | Atende: nenhum dado clínico é aceito, guardado ou inventado (FR-023); nada de diagnóstico, urgência ou fila. |
| III. Monólito modular Java sem Spring/JPA | Atende: Controller só traduz HTTP e chama Service; Service não toca JDBC; Repository sem regra de caso de uso além da atomicidade (ver Complexity Tracking). Interfaces só nas fronteiras de banco. Entidade não é serializada: DTOs explícitos. |
| IV. Segurança e autorização por recurso | Atende: dono vem sempre da sessão; busca já filtrada pela dona; 404 idêntico para alheio/inexistente/id inválido; perfis `ADMINISTRADOR` e simulador recusados por `RotasPrivadas`; SQL parametrizado; ordem de verificação do `docs/api/erros.md` respeitada. |
| V. Regras determinísticas, IA subordinada | Não se aplica: nenhuma IA, extração ou regra de critério participa; apenas a escolha do conjunto de critérios. |
| VI. Persistência, revisões e confiabilidade | Atende: sucesso só após `COMMIT`; pedido + revisão inicial atômicos; troca de serviço incrementa revisão e registra motivo; concorrência tratada por bloqueio de linha; script SQL aditivo. |
| VII. Verificação honesta | Atende: concorrência provada de duas formas (memória e, opt-in, Oracle real); o que não for executado contra o Oracle é registrado como NÃO VERIFICADO. |
| VIII. Código documentado, mudanças mínimas | Atende: JavaDoc em português; mudanças em arquivos existentes limitadas a `Composicao`, `Dependencias`, `Servidor` e três chamadas de teste. |
| Restrições tecnológicas | Atende: sem dependência nova; Oracle mantido; `DB_*` continuam opcionais para subir. |

**Re-avaliação após o desenho (Phase 1)**: sem violações. Os dois pontos que merecem justificativa estão em Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/008-rascunho-selecionar-servico/
├── plan.md                      # Este arquivo
├── research.md                  # Decisões D1–D9
├── data-model.md                # Tabelas usadas, script 06, DTOs, invariantes
├── quickstart.md                # Como verificar e exercitar as rotas
├── contracts/
│   └── api-solicitacoes.md      # Rotas, entradas, saídas, erros e ordem de verificação
├── checklists/
│   └── requirements.md          # Qualidade da especificação (já passou)
└── tasks.md                     # Gerado por /speckit-tasks (não criado aqui)
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── protocolo/
│   ├── entity/Protocolo.java                     # registro imutável de RC_PROTOCOLO
│   ├── repository/ProtocoloRepository.java       # listarAtivos, buscarAtivoPorProtocoloId
│   ├── repository/ProtocoloRepositoryJdbc.java
│   ├── service/ProtocoloService.java             # catálogo e resolução do protocoloId
│   ├── dto/ProtocoloResponse.java                # saída do catálogo
│   ├── dto/ListaProtocolosResponse.java
│   └── controller/ProtocoloController.java       # GET /protocolos
├── solicitacao/
│   ├── entity/Solicitacao.java                   # pedido + protocolo embutido (leitura)
│   ├── entity/EstadoSolicitacao.java             # enum fechado dos 7 estados
│   ├── entity/MotivoRevisao.java                 # enum fechado dos motivos de RC_REVISAO
│   ├── repository/SolicitacaoRepository.java     # contrato (fronteira do banco)
│   ├── repository/SolicitacaoRepositoryJdbc.java # SQL parametrizado, transações curtas
│   ├── repository/ResultadoTroca.java            # NAO_ENCONTRADA | FORA_DE_RASCUNHO | SEM_MUDANCA | ALTERADA
│   ├── service/SolicitacaoService.java           # casos de uso: criar, trocar, listar, abrir
│   ├── dto/SolicitacaoRequest.java               # { protocoloId }, com validar()
│   ├── dto/SolicitacaoResponse.java              # saída (inclui servico e simulacao)
│   ├── dto/ListaSolicitacoesResponse.java
│   └── controller/SolicitacaoController.java     # /solicitacoes e /solicitacoes/{id}
└── config/
    ├── Composicao.java                           # registra /protocolos e /solicitacoes/*
    ├── Dependencias.java                         # + protocolos, solicitacoes
    └── Servidor.java                             # monta os repositórios JDBC reais

backend/src/test/java/br/com/regulacheck/
├── solicitacao/ …                                # service, endpoint, matriz de perfis, concorrência, Oracle opt-in
├── protocolo/ …                                  # catálogo e endpoint
└── auth/CenarioAuth.java (+2 chamadas)           # novas dependências em memória

database/scripts/
├── 01_criar_esquema.sql                          # motivo PROTOCOLO_ALTERADO na restrição (instalação nova)
├── 06_motivo_revisao_protocolo.sql               # NOVO, aditivo, para bancos já criados
└── README.md                                     # ordem e efeito do 06

docs/ e raiz (atualização)
├── CONTRATO_API.md · docs/api/{contratos-dados,estados-e-revisoes,erros,README}.md · docs/api/openapi.yaml
├── docs/api/RegulaCheck.postman_collection.json
├── docs/modelo-de-dados.md · docs/decisoes.md · docs/arquitetura.md (se citar rotas) · README.md
└── CLAUDE.md (apenas o bloco SPECKIT, apontando este plano)
```

**Structure Decision**: segue a estrutura prevista no CLAUDE.md §6 (`solicitacao` e `protocolo` com controller/service/repository/entity/dto). Os pacotes `package-info.java` já existem e são mantidos. Nenhum pacote novo além dos previstos.

## Complexity Tracking

> Só dois pontos exigem justificativa; nenhum viola o gate.

| Ponto | Por que é necessário | Alternativa mais simples rejeitada porque |
|---|---|---|
| `SolicitacaoRepository.trocarProtocolo` decide o resultado (não encontrada / fora de rascunho / sem mudança / alterada) dentro de uma transação, em vez de só gravar | Estado e revisão precisam ser lidos e gravados **sob o mesmo bloqueio de linha**; separar leitura (Service) e gravação (Repository) reintroduz a checagem ingênua seguida de gravação que o CLAUDE.md §10 proíbe | Ler no Service e gravar depois permitiria dois PUTs lerem a mesma revisão e gravarem o mesmo número, ou alterar um pedido que acabou de sair de rascunho. A decisão de **negócio** (qual erro HTTP, qual mensagem) continua no Service, que só traduz o resultado |
| Script SQL novo que recria `RC_REVISAO_CK_MOTIVO` | `RC_REVISAO.MOTIVO` hoje não aceita registrar a troca de serviço; sem isso a revisão não teria motivo (FR-010) | Reusar `CONFERENCIA_ALTERADA` registraria motivo falso; remover a restrição enfraqueceria o banco. O script é aditivo (nenhum dado é apagado) e o `01_` recebe a mesma alteração para instalações novas |
