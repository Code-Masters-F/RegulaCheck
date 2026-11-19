# Feature Specification: App Android — base, login e meus pedidos (RC-18)

**Feature Branch**: `017-app-android-login-pedidos`

**Created**: 17/11/2026

**Status**: Implementada e verificada em 17/11/2026 (PR #71): testes JVM verdes, conferência no emulador e roteiro manual com backend real e Oracle (12 de 12 passos); ver [evidência](../../docs/evidencias/2026-11-17_rc18-app-android.md), [roteiro](../../docs/evidencias/2026-11-17_rc18-roteiro-manual.md) e `tasks.md`

**Input**: User description: "RC-18 (issue #41, Épico 6 #40) — App Android em Kotlin: base do projeto (Gradle, rede, sessão), login e meus pedidos (lista e detalhe). Sem RC-19/RC-20, sem alterar o backend."

**Backlog**: RC-18 · [issue #41](https://github.com/Code-Masters-F/RegulaCheck/issues/41) · Épico 6 [#40](https://github.com/Code-Masters-F/RegulaCheck/issues/40). Os critérios de aceite da #41 e as decisões técnicas da #40 são a fonte da verdade; esta spec não as copia, só as torna verificáveis.

**Referência visual** (não redesenhar): `mobile/design/INDEX.md` (E1.1, E1.2, E1.3), `mobile/design/textos.md` (textos exatos), `mobile/design/design-system.md`, `mobile/design/acessibilidade.md` e `specs/015-fluxo-telas-app/contracts/telas-operacoes.md` (linhas E1.1–E1.3).

**Numeração**: 016 já era `016-iot-simulada`; esta feature usa 017, o número que o épico #40 reservou ao app.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entrar com a conta de paciente (Priority: P1)

A paciente abre o app, vê o aviso acadêmico e entra com o e-mail e a senha da conta fictícia. Se errar a senha, o e-mail fica e a senha é limpa; se o serviço ou a rede falharem, ela vê o motivo e pode tentar de novo.

**Why this priority**: sem sessão nenhuma outra tela do app funciona; é a base de rede e sessão usada pelas demais histórias.

**Independent Test**: com o backend e a carga sintética, entrar com a conta fictícia de paciente chega a "Meus pedidos"; senha errada mostra "E-mail ou senha incorretos." sem sair da tela.

**Acceptance Scenarios**:

1. **Given** a tela de login, **When** a paciente toca em "Entrar" com credenciais válidas de paciente, **Then** o botão fica bloqueado ("Entrando…") até a resposta e a lista de pedidos abre.
2. **Given** credenciais erradas, **When** o servidor recusa (401), **Then** aparece "E-mail ou senha incorretos.", o e-mail é mantido e a senha é limpa.
3. **Given** servidor indisponível (503) ou sem rede/tempo esgotado, **When** a paciente tenta entrar, **Then** aparece a mensagem de serviço indisponível ou sem conexão de E1.1 com "Tentar de novo", e o e-mail é preservado.
4. **Given** uma conta que não é de paciente, **When** o login é aceito pelo servidor, **Then** o app recusa a entrada, encerra essa sessão e não guarda o token.
5. **Given** um token salvo e válido, **When** o app é reaberto, **Then** abre direto em "Meus pedidos".

---

### User Story 2 - Ver meus pedidos (Priority: P1)

A paciente vê só os próprios pedidos, do mais recente ao mais antigo, com serviço, estado (ícone + texto) e última atualização; o mais recente aparece em destaque com o próximo passo. Pode sair a qualquer momento.

**Why this priority**: é a tela de retomada do percurso; sem ela a paciente não encontra o que fazer a seguir.

**Independent Test**: logada como paciente A, a lista mostra exatamente os pedidos que `GET /solicitacoes` devolve para ela; com o backend parado, a tela de erro oferece "Tentar de novo".

**Acceptance Scenarios**:

1. **Given** a sessão aberta, **When** a lista carrega, **Then** aparecem esqueleto e "Carregando seus pedidos…" e depois os pedidos com "Pedido {número} · atualizado em {data e hora}", serviço e estado.
2. **Given** nenhum pedido, **When** a lista carrega, **Then** aparece "Você ainda não tem pedidos." com "Novo pedido".
3. **Given** falha do servidor ou da rede, **When** a lista carrega, **Then** aparece o estado Erro ou Sem conexão de E1.2 com "Tentar de novo".
4. **Given** um estado que o app não conhece, **When** a lista é exibida, **Then** o pedido aparece como "Estado não reconhecido" e o app continua funcionando.
5. **Given** a lista, **When** a paciente toca em "Sair", **Then** o app pede o encerramento ao servidor, apaga o token local mesmo se essa chamada falhar e volta ao login.

---

### User Story 3 - Abrir um pedido e ver o próximo passo (Priority: P2)

Ao tocar num pedido, a paciente vê os dados persistidos (serviço, localidade, documentos anexados, resultado quando houver, datas e revisão) e uma única ação principal conforme o estado.

**Why this priority**: orienta a próxima ação, mas as ações de envio, leitura e análise só funcionam na RC-19.

**Independent Test**: abrir cada pedido da carga sintética mostra os mesmos dados de `GET /solicitacoes/{id}` e o rótulo da ação esperado para o estado; um id inexistente mostra "Pedido não encontrado." com "Voltar para meus pedidos".

**Acceptance Scenarios**:

1. **Given** um pedido em `RASCUNHO` sem documentos, **When** é aberto, **Then** a ação principal é "Anexar documentos"; com documentos, "Ler documentos".
2. **Given** um pedido `PROCESSANDO`, **When** a paciente toca em "Atualizar", **Then** o pedido é consultado de novo.
3. **Given** `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS` ou `FALHA_ANALISE`, **When** é aberto, **Then** a ação é, respectivamente, "Ver pendências", "Ver o que conferir", "Ver resultado" e "Tentar de novo".
4. **Given** `SUBMETIDA_SIMULACAO`, **When** é aberto, **Then** não há ação principal; o pedido só é consultado.
5. **Given** resposta 404, **When** o pedido é aberto, **Then** aparece o estado Não encontrado de E1.3 e o botão volta à lista.

---

### Edge Cases

- **401 em qualquer rota privada**: o token é apagado e o app volta ao login com o aviso de E1.4 ("Sua sessão terminou. Entre novamente.").
- **403**: aparece "Você não tem permissão para ver esta tela." com "Voltar"; a sessão continua.
- **Token ilegível** (chave do Keystore perdida ou dado corrompido): é tratado como sem sessão; o app pede login, sem falhar.
- **Toque repetido em "Entrar" ou "Tentar de novo"**: ignorado enquanto houver envio em andamento.
- **Ação que pertence à RC-19** ("Novo pedido", "Anexar documentos", "Ler documentos", "Ver pendências" etc.): o botão existe com o rótulo do desenho, mas avisa que a etapa ainda não está disponível nesta versão; nada é simulado como feito.
- **Resposta de sucesso com JSON fora do contrato**: tratada como erro de serviço, sem derrubar o app.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O projeto Android MUST ficar em `mobile/app/`, com Wrapper e versões fixadas conforme a #40, e compilar com JDK 21.
- **FR-002**: O app MUST representar toda falha de API num tipo de erro fechado com: sem conexão, tempo esgotado, credenciais inválidas, sessão expirada, sem permissão, não encontrado, conflito (com segundos de `Retry-After` quando houver), arquivo recusado (413/415), requisição inválida (400/422), indisponível (5xx) e resposta inesperada.
- **FR-003**: O app MUST usar os tempos da #40 (conexão 10 s, leitura 30 s; 70 s reservado a ler/analisar) e MUST NOT repetir requisições automaticamente.
- **FR-004**: O app MUST NOT registrar token, senha, cabeçalhos ou corpos em log.
- **FR-005**: O token MUST ser guardado cifrado com chave AES-GCM do Android Keystore em armazenamento privado do app.
- **FR-006**: Um 401 de rota privada MUST apagar a sessão local e levar ao login com o aviso de sessão expirada; um 403 MUST mostrar sem permissão mantendo a sessão.
- **FR-007**: Sair MUST apagar a sessão local mesmo se `POST /auth/logout` falhar.
- **FR-008**: O login MUST bloquear o botão durante o envio, manter o e-mail e limpar a senha em credenciais inválidas, nunca exibir a senha sem pedido explícito ("Mostrar senha") e recusar conta que não seja de paciente.
- **FR-009**: A lista MUST mostrar só o que `GET /solicitacoes` devolve, com estados carregando, vazio, sucesso, erro e sem conexão, estado do pedido em ícone + texto e "Estado não reconhecido" para valor desconhecido.
- **FR-010**: O detalhe MUST mostrar os dados de `GET /solicitacoes/{id}` e uma ação principal por estado (US3); 404 MUST levar ao estado Não encontrado com volta à lista.
- **FR-011**: Toda tela MUST mostrar o aviso de protocolo acadêmico e dados fictícios no rodapé; tocar no rodapé (ou toque longo) MUST mostrar o aviso completo num balão, sem sair da tela. O login MUST mostrar o aviso curto sobre a proposta do app em vez da caixa grande (decisão de 17/11/2026; spec 015, FR-003 e FR-020).
- **FR-012**: Textos MUST seguir `mobile/design/textos.md`; texto vindo da API MUST ser exibido como texto puro.
- **FR-013**: `mobile/README.md` e `docs/ambiente.md` MUST dizer como executar, a URL da API no emulador e no aparelho, o que está implementado e o que é só desenho.

### Key Entities

- **Sessão local**: token, nome e perfil da conta e expiração, guardados cifrados; existe só entre login e logout/401.
- **Pedido (Solicitação)**: id, serviço (especialidade, localidade, aviso fictício), estado, revisão, datas, documentos disponíveis e situação da análise, como no contrato da API.
- **Erro de API**: categoria fechada (FR-002) com código e mensagem do servidor quando houver.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Build e testes automatizados passam num único comando, com os 11 mapeamentos de erro da #41 cobertos (400, 401 login, 401 sessão, 403, 404, 409 com `Retry-After`, 413, 415, 503, sem rede e tempo esgotado).
- **SC-002**: As 3 telas (login, lista, detalhe) e seus estados desenhados conferem com as prévias PNG em textos, ordem e ação principal, verificado por screenshot no emulador.
- **SC-003**: 100% das telas mostram o aviso acadêmico; nenhum estado é transmitido só por cor.
- **SC-004**: Numa execução revisada do logcat, 0 ocorrências de token, senha ou corpo de resposta.
- **SC-005**: Com backend real, a paciente entra e abre um pedido em até 1 minuto (percurso manual registrado por quem executar).

## Assumptions

- O backend e a carga sintética (`database/scripts/02_carga_sintetica.sql`) são usados como estão; a RC-18 não altera o backend.
- A distinção fina do rascunho (a conferir, pronto para verificar, resultado não vigente) depende de rotas de documentos/extrações/análises usadas na RC-19; aqui o rascunho usa só `documentosDisponiveis`.
- `SEM_PENDENCIAS_IDENTIFICADAS` usa "Ver resultado" (a #41 pede "ver resultado"; "Concluir simulação" depende da submissão, fora do épico).
- O aviso de sessão expirada usa o texto de E1.4 em `textos.md` (a #41 o parafraseia).
- A recusa de conta não paciente usa um texto novo, registrado em `textos.md`: "Esta conta não é de paciente. Entre com uma conta de paciente."
- O cabeçalho `GET /auth/sessao` de E1.2 não é chamado: nome e perfil vêm da resposta do login e ficam na sessão local cifrada.
- Fonte ampliada/TalkBack, modo escuro e tablet estão fora do escopo do épico (#40).
