# Research: IoT simulada (RC-21 + RC-22)

Decisões técnicas da feature `specs/015-iot-simulada`. As decisões de produto (quem abre a sessão, faixas, prazo, uma sessão aberta por pedido) estão em [spec.md](spec.md#clarifications); aqui ficam as de implementação.

## D1 — Onde ficam as três rotas

- **Decision**:
  - `POST /solicitacoes/{id}/sessoes-captura` e `GET /solicitacoes/{id}/leituras` entram pelo `SolicitacaoController` já registrado em `RotasPrivadas` com perfil `PACIENTE`, despachando para um novo `CapturaController` (padrão `reconhece(pathInfo)` + `atender`, igual a `AnaliseController`).
  - `POST /sessoes-captura/{id}/leituras` é publicada com `rotas.registrarTelemetria("/sessoes-captura/*", new LeituraSensorController(...))`, que já instala o `SimuladorFiltro` (chave `X-Chave-Simulador`; conta autenticada → 403; sem chave → 401).
- **Rationale**: reaproveita o mecanismo da RC-05 sem tocar filtros; a regra "credencial do simulador só na telemetria" e "conta na telemetria = 403" já está implementada e testada (`RotasPrivadasTest`, `RotasDeExemplo`).
- **Alternatives**: um servlet novo em `/solicitacoes/*/leituras` — impossível, o caminho `/solicitacoes/*` já é de um servlet só; filtro próprio para a chave — duplicaria o `SimuladorFiltro`.

## D2 — Uma sessão aberta por pedido, abertura idempotente

- **Decision**: `abrirSessao` em transação curta: `SELECT … FROM RC_SOLICITACAO WHERE ID = ? AND PACIENTE_ID = ? FOR UPDATE` (mesmo bloqueio usado por envio, troca, exclusão e análise) → procura sessão com `ENCERRADA_EM IS NULL` → devolve a existente (200, `criada: false`) ou insere (201, `criada: true`) → `COMMIT`. Rede de proteção no banco: índice único funcional `RC_SESSAO_CAPTURA_UK_ABERTA ON RC_SESSAO_CAPTURA (CASE WHEN ENCERRADA_EM IS NULL THEN SOLICITACAO_ID END)`; se mesmo assim houver `ORA-00001`, o repositório relê e devolve a aberta.
- **Rationale**: atende "toque duplicado ou pedidos simultâneos não criam outra" (FR-008) com o mesmo padrão já usado nas RC-08/RC-13; o índice protege contra caminhos que não passem pelo bloqueio (carga manual).
- **Alternatives**: só o índice (sem bloqueio) — depende de tratar exceção como fluxo normal; nenhuma proteção — viola o princípio VI (retentativa não duplica).

## D3 — Em que estados do pedido se abre sessão

- **Decision**: qualquer estado, exceto `SUBMETIDA_SIMULACAO` → 409 `CONFLITO_ESTADO` (mesmo código/mensagem já usados para pedido submetido nas RC-08/RC-13). `PROCESSANDO` é permitido: abrir sessão não altera documentos, revisão nem estado.
- **Rationale**: a estação de captura ajuda a fotografar documentos; isso pode acontecer em rascunho e também depois de uma análise com pendências (reenvio). Pedido submetido é só consulta.
- **Alternatives**: só `RASCUNHO` — bloquearia o reenvio da RC-16.

## D4 — Validação da leitura recebida

- **Decision**: corpo JSON (limite de `CorpoJson`, 4 KB; 415 se não for `application/json`) com `sessaoId`, `tipo`, `valor`, `unidade`, `medidaEm`. Ordem: 401/403 (filtro) → 404 sessão inexistente/encerrada/de pedido inexistente (mesma resposta; conferida **antes** de ler o corpo) → 415/400 de formato do corpo → 400 de conteúdo. Regras:
  - `sessaoId` obrigatório e igual ao `{id}` do caminho (o payload identifica a sessão, critério 2 da RC-21);
  - `tipo` ∈ {`LUMINOSIDADE`, `DISTANCIA`}; `unidade` correspondente (`LUX`, `CM`), sem conversão;
  - `valor` numérico finito; limites de **plausibilidade física** (não são a faixa demonstrativa): luminosidade `0 ≤ v ≤ 200000` lux, distância `0 < v ≤ 500` cm; arredondado a 3 casas (precisão de `NUMBER(12,3)`);
  - `medidaEm` ISO-8601 com fuso (`Instant`), não pode estar mais de **30 s** no futuro em relação ao relógio do servidor; sem limite para o passado (leitura antiga vira "desatualizada" na consulta).
  - Campos desconhecidos → 400 (já é o comportamento do `Json` do projeto; conferir no plano de testes).
- **Rationale**: FR-004/FR-005; 404 antes de 400 segue a ordem documentada em `docs/api/erros.md`.
- **Alternatives**: lote de leituras por requisição — mais eficiente, mas o critério pede uma leitura identificável por envio e complica a validação parcial.

## D5 — Ponto central de faixas e classificação

- **Decision**: classe `iot.service.FaixasDemonstrativas` (constantes: 300–1000 lux, 20–40 cm, prazo de atualidade 60 s, tolerância de futuro 30 s, limites físicos) e `iot.service.ClassificadorDeLeitura`, puro (sem HTTP/JDBC), que recebe a última leitura de um tipo e o instante da consulta e devolve `SituacaoSensor` + `Orientacao`.
  - Precedência: sem leitura → `SEM_LEITURA`; `agora − medidaEm > 60 s` → `DESATUALIZADA` (mesmo se o valor estiver fora da faixa); senão `ABAIXO_DA_FAIXA` / `DENTRO_DA_FAIXA` / `ACIMA_DA_FAIXA` com limites inclusivos.
  - Orientação é um código estável + texto fixo em português: `CONDICAO_ADEQUADA`, `MELHORAR_ILUMINACAO`, `REDUZIR_ILUMINACAO_OU_REFLEXO`, `AFASTAR_DOCUMENTO`, `APROXIMAR_DOCUMENTO`, `VERIFICAR_ESTACAO` (desatualizada), `CAPTURA_SEGUE_SEM_SENSOR` (sem leitura).
- **Rationale**: FR-013 a FR-016 e "configuráveis de forma centralizada"; código estável permite à tela do Épico 6 traduzir/ícone sem depender do texto.
- **Alternatives**: variáveis de ambiente para as faixas — mais configurável, mas acrescenta variáveis sem necessidade (CLAUDE.md §13); faixas no banco — exige tabela e carga só para três números.

## D6 — Qual sessão e qual leitura a consulta usa

- **Decision**: sessão mais recente do pedido (`ORDER BY INICIADA_EM DESC, ID DESC FETCH FIRST 1 ROW ONLY`), aberta ou não; para cada tipo, a leitura de maior `MEDIDA_EM` (desempate por `ID`). Consulta sem transação de escrita, filtrada pela dona (`JOIN RC_SOLICITACAO … AND PACIENTE_ID = ?`); pedido alheio/inexistente → 404 idêntico.
- **Rationale**: Edge cases da spec (leituras fora de ordem; várias sessões vindas da carga).

## D7 — Exclusão de rascunho com sessão de captura

- **Decision**: `SolicitacaoRepositoryJdbc.excluirRascunho` passa a apagar `RC_LEITURA_SENSOR` e `RC_SESSAO_CAPTURA` do pedido antes de `RC_DOCUMENTO`, dentro da mesma transação e bloqueio. A gravação de leitura usa o mesmo `FOR UPDATE` do pedido (via junção com a sessão), então nenhuma leitura chega entre o apagar das leituras e o das sessões.
- **Rationale**: hoje a FK `RC_SESSAO_CAPTURA_FK_SOLIC` faria a exclusão cair em `ORA-02292` → "pedido com dependências", ou seja, abrir a sessão bloquearia a exclusão do rascunho — contraria FR-020 ("sensores não alteram o percurso"). A telemetria é dado de apoio, sem valor depois que o rascunho é excluído.
- **Alternatives**: manter a recusa — a paciente não conseguiria excluir rascunho só por ter usado a estação.

## D8 — Simulador

- **Decision**: um único arquivo `simulador-iot/SimuladorSensores.java`, executado pelo *source launcher* do JDK 21 (`java simulador-iot/SimuladorSensores.java …`), sem Maven, sem dependências (JSON montado à mão; só números, enum e instante ISO). `java.net.http.HttpClient` com timeout de conexão e de requisição de 5 s.
  - Argumentos: `--sessao <id>` (obrigatório), `--sequencia normal|fora-da-faixa|recuperacao` (padrão `normal`), `--intervalo-ms <n>` (padrão 1000), `--api <url>` (padrão `API_URL` ou `http://localhost:8080`), `--seco` (só imprime `tipo valor unidade` por leitura, sem instante e sem conexão, para a saída ser reproduzível). Chave lida **somente** de `IOT_API_KEY` (nunca por argumento, para não ficar no histórico do shell).
  - Sequências como tabelas fixas de pares (luminosidade, distância) — reprodutíveis sem semente aleatória. Normal: 5 pares dentro da faixa; fora da faixa: 5 pares com luminosidade baixa e distância grande/pequena; recuperação: 3 pares fora seguidos de 3 dentro.
  - Cada envio: uma leitura por requisição, `medidaEm = Instant.now()`; imprime `tipo valor unidade → status`. Qualquer resposta ≠ 201 ou falha de rede: imprime o código, **não repete**, encerra com código de saída 1. Nunca imprime a chave.
- **Rationale**: CLAUDE.md §12 (script Java, `HttpClient`, sem Spring) e constituição III ("uma aplicação e um módulo Maven" — o simulador não vira segundo módulo). JSON à mão evita classpath.
- **Alternatives**: módulo Maven separado — segundo módulo e build extra; colocar no backend — mistura cliente de demonstração com a API.

## D9 — Testes

- **Decision**:
  - Unitários: `ClassificadorDeLeituraTest` (bordas inclusivas, precedência de desatualizada, sem leitura, futuro dentro da tolerância), validação do `LeituraSensorRequest`.
  - Rota com Jetty + repositórios em memória (`CenarioAuth` ganha `capturas`): abrir sessão (201/200/404/403/409, concorrência com 10 threads → 1 sessão), envio de leitura (matriz 401/403/404/400/415/201 e nada gravado nas recusas), consulta (dois sensores, orientações, desatualizada com `RelogioFixo`, sem sessão, 404 idêntico, admin 403), higiene de log (sem chave nem corpo), exclusão de rascunho com sessão.
  - Simulador: `SimuladorSensoresProcessoTest` executa o arquivo com `ProcessBuilder` usando o `java` da JVM corrente: `--seco` duas vezes → saídas idênticas; envio real contra `ServidorDeTeste` (porta aleatória) → leituras gravadas na memória; chave errada → saída 1 sem repetir. Se o arquivo não for encontrado a partir de `backend/`, o teste falha (não é ignorado).
  - Oracle opt-in: `CapturaOracleTest` (`RC_TESTE_ORACLE=SIM`) e casos novos no `04_testes_restricoes.sql` (índice de sessão aberta). **Não executáveis neste ambiente** sem banco: registrar como NÃO VERIFICADO até alguém do grupo rodar.
- **Rationale**: princípio VII; prioridades: autorização entre contas, concorrência/idempotência.

## D10 — Esquema

- **Decision**: script aditivo `database/scripts/11_iot_sessao_captura.sql` com o índice `RC_SESSAO_CAPTURA_UK_ABERTA`; `01_criar_esquema.sql` ganha o mesmo índice para bancos novos; `04_` ganha casos (segunda sessão aberta recusada; segunda sessão com a primeira encerrada aceita); `README` dos scripts atualizado. Nenhuma coluna nova. A carga sintética (`02_`) já tem uma sessão aberta no pedido 4 — compatível com o índice.
- **Rationale**: tabelas e `CHECK`s de tipo/unidade já existem desde a RC-03.
