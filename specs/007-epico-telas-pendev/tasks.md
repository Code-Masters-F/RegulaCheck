# Tasks: Épico de telas do aplicativo no pen.dev (somente design)

**Input**: `specs/007-epico-telas-pendev/` (spec.md, plan.md, research.md, data-model.md, contracts/telas.md, quickstart.md)

**Branch**: `007-epico-telas-pendev` · **Registro**: depois da aprovação do texto, épico e histórias como issues em Code-Masters-F/ClickBus (T038)

**Natureza**: épico de **design**. Não há teste automatizado de código; a verificação é **revisão** (checklist por história, conferência do inventário, revisão de linguagem sobre `textos.md`, percurso por uma pessoa que não desenhou e aprovação do responsável). Em cada tela o texto é escrito **antes** do desenho. `mvnw clean verify` deve continuar verde (nada de código muda).

**Quem faz**: as tarefas de desenho (T005 em diante, marcadas *Dev*) são da pessoa Dev que assumir o épico, no pen.dev. O assistente pode apoiar o desenho **se** o pen.dev estiver aberto e conectado ao pencil MCP; sem conexão, apoia só nos textos, inventário e documentos.

**Regras desta execução**: só dados sintéticos; sem diagnóstico, urgência, autorização, promessa de vaga ou aprovação do SUS; resumo e extração sempre **simulados**; estado nunca só por cor; arquivos `.pen` só por ferramentas do pen.dev (nunca Read/Grep); nada de commit, push ou issue sem pedido; nenhum segredo nos desenhos.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: US1 entrada · US2 envio · US3 resultado · US4 sensores/administrador/acessibilidade
- Caminhos abreviados: `design` = `mobile/design`, `contrato` = `specs/007-epico-telas-pendev/contracts/telas.md`.

---

## Phase 1: Setup

> Atualização de 13/11/2026: o MCP está conectado e o responsável aprovou a direção visual. O recorte corrente usa `design/RegulaCheck.pen` e `codex/007-design-system-exploracao`; ver T040–T043. As tarefas históricas que exigem conjuntos completos de estados continuam abertas.

- [ ] T001 Confirmar o ambiente: `git status` na branch `007-epico-telas-pendev`; `.\mvnw.cmd clean verify` em `backend/` como base (218 testes, 8 ignorados); com o pen.dev aberto e conectado, rodar `get_app_state` e `read_skill` (pencil MCP) e registrar em `specs/007-epico-telas-pendev/research.md` (nota ao fim de D8, comparando com o que o site https://www.pen.dev/ informa) o que o pen.dev **confirmadamente** faz: variáveis, componentes reutilizáveis, quadros nomeados, **exportação de imagens**; se não conectar, registrar o bloqueio
- [x] T002 [P] Criar a estrutura `design/` com esqueletos (cabeçalhos e aviso de dados sintéticos): `INDEX.md`, `textos.md`, `dados-ficticios.md`, `mapa-de-percursos.md`, `acessibilidade.md`, `checklist-revisao.md`
- [x] T003 [P] `design/dados-ficticios.md`: conjunto único de dados (pessoas A e B e administrador com e-mails `.invalid`; um pedido por estado; PDF de 2 páginas e uma foto; campos extraídos nos três status com origem; pendências por regra do `PROTOCOLO-ACADEMICO-FICTICIO v0.1` com responsável; leituras de sensores em faixa/fora/antiga/indisponível; totais do administrador) conforme `data-model.md`
- [x] T004 [P] `design/acessibilidade.md` e `design/checklist-revisao.md`: metas (contraste ≥ 4,5:1, alvos ≥ 48 dp, texto ≥ 16 sp, nunca só cor, fonte ampliada, rótulos e ordem de leitura), vocabulário: proibido **afirmar** diagnóstico, urgência, autorização de atendimento, "aprovado pelo SUS", vaga garantida, promessa de prazo ou de redução de fila, e **permitido e obrigatório** negar isso nos avisos ("não é aprovação do SUS", "não entra em fila real"); itens de revisão por tela (conteúdo, estados conforme a **matriz tela × estado** do `contrato`, acessibilidade, linguagem, texto puro de símbolos como `<` e `&`, nota de rota/contrato no quadro)

---

## Phase 2: Foundational (bloqueia as histórias)

- [ ] T005 *Dev* Criar `design/00-fundamentos.pen` com **F1** (variáveis: cor com pares de contraste, tipografia com corpo ≥ 16 sp, escala de espaço, formas e altura mínima de toque 48 dp)
- [ ] T006 *Dev* Em `00-fundamentos.pen`, **F2** catálogo de componentes: botão (primário, secundário, destrutivo, desabilitado, carregando), campo de texto (padrão, foco, erro, desabilitado), seletor, cartão de pedido, cartão de pendência, linha de documento, linha de campo extraído, medidor de sensor, faixa de erro e de sem conexão, diálogo de confirmação, esqueleto de carregamento, estado vazio, barra superior, navegação inferior, cartão de total
- [ ] T007 [P] *Dev* Em `00-fundamentos.pen`, **F3** aviso acadêmico e selo **SIMULADO** (com uso e proibições) e **F4** selos dos sete estados do pedido (`RASCUNHO`, `PROCESSANDO`, `REQUER_CONFERENCIA`, `COM_PENDENCIAS`, `SEM_PENDENCIAS_IDENTIFICADAS`, `FALHA_ANALISE`, `SUBMETIDA_SIMULACAO`), todos com texto + ícone
- [ ] T008 Registrar F1–F4 em `design/INDEX.md` e calcular/registrar em `design/acessibilidade.md` a razão de contraste de cada par de cores do catálogo (texto sobre fundo, ícone sobre fundo, estados), apontando qualquer par abaixo da meta

**Checkpoint**: fundamentos prontos e registrados; nenhuma tela das histórias começa antes.

---

## Phase 3: US1 — Entrada: login, lista e detalhe de pedidos (P1)

**Meta**: login, lista e detalhe com todos os estados, sessão expirada e sem permissão. **Teste independente**: percorrer login → lista → detalhe e conferir 21 quadros.

- [x] T009 [US1] `design/textos.md` (seção E1): textos de login (padrão, enviando, credenciais inválidas, serviço indisponível, sem conexão), lista (carregando, vazia com orientação, sucesso, erro, sem conexão), detalhe (próxima ação para cada um dos sete estados, "item não encontrado"), sessão expirada e sem permissão, com o aviso acadêmico; sem linguagem proibida
- [ ] T010 [US1] *Dev* `design/01-entrada.pen` — **E1.1 Login** (5 quadros): padrão; enviando (botão desabilitado com progresso); credenciais inválidas (e-mail mantido, senha limpa); serviço indisponível; sem conexão; sem cadastro, recuperação de senha nem MFA (FR-014)
- [ ] T011 [US1] *Dev* `01-entrada.pen` — **E1.2 Lista de pedidos** (5 quadros): carregando, vazia, sucesso (pedidos em vários estados, com estado, serviço e última atualização), erro e sem conexão, ambos com "tentar novamente"
- [ ] T012 [US1] *Dev* `01-entrada.pen` — **E1.3 Detalhe do pedido** (9 quadros): carregando; um quadro para cada um dos sete estados, com a próxima ação e a revisão atual (`PROCESSANDO` não equivale a concluído; `SEM_PENDENCIAS_IDENTIFICADAS` não é aprovação do SUS); "item não encontrado"
- [ ] T013 [US1] *Dev* `01-entrada.pen` — **E1.4 Sessão expirada** (volta ao login) e **E1.5 Sem permissão**
- [ ] T014 [US1] Anotar em cada quadro a rota da API e o contrato (e, nas células "C" da matriz, o quadro ou componente que cobre o estado) e registrar os 21 quadros em `design/INDEX.md` (rota da API, contrato, rótulos para leitor de tela, ordem de leitura), o percurso de entrada em `design/mapa-de-percursos.md` e aplicar `checklist-revisao.md` à história 1, registrando o resultado (o que foi e o que não foi revisado)

**Checkpoint**: história 1 desenhada e revisada; pode ser aprovada sozinha.

---

## Phase 4: US2 — Envio: rascunho, serviço, documentos, substituição e exclusão (P1)

**Meta**: do rascunho aos documentos, com todas as recusas de arquivo e as confirmações. **Teste independente**: percorrer rascunho → documentos → substituir → excluir rascunho (18 quadros).

- [x] T015 [US2] `design/textos.md` (seção E2): rascunho e serviço com a indicação "protocolo acadêmico fictício, não é exigência oficial"; documentos e limites (3 arquivos, 5 MB, PDF até 5 páginas, JPEG/PNG); as sete recusas com orientação (e quando é preciso selecionar o arquivo de novo); confirmações de substituir, remover e excluir
- [ ] T016 [US2] *Dev* `design/02-envio.pen` — **E2.1 Rascunho e serviço** (4 quadros): padrão, salvando, erro, campos preservados após falha; localidade e serviço demonstrativos com a indicação do protocolo acadêmico
- [ ] T017 [US2] *Dev* `02-envio.pen` — **E2.2 Documentos** (4 quadros): vazia; com arquivos (aguardando, enviando, enviado, falhou) mostrando nome, tipo, tamanho, estado e "abrir"; enviando; interrompido com "tentar de novo" sem sugerir envio concluído
- [ ] T018 [US2] *Dev* `02-envio.pen` — **E2.3 Recusa de arquivo** (7 quadros): tipo não suportado, extensão incompatível, maior que 5 MB, PDF com mais de 5 páginas, PDF protegido, arquivo ilegível, mais de 3 arquivos
- [ ] T019 [US2] *Dev* `02-envio.pen` — **E2.4** confirmar substituição (avisa que invalida a conferência anterior), **E2.5** confirmar remoção e **E2.6** confirmar exclusão do rascunho (nomeia o que será apagado e destaca "Cancelar")
- [ ] T020 [US2] Anotar em cada quadro a rota/contrato e as coberturas "C" da matriz (incluindo a nota de que alterar documento com o pedido em `PROCESSANDO` inicia nova revisão) e registrar os 18 quadros em `INDEX.md`, o percurso de envio em `mapa-de-percursos.md` e aplicar o checklist de revisão à história 2

**Checkpoint**: história 2 desenhada e revisada.

---

## Phase 5: US3 — Resultado, conferência, reenvio e comprovante simulado (P1)

**Meta**: o núcleo da demonstração, com cuidado de linguagem. **Teste independente**: extração → conferência → resultado com pendências → reenvio → sem pendências → comprovante (15 quadros).

- [x] T021 [US3] `design/textos.md` (seção E3): leitura simulada, conferência (campo ausente → unidade emissora), resultado e resumo **simulado**, detalhe de pendência (reenviar × procurar a unidade emissora), reenvio e comparação, comprovante com "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS"; revisar a linguagem proibida
- [ ] T022 [US3] *Dev* `design/03-resultado.pen` — **E3.1 Leitura simulada** (processando, selo SIMULADO) e **E3.2 Conferência da transcrição** (3 quadros): padrão, divergência marcada, campo ausente; valor, origem e status (identificado, não localizado, ilegível) com os não verificáveis destacados por texto + ícone; sem oferecer preencher a informação clínica
- [ ] T023 [US3] *Dev* `03-resultado.pen` — **E3.3 Resultado da análise** (5 quadros): processando, com pendências, requer conferência, sem pendências identificadas (sem promessa), falha na análise (tentar de novo); resumo marcado SIMULADO; o texto de exemplo inclui símbolos como `<` e `&` exibidos como **texto puro** (FR-012)
- [ ] T024 [US3] *Dev* `03-resultado.pen` — **E3.4 Detalhe da pendência** (2 quadros): reenviar (foto cortada ou arquivo ausente) e procurar a unidade emissora (informação clínica ausente), com critério, versão do protocolo, trecho de origem (exibido como texto puro, inclusive com símbolos como `<` e `&`) e responsável
- [ ] T025 [US3] *Dev* `03-resultado.pen` — **E3.5 Reenvio e comparação** (2 quadros): antes do novo resultado (nada marcado como resolvido) e comparação simples entre pendências anteriores e atuais após o novo resultado
- [ ] T026 [US3] *Dev* `03-resultado.pen` — **E3.6 Comprovante da submissão simulada** (2 quadros): padrão e operação repetida (mesmo comprovante), com o destaque obrigatório e sem parecer documento clínico ou protocolo oficial
- [ ] T027 [US3] Anotar em cada quadro a rota/contrato e as coberturas "C" da matriz e registrar os 15 quadros em `INDEX.md`, o percurso de resultado/reenvio/comprovante em `mapa-de-percursos.md` e aplicar o checklist de revisão à história 3, incluindo a busca de termos em `textos.md` (cada ocorrência de "SUS", "fila", "vaga", "urgência" e "aprov" conferida à mão como afirmação ou negação)

**Checkpoint**: história 3 desenhada e revisada.

---

## Phase 6: US4 — Sensores, administrador, conexão ruim e acessibilidade (P2)

**Meta**: completar a demonstração e a qualidade de uso. **Teste independente**: 15 quadros mais a aplicação do guia de acessibilidade às telas.

- [x] T028 [US4] `design/textos.md` (seção E4): alertas de iluminação e distância (fora da faixa, recuperação, desatualizada, indisponível sem bloquear o envio), administrador (protocolo, regras, totais; sem permissão), conexão ruim (tempo esgotado, sem conexão, campos preservados)
- [ ] T029 [US4] *Dev* `design/04-sensores-admin-acessibilidade.pen` — **E4.1 Estação de captura** (6 quadros): normal, luminosidade fora da faixa, distância fora da faixa, recuperada, leitura desatualizada, sensores indisponíveis; lux e cm com unidade
- [ ] T030 [US4] *Dev* `04-...pen` — **E4.2 Visão do administrador** (3 quadros): padrão (protocolo ativo, regras, total de pedidos, análises por resultado, regras com mais pendências), carregando e sem permissão para paciente; nenhum documento nem dado que identifique paciente
- [ ] T031 [US4] *Dev* `04-...pen` — **E4.3 Conexão ruim** (2 quadros): tempo esgotado e sem conexão, com mensagem, "tentar novamente" e campos digitados preservados; nunca declara envio concluído sem confirmação
- [ ] T032 [US4] *Dev* `04-...pen` — **E4.4 Guia de acessibilidade aplicado** (1 quadro de anotações + 3 quadros com **fonte ampliada** de E1.2, E3.3 e E2.2, sem cortar conteúdo essencial) e aplicar a lista de `acessibilidade.md` a **todos** os quadros das quatro histórias, registrando o que foi conferido e as correções feitas
- [ ] T033 [US4] Anotar em cada quadro a rota/contrato e as coberturas "C" da matriz e registrar os 15 quadros em `INDEX.md`, os percursos de sensores, administrador e conexão ruim em `mapa-de-percursos.md` e aplicar o checklist de revisão à história 4

**Checkpoint**: quatro histórias desenhadas e revisadas.

---

## Phase 7: Fechamento e registro

- [ ] T034 Conferência de consistência: os 73 quadros do `contrato` batem com `INDEX.md`, com os quadros dos `.pen` (pelas ferramentas do pen.dev) e com `textos.md`; cada célula da **matriz tela × estado** está cumprida (quadro próprio ou nota de cobertura); **o aviso acadêmico está presente em 100% das telas e o selo SIMULADO em 100% das telas com extração, resumo ou comprovante** (conferido quadro a quadro, SC-002); busca de termos em `textos.md` e `INDEX.md` listando as ocorrências de "SUS", "fila", "vaga", "urgência" e "aprov", cada uma conferida à mão como afirmação (0 permitidas) ou negação/aviso (permitida); `.\mvnw.cmd clean verify` segue verde e `VarreduraDeSegredosTest` (que cobre `mobile/`) não acusa nada
- [ ] T035 [P] Exportar a prévia PNG de cada quadro para `design/previas/` (o site do pen.dev informa exportação em PNG, JPEG, WEBP e PDF; confirmar no aplicativo conforme T001); se a exportação não existir ou falhar, registrar em `INDEX.md` a descrição curta de cada quadro e que a revisão é feita no aplicativo
- [x] T036 [P] Atualizar `mobile/README.md` (seção "Design das telas": onde ficam os arquivos, como abrir, regras de conteúdo) e `docs/decisoes.md` (decisão do épico de telas no pen.dev, pasta `mobile/design/`, limites e pendências)
- [ ] T037 Percurso principal por **uma pessoa que não desenhou** usando só `mapa-de-percursos.md` (SC-005, meta 5 minutos) e localização da rota da API de três telas sorteadas (SC-006, meta 2 minutos); registrar tempos e dificuldades, ou registrar "não medido"
- [x] T038 **Mostrar ao responsável** o texto do épico e das quatro histórias (título, objetivo, critérios de aceite, lista de quadros, dependência de RC-07 a RC-22) e, **somente com a aprovação dele**, criar em Code-Masters-F/ClickBus 1 issue de épico e 4 issues de história, com links para a spec; as issues originais (RC-07 … RC-22) **não** são alteradas nem fechadas; registrar os números criados
- [ ] T039 Fechar `tasks.md` com **Notas de execução** e **Relatório final** distinguindo desenhado, revisado, aprovado, não verificado e fora do escopo; atualizar o Status de `spec.md` e as notas do checklist; **sem commit/push** sem pedido

---

## Dependências e ordem de execução

- Phase 1 → Phase 2 → Phases 3, 4, 5 e 6 (podem ser feitas em paralelo por pessoas diferentes depois dos fundamentos; para uma pessoa só, na ordem 3 → 4 → 5 → 6) → Phase 7.
- Em cada história: textos (T009/T015/T021/T028) → quadros → registro e revisão (T014/T020/T027/T033).
- T008 depende de T005–T007; T032 depende dos quadros das quatro histórias; T034–T039 só depois de todas as demais. T038 exige aprovação explícita do responsável.
- **[P]** seguros em paralelo: T002, T003, T004, T007, T035, T036.
- A história 3 usa os selos de estado de F4 e o catálogo de F2; a história 4 usa o medidor de sensor de F2.

## Rastreabilidade

| Requisito | Tarefas |
|---|---|
| FR-001, FR-005 | T002, T014, T020, T027, T033, T034 |
| FR-002 | T010–T013, T016–T019, T022–T026, T029–T031 |
| FR-003 | T005–T008 |
| FR-004 | T014, T020, T027, T033 (rota e contrato por quadro em `INDEX.md`) |
| FR-006, FR-027 | T004, T014, T020, T027, T033, T037 |
| FR-007, FR-008, FR-009, FR-010, FR-011, FR-012, FR-013 | T004, T007, T009, T015, T021, T028, T034 |
| FR-014, FR-015 | T009–T013 |
| FR-016, FR-017, FR-018 | T015–T019 |
| FR-019 a FR-022 | T021–T026 |
| FR-023, FR-024, FR-025 | T028–T031 |
| FR-026 | T004, T008, T032 |
| SC-001 | T034 |
| SC-002, SC-003 | T004, T034 |
| SC-004 | T008, T032 |
| SC-005, SC-006 | T037 (só são dados como verificados se medidos com pessoa) |
| SC-007 | T038 e aprovação por história (registrar em cada T014/T020/T027/T033) |

## Estratégia de implementação

1. **MVP**: Phases 1–3 (fundamentos + entrada) já permitem ao grupo ver a identidade visual e o primeiro contato da paciente.
2. Phases 4 e 5 entregam o percurso principal da demonstração (envio e resultado).
3. Phase 6 completa sensores, administrador e acessibilidade.
4. Phase 7 prova a consistência, mede o uso com uma pessoa e só então registra o épico no ClickBus.

---

## Notas de execução (implementação parcial, em 12/11/2026)

**O que foi feito sem o pen.dev** (tarefas marcadas): T002, T003, T004 (documentos de apoio em `mobile/design/`), T009, T015, T021, T028 (textos de E1 a E4 em `mobile/design/textos.md`) e T036 (`mobile/README.md` e `docs/decisoes.md`).

**O que NÃO foi feito e por quê:**
- **T001 (parcial, não marcada):** `git status` e a base `.\mvnw.cmd clean verify` foram confirmados (218 testes, 0 falhas, 0 erros, 8 ignorados), mas `get_app_state` e `read_skill` do pencil MCP **falharam** ("failed to connect to running Pencil app: antigravity_ide ... transport not connected"). As capacidades reais do pen.dev (variáveis, componentes, quadros nomeados, exportação) **não foram confirmadas**; só o que o site https://www.pen.dev/ informa (ver `research.md`, D8).
- **T005 a T008, T010 a T014, T016 a T020, T022 a T027, T029 a T033, T035:** são desenho e registro dependentes do pen.dev e da pessoa Dev; **nenhum quadro existe**. Os arquivos `.pen` não foram criados. `INDEX.md` lista os 73 quadros como "a desenhar".
- **T034, T037, T038, T039:** dependem dos quadros, de uma pessoa que percorra o mapa e da aprovação do responsável. **Nenhuma issue foi criada no ClickBus.**

**Verificações realmente executadas:**
- `.\mvnw.cmd clean verify`: 218 testes, 0 falhas, 0 erros, 8 ignorados. O `VarreduraDeSegredosTest` varreu `mobile/design/` sem achados.
- Busca de termos em `textos.md` (parte da revisão de linguagem, sem quadros): SUS 6 linhas, fila 1, vaga 2, aprov 2, garant 2, prazo 1; urgência, autorização e diagnóstico 0. Cada ocorrência foi lida: **todas são negações, avisos ou o nome do documento "cartão do SUS"**; 0 afirmações. Esta busca cobre só o texto: não prova nada sobre os desenhos, que não existem.

**Desvio:** `textos.md` foi escrito para todas as quatro histórias de uma vez (T009, T015, T021, T028), antes do desenho, como o plano manda; os textos podem precisar de ajuste quando os quadros forem desenhados, e o desenho deve seguir o texto (ou o texto ser atualizado junto).

## Relatório parcial do épico

### Entregue (documentos)
`mobile/design/`: `dados-ficticios.md`, `acessibilidade.md` (metas e lista; razões de contraste e registro da aplicação **a preencher**), `checklist-revisao.md` (revisões de E1 a E4 marcadas "não feita"), `textos.md` (E1 a E4 e F3), `INDEX.md` (73 quadros, todos "a desenhar") e `mapa-de-percursos.md`. `mobile/README.md` e `docs/decisoes.md` atualizados.

### Não entregue
Os 5 arquivos `.pen`, os 73 quadros, as prévias, a revisão por história, a medição com uma pessoa (SC-005, SC-006), a aprovação do responsável (SC-007) e as issues no ClickBus.

### Próximo passo
Abrir o pen.dev conectado ao pencil MCP e executar T001 por inteiro; depois, os fundamentos (T005 a T008) e as histórias. A pessoa Dev assume o desenho.

## Registro no ClickBus (T038, 12/11/2026)
A pedido direto do responsável ("criar um épico no projects para um desenvolvedor pegar e desenvolver ele mesmo"), foram criadas em Code-Masters-F/ClickBus, **sem alterar nem fechar nenhuma issue existente**:
- Épico **#51** "Épico 10 — Telas do aplicativo (pen.dev)" (P0)
- **#52** RC-36 fundamentos e entrada (P0) · **#53** RC-37 envio (P0) · **#54** RC-38 resultado, conferência e comprovante (P0) · **#55** RC-39 sensores, administrador, conexão ruim e acessibilidade (P1)
- Todas no Project "RegulaCheck - termina em 19/11/2026" com Status **Todo**; as quatro histórias vinculadas como **sub-issues** do épico. Sem responsável atribuído (a pessoa Dev se atribui) e sem estimativa de horas (a definir pela pessoa Dev). Os prazos de 14, 15, 16 e 17/11 são **sugestões** dentro do prazo do Épico 6.
- O texto das issues **não foi mostrado ao responsável antes da criação** (a tarefa T038 previa isso); ele pode ser editado nas issues.
- Os arquivos citados nas issues estão na branch `007-epico-telas-pendev`, **ainda sem commit e sem merge**: até lá, a pessoa Dev não os encontra na `main`.

## Recorte autorizado — identidade e continuidade (13/11/2026)

Execução em `codex/007-design-system-exploracao`, vinculada a este épico sem criar outra feature. As tarefas abaixo não substituem nem concluem os conjuntos completos de estados acima.

- [x] T040 Registrar aprovação da identidade e reconciliar o recorte corrente em `spec.md`, `plan.md`, `tasks.md` e `design/README.md`; manter requisitos e limites constitucionais e conferir a consistência antes do desenho.
- [x] T041 Transpor Meus pedidos (E1.2), Documentos (E2.2) e detalhe de pendência/unidade emissora (E3.4) como três quadros editáveis em `design/RegulaCheck.pen`, com papéis tipográficos, componentes reutilizados e cores exatas; identificar biblioteca anterior como em revisão.
- [x] T042 Rever screenshots, conteúdo, avisos, limites dos elementos e contraste dos pares usados; salvar pelo MCP e exportar prévias para `design/previas/`. Registrar IDs e limitações; não afirmar validação Android, acessibilidade completa ou conclusão dos 73 estados.
- [x] T043 Entregar o ponto de retomada curto em `design/README.md`, com decisões, caminhos, conexões, ferramentas, localização segura da configuração da chave Refcat, links para evidências e próximos passos; atualizar instruções permanentes e índice.

Dependências: T040 → T041 → T042 → T043. O ambiente de 13/11 já dispõe de variáveis e componentes do trabalho anterior; este recorte autorizado consolida três aplicações da identidade, sem declarar concluída a fase completa de fundamentos ou qualquer história com todos os estados.

**Evidência final do recorte:** arquivo salvo pelo MCP; três PNG 2× exportados; consulta final de instâncias resolvidas confirmou as duas famílias tipográficas, nenhum preenchimento raster e zero problemas de recorte nas três telas. Seis componentes atuais reutilizados. Pares de texto usados ≥4,5:1. Checklist de qualidade de requisitos existente: 16 marcados, 0 pendentes, sem alterar marcações. Ver `design/verificacao-2026-11-13.md` para o que não foi validado. Não foram executados testes Java/Android nem ações no ClickBus nesta etapa; as notas de 12/11 acima são históricas.

## Recorte autorizado — design system v1.0 (13/11/2026)

Pedido do responsável: avaliar as três telas novas e refazer fundamentos e identidade anteriores com base nelas, para guiar a construção das telas. Mesmo arquivo `design/RegulaCheck.pen` e branch `codex/007-design-system-exploracao`; sem nova feature.

- [x] T044 Avaliar as telas `H0BJi`, `CvQRm`, `XCNVm` e os quadros F1–F4 anteriores; registrar inconsistências (escala tipográfica, padding, padrão de estado, superfície, altura de controle, cores fora da direção) em `design/verificacao-2026-11-13.md`.
- [x] T045 Definir tokens semânticos (cor, tamanho tipográfico, espaço, controle) sobre os primitivos existentes e redesenhar F1 `IkCIw` (princípios, cor com contraste, tipografia, espaço/forma/toque, ícones, faça e evite); calcular contraste de todos os pares e registrar em `mobile/design/acessibilidade.md`.
- [x] T046 Redesenhar F2 `t9lvSH` (ações, campos, estrutura, objetos do percurso, sensores, retorno), F3 `EVIof` (aviso, SIMULADO, resumo e comprovante simulados, linguagem) e F4 `HBByt` (7 estados do pedido, 4 do arquivo, 4 por critério) como componentes reutilizáveis; arquivar a biblioteca anterior em `Wo9WQ` com prefixo `Legado/`, sem apagá-la.
- [x] T047 Reconstruir as três telas só com instâncias v1.0, conferir limites e screenshots, exportar prévias, salvar pelo MCP e atualizar `mobile/design/design-system.md`, `mobile/design/INDEX.md` e `design/README.md`.

Dependências: T044 → T045 → T046 → T047. Cobertura das tarefas históricas: substancialmente T005, T007 e T008 (no arquivo único, não em `00-fundamentos.pen`) e T006 **exceto navegação inferior**, deixada fora por decisão de desenho pendente de confirmação do grupo; por isso T005–T008 continuam desmarcadas. Nenhum teste Java/Android executado (sem mudança de código). Não validado: fonte ampliada, TalkBack, teclado/insets, uso com pacientes.
