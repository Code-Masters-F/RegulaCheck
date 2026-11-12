# Tasks: Perfis e isolamento de dados (RC-05)

**Input**: `specs/005-perfis-isolamento/` (spec.md, plan.md, research.md, data-model.md, contracts/api-autorizacao.md, quickstart.md)

**Backlog**: RC-05 — Code-Masters-F/ClickBus#11 · **Branch**: `005-perfis-isolamento`

**Testes**: solicitados pelo plano e por `CLAUDE.md` §14 (prioridade: autorização entre contas). Em cada fase o teste é escrito **antes** da implementação e deve falhar primeiro. Todos rodam sem banco no `.\mvnw.cmd clean verify`. Esta história **não toca o Oracle**: nenhum script SQL, nenhuma execução no banco da faculdade.

**Regras desta execução**: nada de commit/push sem pedido; nenhuma chave do simulador real em arquivo, log ou resposta (só valor fictício de teste); sem CRUD de negócio, tela ou leitura de sensor funcional (FR-023); rotas de exemplo existem **somente** em `src/test`.

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: história da spec (US1 paciente só vê o que é seu, US2 administrador sem documentos, US3 rotas privadas conferidas no servidor, US4 credencial do simulador, US5 mecanismo reutilizável e verificado)
- Caminhos abreviados: `main` = `backend/src/main/java/br/com/regulacheck`, `test` = `backend/src/test/java/br/com/regulacheck`.

---

## Phase 1: Setup

- [x] T001 Rodar `.\mvnw.cmd clean verify` em `backend/` antes de qualquer alteração e registrar o resultado-base (82 testes, 6 ignorados de Oracle, segundo o fechamento da RC-04); conferir `git status` na branch `005-perfis-isolamento` e que `.env` está ignorado
- [x] T002 [P] Atualizar `.env.example`: `IOT_API_KEY` passa a ser documentada como a chave do simulador (opcional para iniciar; sem ela a telemetria responde 401; valor nunca versionado)

---

## Phase 2: Foundational (bloqueia as histórias)

- [x] T003 [P] `main/config/Configuracao.java`: acrescentar `chaveSimulador` lida de `IOT_API_KEY` (opcional; vazia = ausente); manter os construtores existentes usados pelos testes da RC-04 (inclusive o de 4 argumentos de `AuthOracleTest`); `toString` e qualquer log nunca mostram a chave; teste `test/config/ConfiguracaoTest.java` ampliado antes (ausente, vazia, definida, ausente do `toString`)
- [x] T004 [P] `main/auth/autorizacao/Erros.java`: fábricas `acessoNegado()` (403 `ACESSO_NEGADO`, "Você não tem permissão para esta operação.") e `naoEncontrado()` (404 `NAO_ENCONTRADO`, "Recurso não encontrado."), mensagens fixas conforme `data-model.md`; teste `test/auth/autorizacao/ErrosTest.java` escrito antes (status, código, mensagem; duas chamadas geram resposta idêntica)
- [x] T005 [P] `main/auth/autorizacao/Propriedade.java`: `exigir(Optional<T>)` devolve o recurso ou lança `Erros.naoEncontrado()`; JavaDoc explicando que a busca deve chegar **já filtrada pela dona**; teste `test/auth/autorizacao/PropriedadeTest.java` escrito antes (presente devolve; vazio lança o mesmo 404 para "inexistente" e "alheio")
- [x] T006 `main/auth/autorizacao/AutorizacaoFiltro.java`: roda depois do `AutenticacaoFiltro`, lê a `SessaoAutenticada` do atributo da requisição e responde 403 padrão se o perfil não estiver entre os da rota; sem sessão no atributo (erro de montagem) lança `IllegalStateException`; cada recusa gera uma linha de log com `usuarioId`, perfil, método, caminho sem query e código, nunca token, chave ou corpo (D10); teste escrito antes em `test/auth/autorizacao/AutorizacaoFiltroTest.java`
- [x] T007 `main/auth/autorizacao/RotasPrivadas.java`: `registrar(contexto, caminho, servlet, Perfil…)` instala `AutenticacaoFiltro` + `AutorizacaoFiltro` na ordem certa e guarda a declaração; perfis vazios recusados na montagem (`IllegalArgumentException`); expõe a lista de caminhos declarados e o conjunto de caminhos públicos (`/health`, `/auth/login`, coringa `/*`) para o teste de omissão; teste `test/auth/autorizacao/RotasPrivadasTest.java` escrito antes (registro correto, perfis vazios, caminho repetido)
- [x] T008 `main/config/Composicao.java`: registrar `/auth/sessao` e `/auth/logout` por `RotasPrivadas` (perfis `PACIENTE` e `ADMINISTRADOR`) no lugar dos filtros soltos; acrescentar o ponto de extensão `montar(Configuracao, Dependencias, Consumer<RotasPrivadas>)` chamado **antes** da rota coringa (a assinatura atual continua, com extensão vazia) para que os testes registrem rotas de exemplo pelo mesmo mecanismo; todos os testes da RC-04 seguem verdes

**Checkpoint**: erros fixos, auxiliar de dona, filtro de perfil e registro de rotas prontos e testados isoladamente.

---

## Phase 3: Fixtures de teste (rotas e recursos de exemplo)

- [x] T009 [P] `test/auth/autorizacao/RecursosDeExemplo.java`: repositório em memória com duas pacientes sintéticas (A e B), uma conta de paciente **desativada** com um recurso, um administrador e, para **cada um dos cinco tipos** (solicitação, documento aninhado em solicitação, análise, resumo, submissão), um recurso de A e um de B; a busca recebe a dona e devolve `Optional` já filtrado (padrão D5), inclusive a conferência pai/filho do documento; incluir também sessões de captura de exemplo (uma **aberta** e uma **encerrada**) para a telemetria da Phase 7 e um contador de acessos ao repositório
- [x] T010 [P] `test/auth/autorizacao/RotasDeExemplo.java`: servlets de teste que usam o mecanismo real (`RotasPrivadas`, `Propriedade`, `AutenticacaoFiltro.sessaoDe`): recursos individuais (`GET`, `PUT`, `DELETE`, listagem) para PACIENTE; `GET /teste/admin/indicadores` para ADMINISTRADOR devolvendo só totais; `POST /teste/telemetria/{id}` para o simulador (preenchida na Phase 7). Estender `test/auth/ServidorDeTeste.java` com `iniciarComRotas(...)` que usa o ponto de extensão da T008

---

## Phase 4: US1 — Paciente só enxerga o que é seu (P1)

**Meta**: 404 idêntico para recurso alheio e inexistente, em leitura, alteração, exclusão e listagem.

- [x] T011 [US1] Escrever `test/auth/autorizacao/MatrizPacienteTest.java`: para os **5 tipos** × **GET/PUT/DELETE**, a paciente A recebe sucesso no próprio, 404 no de B e 404 em ID inexistente, e os três corpos de 404 são **idênticos**; B segue com seus dados intactos após as tentativas de A; listagem de A contém só os de A; documento de B pedido pela solicitação de A (rota aninhada) dá 404; ID inválido (texto, negativo, enorme) responde de forma consistente sem erro 500; campo de dono (`pacienteId`, `ownerId`) no corpo **ou como parâmetro de consulta** é ignorado ou recusado com 400 e a dona continua sendo a da sessão; recurso cuja dona foi desativada segue com 404 para A e B (SC-001, FR-001 a FR-006)
- [x] T012 [US1] Fazer T011 passar completando `RotasDeExemplo` (T010) com os servlets dos 5 tipos; nenhum servlet de exemplo acessa o repositório fora de `Propriedade.exigir`; se T011 expuser falha no mecanismo, corrigir `Propriedade`/filtros, não o teste

---

## Phase 5: US2 — Administrador vê protocolo e indicadores, não documentos (P1)

- [x] T013 [US2] Escrever `test/auth/autorizacao/MatrizAdministradorTest.java`: administrador recebe 200 em `/teste/admin/indicadores`; recebe **403** (não lista vazia) em todos os GET/PUT/DELETE e na listagem dos 5 tipos, sem nenhum dado do recurso no corpo; paciente em rota administrativa recebe 403; o corpo do indicador não contém nome, e-mail nem id de paciente (SC-002, FR-007, FR-008)
- [x] T014 [US2] Fazer T013 passar (rotas de recurso declaram só `PACIENTE`; indicador declara só `ADMINISTRADOR`); confirmar que a recusa acontece **antes** de qualquer consulta ao repositório de exemplo (contador de acessos do fixture = 0)

---

## Phase 6: US3 — Todas as rotas privadas conferidas no servidor (P1)

- [x] T015 [US3] Escrever `test/auth/autorizacao/OrdemDeVerificacaoTest.java` (rota de exemplo com dona): sem token, token inválido e expirado → 401 antes de qualquer outra conferência; perfil indevido → 403; dono alheio → 404; corpo inválido numa rota alheia → 404 e **não** 400 (dona antes da validação); corpo inválido na rota própria → 400; método não previsto → 405 só depois de autenticar; `/health` e `/auth/login` continuam públicos; perfil da conta alterado no repositório em memória entre duas chamadas vale já na segunda (FR-013); mensagens de 403 e 404 iguais para qualquer motivo e sem dona, perfil exigido, SQL ou identificador interno (FR-009 a FR-012, SC-003)
- [x] T016 [US3] Escrever `test/auth/autorizacao/OmissaoDeRotaTest.java`: monta o servidor como na produção e, via reflexão sobre os mapeamentos do `ServletContextHandler`, falha se existir caminho que não seja público conhecido nem declarado em `RotasPrivadas`; incluir um caso negativo registrando uma rota **sem** declaração e verificando que o teste de varredura a detecta; verificar também que `RotasPrivadas` recusa na montagem uma declaração sem perfil. Não há guarda em tempo de execução (research D3, limite assumido) (FR-010, SC-004, US5 cenário 3)
- [x] T017 [US3] Fazer T015 e T016 passarem (ajustes em `RotasPrivadas`/`Composicao` se necessário); ordem de verificação documentada no JavaDoc de `AutorizacaoFiltro`

---

## Phase 7: US4 — Credencial do simulador restrita à telemetria (P1)

- [x] T018 [US4] Escrever `test/auth/autorizacao/SimuladorFiltroTest.java` e `SimuladorEndpointTest.java` (chave fictícia `chave-de-teste-ficticia` definida na `Configuracao` do teste): chave válida + sessão de captura aberta → 200; chave válida + sessão inexistente **ou** encerrada → 404 com corpo idêntico; chave ausente ou errada → 401; chave válida em qualquer rota de conta (os 5 tipos, indicadores, `/auth/sessao`, `/auth/logout`) → 403; token de paciente ou de administrador na telemetria → 403; sem `IOT_API_KEY` configurada a telemetria responde 401 e o servidor sobe normalmente; token de conta **junto** com chave válida segue o critério da chave (US4, D8) (SC-005)
- [x] T019 [US4] `main/auth/autorizacao/SimuladorFiltro.java`: lê `X-Chave-Simulador`, compara em tempo constante (`MessageDigest.isEqual` sobre o SHA-256 de ambos), 401 sem credencial válida, 403 se houver token de conta válido sem chave; nunca registra a chave. Em `main/auth/controller/AutenticacaoFiltro.java`, reconhecer chave do simulador válida numa rota de conta e responder 403 (em vez de 401). O `AutenticacaoFiltro` recebe a chave do simulador pelo construtor, vinda da `Configuracao` (`Composicao` repassa). Registrar a telemetria de exemplo em `RotasDeExemplo` apenas no teste; fazer T018 passar
- [x] T020 [US4] Escrever `test/auth/autorizacao/HigieneDeLogAutorizacaoTest.java` (mesma técnica de captura de `System.err` da RC-04, com controle positivo): durante todas as recusas 401/403/404 e usos da chave, nem o log nem as respostas contêm token, chave do simulador, senha ou conteúdo do recurso (FR-017, FR-021, SC-006)

---

## Phase 8: US5 — Mecanismo reutilizável, contrato e guia (P2)

- [x] T021 [US5] Atualizar `CONTRATO_API.md` (coluna de perfil permitido e regra de dona por rota, conforme `contracts/api-autorizacao.md`), `docs/api/erros.md` (casos de uso de 401/403/404, ordem de verificação, mensagens fixas), `docs/api/openapi.yaml` (respostas 403/404 e esquema de segurança do simulador), `docs/api/RegulaCheck.postman_collection.json` e a seção *Situação das rotas* de `docs/api/README.md`
- [x] T022 [P] [US5] Criar `docs/api/autorizacao.md` (guia curto "como proteger uma rota nova": os cinco passos do `quickstart.md`, a matriz a copiar, o que o teste de omissão acusa) e referenciá-lo em `docs/api/README.md`
- [x] T023 [P] [US5] Atualizar `docs/arquitetura.md` (camadas de filtro e ordem de verificação), `docs/decisoes.md` (autorização deixa de ser "Adiada"; 404 vs 403; simulador por chave de configuração e sua limitação; negar por omissão), `docs/ambiente.md` (`IOT_API_KEY`), `mobile/README.md` (tratamento de 401/403/404 pelo app) e o JavaDoc de `main/usuario/entity/Perfil.java` (regras agora existem)

---

## Phase 9: Verificação e fechamento

- [x] T024 Varredura de segredos nos arquivos novos e alterados e em `docs/`, `specs/005-perfis-isolamento/` e `.env.example`: usuário/senha do banco, token do GitHub, valor real de `IOT_API_KEY`, token de sessão; confirmar que `.env` não aparece no `git status`; a chave fictícia de teste só pode estar nos testes
- [x] T025 Rodar `.\mvnw.cmd clean verify` completo e registrar a contagem real de testes (executados, verdes, ignorados) e a duração da matriz de autorização (SC-008, alvo < 1 min), comparando com o resultado-base da T001; confirmar que `ArquiteturaTest` e todos os testes da RC-04 seguem verdes
- [x] T026 Percurso manual possível sem rotas de negócio, com `java -jar` e `curl` (quickstart passos 1, 2 e 4): `/auth/sessao` sem token → 401; com token de paciente → 200; com `X-Chave-Simulador` válida → 403; API sobe sem `IOT_API_KEY`; registrar a saída real e o que **não** pôde ser verificado (passos 3 e 5, cobertos só por teste)
- [x] T027 Revisar o diff; marcar tarefas concluídas; atualizar `spec.md` (Status) e `checklists/requirements.md` conforme o estado real; preencher o relatório final RC-05 → FR/SC → arquivos → verificações, com o que ficou **não verificado** (SC-007 com uma pessoa, rotas de negócio futuras, vínculo da chave por paciente, igualdade de tempo entre alheio e inexistente, guarda em tempo de execução contra rota publicada fora do mecanismo); **não** fazer commit

---

## Dependências

- Setup (T001–T002) → Foundational. T003, T004, T005 são paralelos; T006 depende de T004; T007 depende de T006; T008 depende de T007.
- Fixtures (T009, T010) dependem de T005, T007 e T008.
- US1 (T011–T012) depende das fixtures; US2 (T013–T014) depende de US1 (mesmas rotas de exemplo); US3 (T015–T017) depende de T008 e das fixtures; US4 (T018–T020) depende de T003, T008 e T010.
- Docs (T021–T023) dependem de códigos e mensagens fixados (após T017 e T019); T024 depois de tudo escrito; T025 → T026 → T027 por último.

## Cobertura

| Requisito | Tarefas |
|---|---|
| FR-001 a FR-006 (dona, 404 idêntico, listagens, aninhados) | T005, T009–T012 |
| FR-007, FR-008 (administrador, indicadores agregados) | T013, T014 |
| FR-009 a FR-012 (autenticação e perfil no servidor, registro obrigatório com perfil e detecção de omissão no build, ordem, rotas públicas) | T006–T008, T015–T017 |
| FR-013 (perfil lido a cada requisição) | já garantido pela RC-04; testado no T015 (perfil alterado no repositório em memória vale na chamada seguinte) |
| FR-014 a FR-017 (simulador) | T003, T018–T020 |
| FR-018, FR-019 (contrato documentado) | T021 |
| FR-020 (mecanismo único) | T006, T007, T022 |
| FR-021 (log de recusas sem dado sensível) | T006, T020 |
| FR-022 (testes de matriz e omissão) | T011, T013, T016 |
| FR-023, FR-024 (sem CRUD de negócio; sem mudança no banco) | T009–T010 (rotas só em teste); nenhuma tarefa de SQL |
| SC-001 a SC-006, SC-008 | T011, T013, T015, T016, T018, T020, T024, T025 |
| SC-007 (guia em 10 min com uma pessoa) | T022 escreve o guia; **medição com pessoa não é possível aqui → não verificado** |

## Estratégia

MVP: Foundational + fixtures + US1 (paciente isolada) + US2 + US3, que já fecham o núcleo de segurança. Depois US4 (simulador) e US5 (contrato e guia). Fecha com varredura de segredos, build completo e relatório honesto: o que depende de rotas de negócio futuras ou de uma pessoa fica **não verificado**.

---

## Notas de execução (desvios em relação ao texto das tarefas)

- **T003–T006**: os testes unitários de `Erros`, `Propriedade`, `ChaveSimulador` e `Configuracao` ficaram juntos em `test/auth/autorizacao/UnidadesDoMecanismoTest.java`; não há `ErrosTest`, `PropriedadeTest` nem `AutorizacaoFiltroTest` separados. O `AutorizacaoFiltro` é exercitado pelos testes de endpoint (matrizes e ordem de verificação) e o caso "lista de perfis vazia" está em `RotasPrivadasTest`.
- **T005**: `Propriedade.exigir` recebe também a `SessaoAutenticada` (só o id técnico da conta vai ao log, FR-021).
- **T004**: as mensagens fixas foram escritas **sem acentos** (`Voce nao tem permissao para esta operacao.` e `Recurso nao encontrado.`), seguindo o padrão do código existente e mantendo o 404 idêntico ao da rota coringa (`RotaNaoEncontradaController`). O contrato, o `data-model.md` e os documentos de API foram ajustados a esses textos.
- **T007/T016/T017**: além do teste de varredura, `RotasPrivadas.exigirTudoDeclarado()` é chamado ao fim de `Composicao.montar`: a API **não sobe** se houver servlet publicado fora do registro. É guarda de **montagem**, não por requisição (research D3). `OmissaoDeRotaTest` foi incorporado a `RotasPrivadasTest`.
- **T019**: `AutenticacaoService.naoAutenticado()` passou a ser público para o `SimuladorFiltro` devolver o mesmo 401; `AutenticacaoFiltro.tokenDe` também (antes era privado). `AutenticacaoFiltro` agora também registra em log as recusas 401 (sem token, chave nem corpo).
- **T021**: o JSON do Postman foi regravado pelo serializador (pequenas mudanças de formatação além do item novo).
- **T026**: o percurso manual foi feito com o jar real **sem banco** e sem gravar nada no Oracle (a história não toca o banco); os passos que exigem login real não foram repetidos contra o Oracle (ver relatório).
- **Verificação de que os testes pegam falhas**: com a rota de recurso da paciente aberta também ao administrador, `MatrizAdministradorTest` falhou como esperado; o arquivo foi restaurado e o build completo rodou de novo.

---

## Relatório final da RC-05

### Rastreabilidade
RC-05 (ClickBus#11) → FR-001…FR-024 / SC-001…SC-008 (`spec.md`) → T001…T027 → arquivos abaixo → verificações.

### Implementado e verificado (`.\mvnw.cmd clean verify`, sem banco)
- **Base (T001):** 82 testes, 0 falhas, 6 ignorados (testes de Oracle, opt-in). **Final (T025):** **119 testes, 0 falhas, 0 erros, 6 ignorados** (os mesmos 6 de Oracle). 37 testes novos.
- **Mecanismo** (`backend/src/main/java/br/com/regulacheck/auth/autorizacao/`): `RotasPrivadas`, `AutorizacaoFiltro`, `Propriedade`, `Erros`, `ChaveSimulador`, `SimuladorFiltro`; alterados `AutenticacaoFiltro` (chave do simulador → 403; log de 401), `AutenticacaoService` (`naoAutenticado` público), `Composicao` (rotas por `RotasPrivadas`, ponto de extensão e `exigirTudoDeclarado`), `Servidor`, `Configuracao` (`IOT_API_KEY`, `toString` sem segredo), `Perfil` (JavaDoc).
- **Testes** (`backend/src/test/java/br/com/regulacheck/auth/autorizacao/`): `MatrizPacienteTest` (7), `MatrizAdministradorTest` (3), `OrdemDeVerificacaoTest` (8), `RotasPrivadasTest` (5), `SimuladorEndpointTest` (6), `HigieneDeLogAutorizacaoTest` (1), `UnidadesDoMecanismoTest` (7), mais fixtures `RecursosDeExemplo`, `RotasDeExemplo`, `AmbienteDeAutorizacao`.
- **Critérios cobertos por teste:** FR-001…FR-006 e SC-001 (5 tipos × GET/PUT/DELETE, 404 idêntico, listagem, aninhado, ID inválido, dono no corpo/parâmetro, dona desativada); FR-007/FR-008 e SC-002 (403 sem consulta ao repositório, indicadores sem dado identificável); FR-009…FR-013 e SC-003 (401 → 403 → 404 → 400, 405 depois da autenticação, rotas públicas, perfil alterado vale na chamada seguinte); FR-014…FR-017 e SC-005 (chave só na telemetria; contas 403 na telemetria; sem `IOT_API_KEY` → 401 e API sobe); FR-020…FR-022, SC-004 e SC-008 (rota fora do registro derruba a montagem; matriz inteira em ~1 s); SC-006 (log e respostas sem token, chave, senha ou conteúdo).
- **Varredura de segredos (T024):** 244 arquivos; 0 ocorrências do usuário/senha do banco e do token do GitHub; sem padrões de chave; a chave de teste fictícia só existe em `AmbienteDeAutorizacao`; `.env` ignorado pelo Git.
- **Percurso manual (T026), jar real sem banco:** `/health` 200; `/auth/sessao` sem token 401; com chave do simulador válida 403; chave errada 401; `/auth/logout` com chave válida 403; rota inexistente 404; sem `IOT_API_KEY` a API sobe e a chave apresentada dá 401.
- **Documentação:** `CONTRATO_API.md` (perfil por rota), `docs/api/{erros.md, autorizacao.md (novo), README.md, openapi.yaml, RegulaCheck.postman_collection.json}`, `docs/{arquitetura.md, decisoes.md, ambiente.md, modelo-de-dados.md}`, `mobile/README.md`, `.env.example`.

### Não verificado / limites
- **SC-007** (uma pessoa proteger uma rota nova só com o guia em até 10 minutos): o guia existe (`docs/api/autorizacao.md`), mas **não foi medido com uma pessoa**.
- **Rotas de negócio**: não existem; o mecanismo foi provado com rotas de exemplo que vivem só em `src/test`. Cada história futura deve aplicar o mecanismo e repetir a matriz do seu recurso.
- **Consultas reais filtradas pela dona no Oracle** (junção até `RC_SOLICITACAO.PACIENTE_ID`): não existem ainda; só o padrão está documentado e provado com repositório em memória.
- **Login real contra o Oracle com os filtros novos**: não repetido nesta história (o percurso foi sem banco); a RC-04 já havia verificado os endpoints de login/sessão/logout no Oracle e os testes em memória cobrem os filtros novos.
- **Igualdade de tempo entre recurso alheio e inexistente**: garantida só pelo desenho (consulta única), sem teste de cronometragem.
- **Guarda por requisição contra rota publicada fora do registro**: não existe; há falha na montagem e nos testes.
- **Vínculo da chave do simulador a uma paciente/sessão**: fora do escopo (chave única por ambiente; recusa de sessão inexistente ou encerrada).
- **Nenhuma operação no Oracle** foi feita nesta história.

### Fora do escopo (confirmado)
CRUDs de negócio, tela Android, cadastro/recuperação de senha/MFA, força bruta, acesso excepcional do administrador, auditoria completa.
