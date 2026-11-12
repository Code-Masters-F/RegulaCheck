# Tasks: Login e encerramento de sessão (RC-04)

**Input**: `specs/004-login-sessao/` (spec.md, plan.md, research.md, data-model.md, contracts/api-auth.md, quickstart.md)

**Backlog**: RC-04 — Code-Masters-F/ClickBus#10 · **Branch**: `004-login-sessao`

**Testes**: solicitados pelo plano (D12) e por `CLAUDE.md` §14. Em cada fase, o teste é escrito **antes** da implementação e deve falhar primeiro. Testes sem banco rodam no `.\mvnw.cmd clean verify`; os de Oracle são opt-in e, se não rodarem, ficam registrados como **não verificados**.

**Regras desta execução**: nada de commit/push sem pedido; credenciais do banco só em variáveis de ambiente carregadas do `.env` (ignorado pelo Git) sem imprimi-las; o `05_` só cria uma tabela `RC_*`; o `90_` é **editado mas não executado**; `PEQUENOVAREJO`, `VENDA` e as 12 tabelas da RC-03 não são alteradas; senha, hash completo e token nunca vão para log, resposta ou arquivo versionado (exceto a senha de demonstração fictícia já documentada).

## Formato: `[ID] [P?] [Story] Descrição com caminho`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência pendente)
- **[US#]**: história da spec (US1 entrar, US2 senha segura, US3 usar e encerrar sessão, US4 comportamento do cliente e contrato)
- Caminhos de Java abreviados: `main` = `backend/src/main/java/br/com/regulacheck`, `test` = `backend/src/test/java/br/com/regulacheck`.

---

## Phase 1: Setup

- [x] T001 Rodar `.\mvnw.cmd clean verify` em `backend/` antes de qualquer alteração e registrar o resultado-base (testes existentes verdes); conferir que `.env` está no `.gitignore` e que `git status` está limpo na branch `004-login-sessao`
- [x] T002 Adicionar em `backend/pom.xml`, com versões fixas em `<properties>`, as dependências `at.favre.lib:bcrypt` 0.10.2, `com.oracle.database.jdbc:ojdbc11` 23.26.4.0.0 e `com.zaxxer:HikariCP` 6.3.0; rodar `.\mvnw.cmd dependency:resolve`, corrigir a versão se alguma não resolver (registrando a mudança em `research.md` D2) e confirmar que `ArquiteturaTest` segue verde e que `java -jar target\regulacheck-backend.jar` ainda sobe e responde `GET /health` 200 (o *shade* não pode quebrar com o driver)
- [x] T003 [P] Atualizar `.env.example` com `SESSION_TTL_MINUTES` (padrão 30, opcional) e a nota de que `RC_TESTE_ORACLE=SIM` liga os testes de Oracle; nenhum valor real

---

## Phase 2: Foundational (bloqueia as histórias)

- [x] T004 Escrever `database/scripts/05_sessao_login.sql` com cabeçalho (finalidade, ordem, efeito, pré-requisito: scripts 01 e 02) criando `RC_SESSAO_LOGIN` conforme `data-model.md` (identity, PK, FK para `RC_USUARIO`, `TOKEN_HASH CHAR(64)` único com CHECK hexadecimal, CHECK de prazo) e o índice `RC_SESSAO_LOGIN_IX_USUARIO`; nomes ≤ 30 caracteres; sem `DROP`, `TRUNCATE` ou `DELETE`
- [x] T005 [P] Editar `database/scripts/90_limpeza_DESTRUTIVA.sql` para listar `RC_SESSAO_LOGIN` como primeira tabela a remover (ordem inversa de dependência), mantendo a guarda `'NAO'`; **não executar** o script
- [x] T006 Executar o `05_` no Oracle com o executor temporário do scratchpad e conferir: estado antes (12 tabelas `RC_*`, 77 linhas, `PEQUENOVAREJO` e `VENDA` com 0 linhas), tabela, restrições e índice criados, estado depois sem outras mudanças, e a repetição do `05_` falhando com ORA-00955 sem alterar nada; registrar a saída real
- [x] T007 [P] `main/config/Configuracao.java`: acrescentar `banco` (URL, usuário, senha de `DB_*`; todos opcionais) e `ttlSessao` (`SESSION_TTL_MINUTES`, padrão 30, inteiro de 1 a 1440, inválido → `IllegalArgumentException` com mensagem clara); manter o construtor `(host, porta)` usado pelos testes atuais; sobrescrever `toString` para ocultar a senha; teste `test/config/ConfiguracaoTest.java` escrito antes (padrões, TTL inválido, senha ausente do `toString`)
- [x] T008 [P] `main/config/BancoDados.java`: `DataSource` preguiçoso com Hikari (`maximumPoolSize=3`, `connectionTimeout=5000`, `initializationFailTimeout=-1`); sem `DB_*` ou com falha de conexão lançar `ExcecaoNegocio` 503 `DEPENDENCIA_INDISPONIVEL` com mensagem genérica; nunca registrar URL com credencial, SQL ou senha; teste `test/config/BancoDadosTest.java` escrito antes (sem configuração → 503; não tenta conectar ao ser criado; **configurado para uma porta fechada em `localhost` com tempo de conexão curto** → 503 e nenhuma sessão aberta, sem expor mensagem do driver)
- [x] T009 `main/common/Json.java` e `main/common/http/ServletJson.java`: ler o corpo JSON com limite de 4 KB, exigir `Content-Type: application/json` (senão 415 `TIPO_NAO_SUPORTADO`), JSON ilegível ou corpo vazio → 400 `REQUISICAO_INVALIDA` com `detalhes`; teste `test/common/http/CorpoJsonTest.java` escrito antes
- [x] T010 [P] `main/usuario/entity/Usuario.java`, `main/usuario/repository/UsuarioRepository.java` (interface: buscar por e-mail e por id) e `UsuarioRepositoryJdbc.java` com SQL parametrizado e `try-with-resources`; falha de SQL vira 503 genérico sem expor mensagem do driver
- [x] T011 [P] `main/auth/entity/Sessao.java`, `main/auth/repository/SessaoRepository.java` (interface: criar, buscar por hash do token, encerrar por id, remover por hash apenas para limpeza de teste) e `SessaoRepositoryJdbc.java`, parametrizado e UTC
- [x] T012 [P] Repositórios em memória para testes em `test/auth/` (`UsuarioRepositoryMemoria`, `SessaoRepositoryMemoria`) e um `RelogioFixo` (`java.time.Clock` ajustável); serão injetados nos testes com Jetty pelo ponto de entrada criado na T020

**Checkpoint**: tabela criada no Oracle, configuração, banco preguiçoso, leitura de JSON e repositórios prontos.

---

## Phase 3: US2 — Senha protegida e verificada com segurança (P1)

**Meta**: BCrypt verifica as senhas e os hashes da RC-03.

- [x] T013 [US2] Escrever `test/auth/service/VerificadorSenhaTest.java`: senha correta, errada, vazia, com acentos, hash em formato inválido (não é aceito e não lança), senha acima de 72 bytes (recusada sem exceção) e senha nunca presente em mensagens de exceção
- [x] T014 [US2] Escrever `test/auth/HashesRc03CompatibilidadeTest.java`: lê os 3 hashes `$2a$12$…` de `database/scripts/02_carga_sintetica.sql` (caminho `../database/scripts/02_carga_sintetica.sql`, pois o Maven roda a partir de `backend/`, como o `ArquiteturaTest`; o script tem exatamente 3 ocorrências do prefixo), confirma custo 12 e verifica a senha de demonstração fictícia (constante de teste, a mesma do README dos scripts) em cada um; uma senha errada falha em todos (SC-001 sem banco; fecha o "não verificado" da RC-03)
- [x] T015 [US2] Implementar `main/auth/service/VerificadorSenha.java` com `BCrypt.verifyer()` (nunca hash caseiro), recusando antes da biblioteca senha > 72 bytes; incluir o hash BCrypt custo 12 "falso" fixo para equalizar tempo (gerado com a mesma biblioteca de um valor aleatório descartado) e JavaDoc explicando o motivo; fazer T013 e T014 passarem

---

## Phase 4: US1 — Entrar com a conta de demonstração (P1)

**Meta**: `POST /auth/login` abre a sessão ou devolve o erro genérico.

- [x] T016 [US1] Escrever `test/auth/service/AutenticacaoServiceLoginTest.java` (repositórios em memória, `RelogioFixo`): sucesso (sessão gravada **antes** de devolver o token, `expiraEm` = agora + TTL), senha errada, e-mail inexistente, conta desativada e senha longa com o mesmo resultado de falha, e-mail com maiúsculas/espaços aceito, falha do repositório não abre sessão, e verificação de senha executada também para e-mail inexistente
- [x] T017 [P] [US1] Escrever `test/auth/service/GeradorTokenTest.java` e implementar `main/auth/service/GeradorToken.java`: 32 bytes de `SecureRandom`, Base64 URL sem preenchimento (43 caracteres), tokens distintos, SHA-256 hexadecimal estável de 64 caracteres
- [x] T018 [P] [US1] `main/auth/dto/LoginRequest.java` e `LoginResponse.java` (token, tipo `Bearer`, `expiraEm`, usuário com nome e perfil), sem hash nem id interno; validação de tamanho (e-mail até 254) e de campos obrigatórios com `detalhes` por campo
- [x] T019 [US1] Implementar `AutenticacaoService.login` em `main/auth/service/AutenticacaoService.java` (JavaDoc: efeitos, exceções, equalização de tempo) usando `Clock`, `GeradorToken`, `VerificadorSenha` e os repositórios; falha de credencial → 401 `CREDENCIAIS_INVALIDAS` única; registrar em log apenas resultado, código, `usuarioId` e duração; fazer T016 passar
- [x] T020 [US1] Implementar `main/auth/controller/LoginController.java` (apenas `POST`) e registrar `/auth/login` em `main/config/Composicao.java` (a rota coringa continua por último). Criar o **ponto de entrada para testes**: record `main/config/Dependencias.java` (`UsuarioRepository`, `SessaoRepository`, `Clock`), `Composicao.montar(Configuracao, Dependencias)` e `Servidor.criar(Configuracao, Dependencias)`; o `Servidor.criar(Configuracao)` atual continua existindo e monta as dependências reais (`BancoDados` preguiçoso + repositórios JDBC + relógio do sistema), de modo que `SaudeEndpointTest` não muda
- [x] T021 [US1] Escrever e passar `test/auth/AuthEndpointLoginTest.java` com Jetty real em porta livre e repositórios em memória injetados por `Servidor.criar(Configuracao, Dependencias)`: 200 com o formato do contrato, os quatro casos de falha com corpo **idêntico**, 400 com campos, 415, 405 e 503 sem banco configurado (`/health` segue 200); dois logins seguidos com as mesmas credenciais geram duas sessões independentes (História 4, cenário 2)

---

## Phase 5: US3 — Usar a sessão e encerrá-la (P1)

**Meta**: filtro reutilizável, consulta da sessão atual e logout.

- [x] T022 [US3] Escrever `test/auth/service/AutenticacaoServiceSessaoTest.java` e `test/auth/AuthEndpointSessaoTest.java`: sessão válida devolve identidade e expiração; logout invalida o token na hora; reuso do token após logout, token ausente, malformado, desconhecido e expirado (avançando o `RelogioFixo`) dão o mesmo 401 `NAO_AUTENTICADO`; conta desativada depois do login perde a sessão; duas sessões da mesma conta são independentes (logout de uma não afeta a outra); logout repetido dá 401; corpo da requisição nunca define a identidade
- [x] T023 [US3] Implementar `main/auth/controller/SessaoAutenticada.java` e, em `AutenticacaoService`, `autenticar(token)` (hash do token → sessão aberta, não expirada, conta ativa) e `encerrar(sessao)`; token comparado só pelo hash
- [x] T024 [US3] Implementar `main/auth/controller/AutenticacaoFiltro.java` (Filter Jakarta): lê `Authorization: Bearer`, grava `SessaoAutenticada` como atributo da requisição ou responde 401 padrão sem diferenciar o motivo; JavaDoc explicando como as próximas histórias o registram em suas rotas
- [x] T025 [US3] Implementar `SessaoController` (`GET /auth/sessao`) e `LogoutController` (`POST /auth/logout`, 204), ambos lendo a identidade do atributo da requisição, e registrar rotas e filtro em `Composicao`; fazer T022 passar
- [x] T026 [US3] Escrever e passar `test/auth/HigieneDeLogTest.java`: antes de iniciar o servidor, definir a propriedade de sistema `org.slf4j.simpleLogger.logFile` para um arquivo temporário do teste (o `slf4j-simple` fixa o destino quando o logger inicia, então o teste roda em classe própria, sem outro teste que inicialize o logger antes); durante login com sucesso, falhas, sessão e logout, ler esse arquivo e as respostas e confirmar que não contêm senha, hash, token completo nem e-mail enviado (FR-011, SC-004)

---

## Phase 6: US4 — Contrato e comportamento do cliente (P2)

**Meta**: documentação coerente com o que foi implementado.

- [x] T027 [US4] Atualizar `CONTRATO_API.md` (acrescentar `GET /auth/sessao` e `POST /auth/logout`, marcar as três rotas como implementadas), `docs/api/erros.md` (código `CREDENCIAIS_INVALIDAS` e uso de `NAO_AUTENTICADO`), `docs/api/openapi.yaml` (as três rotas, esquemas e respostas), `docs/api/RegulaCheck.postman_collection.json` (login, sessão, logout com variável de token) e a seção *Situação das rotas* de `docs/api/README.md`
- [x] T028 [P] [US4] Atualizar `mobile/README.md` com o comportamento esperado da tela de login (as seis regras de `contracts/api-auth.md`), deixando explícito que a tela e o armazenamento seguro do token serão implementados na história que criar o app
- [x] T029 [P] [US4] Atualizar `database/scripts/README.md` (ordem e efeito do `05_`, exigência dos scripts 01 e 02, `90_` agora removendo também `RC_SESSAO_LOGIN`), `docs/modelo-de-dados.md` (tabela `RC_SESSAO_LOGIN`, diagrama e dicionário, com nota explícita distinguindo `RC_SESSAO_LOGIN` — autenticação da paciente — de `RC_SESSAO_CAPTURA` — leituras dos sensores) e `database/README.md` (registro de verificação do `05_`)
- [x] T030 [P] [US4] Atualizar `docs/arquitetura.md` (fluxo de autenticação e o `AutenticacaoFiltro`), `docs/decisoes.md` (BCrypt/token/pool/prazo/limites) e `docs/ambiente.md` (variáveis `DB_*` e `SESSION_TTL_MINUTES`, comportamento 503 sem banco, como rodar os testes de Oracle)

---

## Phase 7: Verificação com o banco real e fechamento

- [x] T031 Escrever `test/auth/AuthOracleTest.java` (nome terminado em `Test` para o *surefire* já configurado executá-lo; sem plugin novo; `assumeTrue` em `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `RC_TESTE_ORACLE=SIM`, senão é ignorado): as 3 contas da carga entram, senha errada falha, sessão consultada, logout, reuso 401 e expiração com TTL curto; **simular reinício**: depois de abrir uma sessão e encerrar outra, montar um serviço novo sobre o mesmo banco e confirmar que o token encerrado segue recusado e o aberto segue válido; ao final remove **apenas** as sessões criadas pelo teste (por hash do token); conferir que a contagem das 12 tabelas da RC-03 não mudou
- [x] T032 Carregar as variáveis de `DB_*` do `.env` no processo do Maven sem imprimi-las (script PowerShell no scratchpad, fora do repositório, que lê o `.env` e define `$env:` apenas na sessão do comando, sem `echo` dos valores) e rodar `.\mvnw.cmd verify` com `RC_TESTE_ORACLE=SIM`; registrar a saída real (testes executados, verdes, ignorados) e conferir depois, no Oracle, que `RC_SESSAO_LOGIN` ficou sem linhas de teste e que `PEQUENOVAREJO`/`VENDA` seguem com 0 linhas
- [x] T033 Percurso manual do `quickstart.md` com `java -jar` e `curl`: login das 3 contas, `sessao`, `logout`, reuso, senha errada e e-mail inexistente com resposta idêntica, API sem `DB_*` (login 503 e `/health` 200); medir o tempo do login (alvo ≤ 1 s); cronometrar, seguindo **somente** o `quickstart.md`/README, entrar, consultar a sessão e sair (SC-005, alvo < 5 min); registrar a saída real. A igualdade de tempo entre e-mail existente e inexistente fica garantida por teste de que a senha é verificada nos dois casos (T016), sem teste de cronometragem, que seria instável
- [x] T034 Varredura de segredos nos arquivos alterados e novos desta história e em `docs/`, `specs/004-login-sessao/`, `database/` e `.env.example`: usuário e senha do banco, token do GitHub, hash completo em log/resposta e senha de demonstração fora do README dos scripts e das constantes de teste; confirmar que `.env` não aparece no `git status`
- [x] T035 Rodar `.\mvnw.cmd clean verify` completo e registrar contagem de testes; revisar o diff, marcar as tarefas concluídas, atualizar `checklists/requirements.md` conforme o estado real (inclusive o que ficou **não verificado**) e preencher o relatório final RC-04 → FR/SC → arquivos → verificações; **não** fazer commit

---

## Dependências

- Setup (T001–T003) → Foundational. T002 antes de qualquer classe que use as bibliotecas.
- T004 → T006 (executar depois de escrito); T005 independente; T006 antes de T031/T032.
- T007, T008, T009 precedem T010–T012 e todos os controllers; T010–T012 são paralelos entre si.
- US2 (T013–T015) vem **antes** de US1 porque o login precisa do verificador, apesar de ambas serem P1.
- US1: testes (T016, T017) antes de T019/T020; T019 antes de T020; T021 depois de T020.
- US3 depende de T019 (sessão existe), T011 e T012; T023 → T024 → T025.
- US4 (T027–T030) pode andar em paralelo com US3 depois que as rotas e códigos estiverem fixos; precisa estar fechada antes de T034.
- T031 depende de US1 e US3; T032–T033 dependem de T006 e T031; T034 e T035 por último.

## Cobertura

| Requisito | Tarefas |
|---|---|
| FR-001 a FR-003 (login, erro genérico, validação) | T009, T016, T018–T021 |
| FR-004, FR-005 (BCrypt, contas de demonstração) | T013–T015, T031–T033 |
| FR-006 a FR-008 (prazo, sessão atual, logout, 401) | T004, T022–T025 |
| FR-009 (identidade só pela sessão) | T022, T024, T025 |
| FR-010 (token imprevisível, só o hash guardado) | T004, T011, T017, T019 |
| FR-011 (sem segredo em log/resposta) | T008, T019, T026, T034 |
| FR-012 (sessões independentes) | T021, T022 |
| FR-013 (contrato, OpenAPI, Postman, app) | T027, T028 |
| FR-014 (filtro reutilizável, sem perfis) | T024, T030 |
| FR-015 (limites do projeto) | T034, T035 |
| SC-001 a SC-006 | T014, T031–T033 (SC-001), T021 (SC-002), T022 (SC-003), T026/T034 (SC-004), T033 (SC-005), T022 (SC-006) |

## Estratégia

MVP: Setup + Foundational + US2 + US1 (entra com as 3 contas). Em seguida US3 (sessão e logout) e US4 (contrato). A verificação com o Oracle real fecha a história; qualquer teste de banco que não rodar é registrado como **não verificado**, nunca como aprovado.

---

## Notas de execução (desvios em relação ao texto das tarefas)

- **T009**: a leitura limitada do corpo ficou em uma classe própria, `main/common/http/CorpoJson.java`, com `Json.ler` em `main/common/Json.java`; `ServletJson` ganhou só `responderJson`. `ExcecaoNegocio` passou a aceitar `detalhes` por campo (usado pelo 400).
- **T011**: a operação "remover por hash" **não** entrou em `SessaoRepository` (seria código de produção usado só por teste). A limpeza do teste de Oracle (T031) usa SQL próprio no teste.
- **T023**: `SessaoAutenticada` ficou em `main/auth/service/` (e não em `controller/`) para o serviço não depender da camada HTTP.
- **T026**: a captura de log troca `System.err` por um buffer durante o teste, porque o `slf4j-simple` sem cache escreve em `System.err` a cada chamada; o controle positivo do teste prova que a captura funcionou. Não foi preciso a propriedade `logFile`.
- **T006**: `05_` executado no Oracle em 12/11/2026: 1ª execução 2 instruções OK; 2ª execução ORA-00955 nas duas, sem alterar nada; depois, 13 tabelas `RC_*`, 77 linhas nas 12 da RC-03, `RC_SESSAO_LOGIN` com 0 linhas, `PEQUENOVAREJO` e `VENDA` com 0 linhas, maior nome de restrição com 26 caracteres.

---

## Relatório final da RC-04 (12/11/2026)

**Implementado e verificado**

| Requisito / critério | Evidência (resultado real) |
|---|---|
| FR-001–003 login, erro genérico, validação | `AutenticacaoServiceLoginTest` (9), `AuthEndpointLoginTest` (10, Jetty real) e `CorpoJsonTest` (7): 200 no formato do contrato; 4 falhas de credencial com corpo idêntico; 400 com detalhes por campo; 415; 405; 503 sem banco |
| FR-004–005 BCrypt e contas de demonstração | `VerificadorSenhaTest` (8) e `HashesRc03CompatibilidadeTest` (3: os 3 hashes `$2a$12$` de `02_carga_sintetica.sql` conferem com a senha de demonstração); login real das 3 contas no Oracle (`AuthOracleTest` e percurso manual) |
| FR-006–008 prazo, sessão atual, logout, 401 | `AutenticacaoServiceSessaoTest` (7), `AuthEndpointSessaoTest` (11) e Oracle: expiração no minuto 30, logout invalida na hora, mesmo corpo 401 em 7 formas de sessão inválida |
| FR-009 identidade só pela sessão | teste em que o corpo aponta a conta B e só a sessão do token é encerrada |
| FR-010 token imprevisível, só o hash guardado | `GeradorTokenTest` (6); teste de Oracle confirma que o token em claro não é o valor gravado |
| FR-011 sem segredo em log/resposta | `HigieneDeLogTest` (log capturado + respostas, com controle positivo); varredura de segredos: 0 ocorrências de usuário/senha do banco e do token do GitHub nos 219 arquivos versionáveis |
| FR-012 sessões independentes | testes de servlet e de Oracle (logout de uma sessão não afeta outra da mesma conta) |
| FR-013–014 contrato e filtro reutilizável | `CONTRATO_API.md`, `docs/api/*` (erros, OpenAPI, Postman), `mobile/README.md`; `AutenticacaoFiltro` documentado em `docs/arquitetura.md` |
| Banco | `05_sessao_login.sql` executado no Oracle 19c (2 instruções OK; repetição dá ORA-00955); 13 tabelas `RC_*`; 77 linhas nas 12 da RC-03; `PEQUENOVAREJO` e `VENDA` com 0 linhas |
| Build | `mvnw clean verify` sem Oracle: 82 testes, 0 falhas, 6 ignorados (`AuthOracleTest`, opt-in); com Oracle (`RC_TESTE_ORACLE=SIM`): 82 testes, 0 falhas, 0 ignorados; jar de 15 MB sobe e responde `/health` |

**Não verificado**

- Tempo do login: alvo do plano de ≤ 1 s. O primeiro login do processo levou cerca de 1,6 s (inclui abrir a conexão com o Oracle) e os seguintes cerca de 0,3 s; o alvo foi medido, mas **não atingido no primeiro acesso**.
- SC-005 (uma pessoa seguir só a documentação em menos de 5 minutos): não medido com uma pessoa; o percurso foi executado por script.
- Execução do `05_` no SQL Developer/SQLcl (o grupo deve repetir a ordem `01`→`02`→`05` uma vez).
- Tela Android (decisão do responsável): fora desta história; o comportamento está só documentado.

**Fora do escopo declarado (limitações conhecidas)**: proteção contra força bruta, recuperação de senha, MFA, renovação e purga de sessões, perfis e isolamento por paciente (RC-05).

**Desvios do plano**: ver *Notas de execução* acima (remoção por hash não entrou no repositório; `SessaoAutenticada` em `auth/service`; classe `CorpoJson`; captura de log via `System.err`).

**Sem commit**: nenhuma alteração foi commitada ou enviada ao GitHub.
