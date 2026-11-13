# Implementation Plan: Enviar e consultar documentos (RC-08)

**Branch**: `009-enviar-consultar-documentos` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-enviar-consultar-documentos/spec.md`

## Summary

A paciente anexa arquivos ao próprio **rascunho**, vê a lista dos documentos com o estado de cada envio e abre o arquivo de volta. Três rotas novas, todas privadas e só para `PACIENTE`, sob o caminho do pedido:

| Rota | Efeito |
|---|---|
| `POST /solicitacoes/{id}/documentos` | envia **um** arquivo (multipart) com o tipo documental; 201 novo, 200 se for repetição da mesma tentativa |
| `GET /solicitacoes/{id}/documentos` | lista os documentos disponíveis do pedido |
| `GET /solicitacoes/{id}/documentos/{docId}` | baixa o arquivo (anexo), só a dona e só pelo pedido ao qual pertence |

O pedido (`GET /solicitacoes/{id}`) passa a informar a **versão do conjunto documental**, a quantidade de documentos disponíveis e que **a análise não foi realizada**.

Abordagem técnica, por camadas, reaproveitando RC-04/05/06/07:

1. **Pacote `documento`** (controller → service → repository → entity/dto) já previsto no CLAUDE.md §6, com `package-info.java` existentes. As rotas ficam sob `/solicitacoes/*`, que já pertence ao `SolicitacaoController` (o mecanismo da RC-05 recusa dois registros para o mesmo caminho): ele **delega** os subcaminhos `/{id}/documentos[/{docId}]` ao `DocumentoController`, sem herança.
2. **Reuso integral da RC-06**: `ConferenciaDeArquivo` decide aceite/recusa (tipo real, 5 MB, 5 páginas, pixels), `ArmazenamentoPrivado` guarda com nome interno aleatório, `DownloadSeguro` entrega como anexo, `LimitesArquivo.MAXIMO_ARQUIVOS` = 3. Nada disso é reescrito.
3. **Multipart** com `jakarta.servlet` (`request.getPart`), configurado no servlet com limites de tamanho e pasta temporária privada (a quarentena). Partes: `arquivo` (obrigatória) e `tipoDocumental` (obrigatória). Cabeçalho opcional `X-Chave-Envio` é a **identificação de tentativa**.
4. **Uma operação atômica no `DocumentoRepository`** (`registrar`): sob `SELECT … FOR UPDATE` na linha do pedido, confere dona, estado `RASCUNHO`, repetição da tentativa e o limite de 3; em seguida insere o documento, incrementa a revisão, atualiza o pedido e insere a revisão (`DOCUMENTO_ENVIADO`) na **mesma transação**. O bloqueio é o mesmo da RC-07, o que também serializa envio contra troca de serviço.
5. **Ordem de gravação segura**: conferir → (repetição? responde sem gravar nada) → guardar o arquivo → transação do banco. Se a transação falha ou resulta em recusa, o arquivo é removido (`ArmazenamentoPrivado.remover`). Nunca fica registro sem arquivo; arquivo sem registro só por queda do processo entre os dois passos, risco documentado.
6. **Um ajuste de banco, aditivo**: script `07_documento_tipo_e_tentativa.sql` acrescenta a `RC_DOCUMENTO` as colunas `TIPO_DOCUMENTAL`, `CHAVE_ENVIO` e `HASH_CONTEUDO`, com restrições e índice único `(SOLICITACAO_ID, CHAVE_ENVIO)`. O mesmo ajuste entra no `01_` (instalação nova). Não apaga nem altera linhas existentes; é o grupo quem executa.
7. **Documentação e contratos** (`CONTRATO_API.md`, `docs/api/*`, OpenAPI, Postman, modelo de dados, scripts SQL, ambiente) atualizados na mesma entrega.

Nada de extração, análise, resumo, submissão, substituir/remover documento ou tela nesta história.

## Technical Context

**Language/Version**: Java 21 (JDK 25 funciona), como nas histórias anteriores.

**Primary Dependencies**: nenhuma nova. Jetty 12.1 EE10 (`jetty-ee10-servlet`, que já traz o suporte multipart do Servlet 6), HikariCP, driver Oracle, Jackson, SLF4J, PDFBox (já usado pela conferência), JUnit.

**Storage**: Oracle por JDBC (`RC_DOCUMENTO`, `RC_SOLICITACAO`, `RC_REVISAO`) mais a **área privada de arquivos** em disco (`DOCUMENT_STORAGE_PATH`). Única mudança de esquema: três colunas e duas restrições em `RC_DOCUMENTO` (script 07). Nenhum dado é apagado.

**Testing**: JUnit; Jetty real em porta livre com repositórios em memória e área de arquivos em pasta temporária (`ServidorDeTeste`/`CenarioAuth`, como em `DownloadAutenticadoTest`); testes unitários do Service; teste de concorrência sobre o repositório em memória; um teste **opt-in** contra o Oracle real (`RC_TESTE_ORACLE=SIM`) que valida o bloqueio, a restrição única e o limite de 3 de verdade e remove só o que criou. `mvnw clean verify` roda sem banco.

**Target Platform**: JVM 21+ em Windows/Linux.

**Project Type**: Web service (monólito modular, módulo Maven único `backend/`).

**Performance Goals**: envio de um arquivo de até 5 MB conferido, guardado e registrado em menos de 5 s com o Oracle da faculdade; lista e metadados em menos de 1 s.

**Constraints**: pool de no máximo 3 conexões (banco compartilhado) — a conexão só é aberta **depois** da conferência e da gravação do arquivo, e fica presa apenas pela transação curta; nenhum arquivo na memória inteira (leitura em fluxo com limite); sem log de nome de arquivo, conteúdo ou nome interno; API continua subindo sem `DB_*` e sem `DOCUMENT_STORAGE_PATH`; script SQL só aditivo.

**Scale/Scope**: ~16 classes novas de produção (pacote `documento` + ajustes), 1 script SQL, 3 rotas, ~12 classes de teste, atualização de ~12 documentos. Dados 100% sintéticos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: respostas carregam `simulacao: true` onde há pedido; arquivos de teste sintéticos; nenhum texto sugere aprovação oficial. |
| II. Limites clínicos e integridade documental | Atende: o sistema não lê nem interpreta o conteúdo; a paciente não altera documento já enviado (sem substituição nesta história); nenhuma resposta sugere conferência ou aprovação (FR-024). |
| III. Monólito modular Java sem Spring/JPA | Atende: Controller só traduz HTTP e chama Service; Service não toca JDBC; Repository sem regra de caso de uso além da atomicidade (ver Complexity Tracking). Entidade não é serializada: DTOs. Interface só nas fronteiras de banco e de arquivo, já existentes. |
| IV. Segurança e autorização por recurso | Atende: dona sempre pela sessão; busca já filtrada pela dona e amarrada ao pedido do caminho; 404 idêntico para alheio/inexistente/outro pedido; arquivo fora de diretório público, nome interno aleatório, download autenticado como anexo (RC-06); SQL parametrizado; administrador e simulador recusados por `RotasPrivadas`. |
| V. Regras determinísticas, IA subordinada, dados não confiáveis | Atende por omissão: nenhum conteúdo é lido nem enviado a IA/extração; o arquivo é só dado guardado e devolvido. |
| VI. Persistência, revisões e confiabilidade | Atende: sucesso só após `COMMIT`; documento + revisão atômicos; repetição não duplica (restrição única e comparação de conteúdo); concorrência por bloqueio de linha; arquivo removido se o registro falha; script aditivo. |
| VII. Verificação honesta | Atende: concorrência provada em memória e, opt-in, no Oracle real; o que não rodar no banco real é relatado como NÃO VERIFICADO. |
| VIII. Código documentado, mudanças mínimas | Atende: JavaDoc em português; alterações em arquivos de outras histórias listadas em Project Structure e justificadas em Complexity Tracking. |
| Restrições tecnológicas | Atende: sem dependência nova; Oracle mantido; `DB_*` e `DOCUMENT_STORAGE_PATH` continuam opcionais para subir. |

**Re-avaliação após o desenho (Phase 1)**: sem violações. Os pontos que merecem justificativa estão em Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/009-enviar-consultar-documentos/
├── plan.md                      # Este arquivo
├── research.md                  # Decisões D1–D12
├── data-model.md                # Colunas novas, script 07, DTOs, invariantes
├── quickstart.md                # Como verificar e exercitar as rotas
├── contracts/
│   └── api-documentos.md        # Rotas, entradas, saídas, erros e ordem de verificação
├── checklists/
│   └── requirements.md          # Qualidade da especificação (já passou)
└── tasks.md                     # Gerado por /speckit-tasks (não criado aqui)
```

### Source Code (repository root)

```text
backend/src/main/java/br/com/regulacheck/
├── documento/
│   ├── entity/Documento.java                       # registro imutável de RC_DOCUMENTO (sem caminho interno exposto)
│   ├── entity/TipoDocumental.java                  # enum fechado: ENCAMINHAMENTO, IDENTIFICACAO, AUTORIZACAO_RESPONSAVEL, OUTRO
│   ├── entity/EstadoEnvio.java                     # enum fechado; hoje só ENVIADO
│   ├── repository/DocumentoRepository.java         # contrato (fronteira do banco)
│   ├── repository/DocumentoRepositoryJdbc.java     # SQL parametrizado, transação curta com FOR UPDATE
│   ├── repository/ResultadoRegistro.java           # NAO_ENCONTRADO | FORA_DE_RASCUNHO | LIMITE | CONFLITO_CHAVE | REPETIDO | REGISTRADO
│   ├── service/DocumentoService.java               # casos de uso: enviar, listar, abrir
│   ├── dto/DocumentoResponse.java                  # saída (sem caminho interno, sem hash)
│   ├── dto/ListaDocumentosResponse.java
│   ├── dto/EnvioDocumento.java                     # entrada já validada (tipo, chave, partes)
│   └── controller/DocumentoController.java         # POST/GET sob /solicitacoes/{id}/documentos
├── solicitacao/
│   ├── controller/SolicitacaoController.java       # + delega subcaminhos /documentos ao DocumentoController
│   ├── dto/SolicitacaoResponse.java                # + versaoConjuntoDocumental, documentosDisponiveis, analise
│   ├── entity/Solicitacao.java                     # + documentosDisponiveis
│   ├── entity/MotivoRevisao.java                   # (já tem DOCUMENTO_ENVIADO)
│   └── repository/SolicitacaoRepositoryJdbc.java   # + subconsulta de contagem em COLUNAS_PEDIDO
├── auth/autorizacao/RotasPrivadas.java             # + registrar com MultipartConfigElement
└── config/
    ├── Composicao.java                             # monta armazenamento, conferência e DocumentoService
    ├── Dependencias.java                           # + documentos
    └── Servidor.java                               # monta DocumentoRepositoryJdbc

backend/src/test/java/br/com/regulacheck/
├── documento/ …                                    # service, endpoint, matriz de perfis, concorrência, idempotência, higiene de log, Oracle opt-in
├── solicitacao/ …                                  # ajustes nos testes que constroem Solicitacao
└── auth/CenarioAuth.java (+1 dependência)          # DocumentoRepositoryMemoria e pasta temporária de arquivos

database/scripts/
├── 01_criar_esquema.sql                            # colunas e restrições novas (instalação nova)
├── 07_documento_tipo_e_tentativa.sql               # NOVO, aditivo, para bancos já criados
├── 04_testes_restricoes.sql                        # + casos de tipo documental e chave de envio
└── README.md                                       # ordem e efeito do 07

docs/ e raiz (atualização)
├── CONTRATO_API.md · docs/api/{contratos-dados,estados-e-revisoes,erros,protecoes,autorizacao,README}.md · docs/api/openapi.yaml
├── docs/api/RegulaCheck.postman_collection.json
├── docs/modelo-de-dados.md · docs/decisoes.md · docs/arquitetura.md · docs/ambiente.md · README.md
└── CLAUDE.md (apenas o bloco SPECKIT, apontando este plano)
```

**Structure Decision**: segue o CLAUDE.md §6 (`documento` com controller/service/repository/entity/dto; `armazenamento` já existe). O `SolicitacaoController` ganha um roteamento por subcaminho, em vez de um segundo servlet no mesmo prefixo, porque a RC-05 proíbe dois registros para o mesmo caminho e `docs/api/autorizacao.md` já prevê essa evolução.

## Complexity Tracking

> Nenhuma violação do gate. Estes pontos merecem justificativa.

| Ponto | Por que é necessário | Alternativa mais simples rejeitada porque |
|---|---|---|
| `DocumentoRepository.registrar` decide o desfecho (não encontrado / fora de rascunho / limite / conflito de chave / repetido / registrado) dentro de uma transação | Estado, contagem, chave de envio e revisão precisam ser lidos e gravados **sob o mesmo bloqueio de linha**; separar leitura (Service) e gravação (Repository) reintroduz a checagem ingênua seguida de gravação que o CLAUDE.md §10 proíbe e permitiria 4 documentos sob concorrência | Contar no Service e inserir depois deixaria dois envios verem "2 documentos" e ambos gravarem. A decisão de **negócio** (qual erro HTTP e qual mensagem) continua no Service, que só traduz o resultado |
| Script SQL novo (`07_`) que acrescenta colunas a `RC_DOCUMENTO` | O esquema não tem onde guardar o tipo documental, a chave de envio nem o hash do conteúdo (necessário para distinguir repetição de reutilização indevida da chave) | Guardar o tipo no nome do arquivo ou em memória perderia a informação; deduplicar só por hash bloquearia a paciente de reenviar legitimamente o mesmo arquivo com outro tipo. O script é aditivo, com `DEFAULT 'OUTRO'` para as linhas da carga sintética |
| `SolicitacaoController` roteia `/documentos` para outro controller | O caminho `/solicitacoes/*` já está registrado; a RC-05 não aceita registro duplicado e servlet não casa curinga no meio do caminho | Registrar `/documentos/*` separado quebraria o contrato `…/solicitacoes/{id}/documentos` já publicado em `CONTRATO_API.md` |
| `Solicitacao` ganha `documentosDisponiveis` e a consulta do pedido ganha uma subconsulta de contagem | FR-018 pede a contagem na consulta do pedido; calcular por pedido em outra chamada geraria N+1 na listagem de até 50 | Omitir a contagem na lista deixaria o aplicativo sem como mostrar "2 de 3 documentos" sem uma chamada extra por pedido |
| Arquivo em disco gravado **antes** da transação do banco | Evita manter conexão do pool (máx. 3) e linha bloqueada durante I/O de arquivo | Gravar dentro da transação seguraria a conexão e o bloqueio durante a escrita em disco; o custo é a compensação por `remover` quando a transação falha (já prevista pelo `ArmazenamentoPrivado`) |
