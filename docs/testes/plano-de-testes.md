# Plano de testes do RegulaCheck (RC-24)

Cenários T01 a T16 da **issue #50** (fonte primária; [CONTRATO_TESTES.md](../../CONTRATO_TESTES.md) é só o resumo dos mesmos cenários), desdobrados em casos reproduzíveis `Txx-yy`, mais o percurso integrado `P01`. Este arquivo é o plano da issue #50. **A execução, as evidências e os retestes são da RC-25 (#51).**

## 1. Aviso e escopo

- Todo o material é **fictício e sintético**: contas `.invalid`, documentos de `samples/`, protocolo acadêmico fictício. Nenhum dado real entra aqui.
- **Leitura dos documentos (extração/OCR) e resumo de pendências são SIMULADOS** (decisão de 12/11/2026). O leitor reconhece o arquivo pelo SHA-256 e devolve dados predefinidos; o resumo vem de um adaptador simulado. Nenhum caso deste plano prova leitura real nem IA real. A submissão e os sensores também são simulados.
- "Sem pendências identificadas" **não é aprovação do SUS**, vaga, prazo nem autorização de atendimento.
- Caso que usa **dublê** (provedor de IA indisponível, saída fora do contrato, falha do leitor) vem marcado `Dublê (não é integração real)` e não comprova integração real.
- Este plano **não executa nada**. Todo caso está "Não executado" e com resultado obtido, evidência, responsável e data em branco.

## 2. Status do plano

| Item | Situação |
|---|---|
| Data de criação | 18/11/2026 |
| Commit do plano | ___ (preencher quando o plano for commitado) |
| Versão testável (commit da API e do app) | ___ (preencher na execução, RC-25) |
| Revisão por outro integrante | **PENDENTE** (nenhuma pessoa além de quem escreveu leu o plano; só deixa de ser pendente quando ela registrar nome e data aqui) |
| Execução dos casos | Não iniciada (RC-25) |

## 3. Decisões aprovadas × funcionalidades pendentes × diferenças da issue

### 3.1 Decisões de escopo aprovadas (não são falhas)

| Decisão | Efeito nos casos |
|---|---|
| Leitura/extração **simulada** (12/11/2026, CLAUDE.md §4 e §11) | Casos de leitura dizem "simulada"; esperado = `gabarito.md`; arquivo desconhecido gera `LEITURA_VAZIA` |
| Resumo de pendências **simulado** (12/11/2026) | Casos de resumo dizem "simulado"; falha de IA só com dublê |
| Submissão **simulada** | Comprovante com "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" |
| Sensores por **script** (simulador Java) | Leituras vêm de `simulador-iot/SimuladorSensores.java` |
| Protocolo acadêmico fictício `PROTOCOLO-ACADEMICO-FICTICIO-v0.1` | Existe **um** serviço ativo |

### 3.2 Funcionalidades ainda pendentes (viram "Impedimento conhecido")

| Pendência | Efeito |
|---|---|
| RC-20 — indicadores do administrador (**P1**, issue #43: "só se sobrar tempo"). Não existe rota `/admin/indicadores` nem outra rota administrativa; só `/protocolos` e as de paciente | T03-01 e T03-02 |
| RC-22 — alertas dos sensores **na interface do app** (o app não consome `GET /solicitacoes/{id}/leituras`) | T16-06 |
| Resposta do app às leituras (RC-22) e demais P1 (RC-33 regressão automática, RC-34 download do comprovante, RC-35) | fora do plano |

Os PRs do app RC-18 (#71) e RC-19 (#74) **já foram mergeados** (merge commits `4e7a3f6` e `8ea60f8` na história da branch); o app está no repositório e os casos de app **não** têm impedimento por PR aberto.

### 3.3 Diferenças entre a issue #50 e o contrato real

| Tema | Issue diz | Contrato real (usado no plano) |
|---|---|---|
| T02 | "todas as operações são negadas conforme o contrato de autorização" | Recurso de outra paciente responde o **mesmo 404 `NAO_ENCONTRADO`** de recurso inexistente (`docs/api/erros.md`, `autorizacao.md`) |
| T03 | paciente recebe HTTP 403 (o caminho `/admin/indicadores` vem do CONTRATO_TESTES.md; a issue não fixa o caminho) | A rota **não existe** (RC-20). O 403 só vale quando existir; até lá a resposta seria 404 |
| T03 | administrador recebe agregados | Sem rota de agregados. Hoje o administrador recebe **403** em qualquer recurso individual de paciente (testável) |
| T04 | "campo permitido" | O único campo editável é o **serviço** (`protocoloId`); campo obrigatório ausente/vazio → 400 `REQUISICAO_INVALIDA`; serviço inexistente → 422 `REGRA_DE_NEGOCIO` |
| T05 | "extração real" | Extração **simulada** |
| T06 | PDF protegido | **HTTP 400 `REQUISICAO_INVALIDA`** (detalhe `arquivo`); arquivo acima de 5 MB → **413** `ARQUIVO_MUITO_GRANDE`; formato inválido → 415 |
| T12 | "IA indisponível" | Não há provedor real; só adaptador simulado e dublê de teste |
| T16 | alertas | Backend (leitura, faixas, desatualização, chave) testável; **alerta na interface** é RC-22 |
| Percurso integrado | "resumo real por IA" | Resumo **simulado** |

## 4. Status e regras de uso

| Status | Quando usar |
|---|---|
| **Não executado** | Estado inicial de todo caso. Também vale quando existe impedimento conhecido que ninguém tentou executar |
| **Aprovado** | Executado e **todos** os itens do "Resultado esperado" do caso conferem, com evidência. Testar só parte do comportamento **não** aprova o caso |
| **Reprovado** | Executado e algum item difere do esperado. Registrar como achado; **não** editar o esperado para coincidir |
| **Bloqueado** | Alguém **tentou** executar e um impedimento (ambiente, dependência, funcionalidade pendente) foi identificado e registrado com o motivo |

Regras: (1) nenhum caso é aprovado sem execução; (2) "Impedimento conhecido" é anotação, não status; só a RC-25 muda para "Bloqueado", depois de tentar e registrar; (3) um caso com várias verificações só aprova se todas conferirem; casos parcialmente verificáveis foram divididos; (4) divergência = achado com evidência; (5) caso executado por dublê é registrado como tal.

## 5. Ambiente e preparação

### 5.1 O que precisa estar de pé

| Peça | Como | Obrigatório para |
|---|---|---|
| JDK 21+ e Maven Wrapper | `java -version`; a partir de `backend/`: `.\mvnw.cmd -version` | tudo |
| API | a partir de `backend/`: `.\mvnw.cmd clean verify`, depois `java -jar target\regulacheck-backend.jar`; conferir `curl http://localhost:8080/health` → 200 `{"status":"UP"}` | casos de API |
| Oracle (conta do grupo) | exportar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (pedir ao grupo; **nunca** copiar para o repositório ou evidência); esquema e carga: `database/scripts/` na ordem do README | casos "API real + Oracle" |
| `DOCUMENT_STORAGE_PATH` | pasta **fora** do repositório; sem ela, enviar/baixar documento responde 503 | upload, download, leitura |
| `IOT_API_KEY` | mesma chave na API e no simulador; sem ela a telemetria responde 401 | T16 |
| `SESSION_TTL_MINUTES` | 1 a 1440 (padrão 30); usar `1` em T01-05/T15-03/T15-07 | sessão expirada |
| Emulador Android e app | `mobile/README.md`; URL `http://10.0.2.2:8080`; `docs/ambiente.md` (JDK 21 do Android Studio só na sessão) | casos "App no emulador" |
| Simulador | a partir da raiz: `java simulador-iot/SimuladorSensores.java --sessao N --sequencia normal|fora-da-faixa|recuperacao [--seco]` | T16 |

Sem `DB_*`, `/health` responde, mas `/auth/*`, `/protocolos` e `/solicitacoes` respondem 503 `DEPENDENCIA_INDISPONIVEL`.

Se o contorno do `JAVA_TOOL_OPTIONS` descrito em `docs/ambiente.md` ("Problemas comuns") for necessário, registre-o na evidência.

### 5.2 Contas fictícias

| Conta | Perfil | Uso |
|---|---|---|
| `paciente.a@exemplo.invalid` | PACIENTE | paciente "A" (na carga: pedidos 1, 2 e 4) |
| `paciente.b@exemplo.invalid` | PACIENTE | paciente "B" (na carga: pedido 3) |
| `admin@exemplo.invalid` | ADMINISTRADOR | administrador |

A senha de demonstração está em [database/scripts/README.md](../../database/scripts/README.md) (seção "Contas e senhas de demonstração"). **Não a copie** para evidências. Pedidos da carga não têm arquivo guardado (download responde 503); para isso, crie o pedido pelo procedimento abaixo.

### 5.3 Material de entrada

- Documentos e gabaritos: [samples/](../../samples/README.md); catálogo: [samples/catalogo-conjuntos.md](../../samples/catalogo-conjuntos.md); conjuntos `samples/conjuntos/<NN>-<nome>/` com `gabarito.md` (escrito antes da execução).
- O conjunto 01 usa as duas amostras oficiais da raiz: `samples/encaminhamento-sintetico.pdf` e `samples/identificacao-sintetica.png`. Os demais usam os arquivos da própria pasta. **Não regenere** arquivo de amostra (o SHA-256 é a chave da leitura simulada).
- **Data de referência:** `2026-11-12` vale só na regressão automática (`ConjuntosDemonstracaoTest`). Em execução manual vale o dia do envio. A fronteira dos 18 anos do conjunto 10 só é exercitada na regressão.
- **Tipo documental** no envio: `encaminhamento*.pdf` → `ENCAMINHAMENTO`; `identificacao*.png` → `IDENTIFICACAO`; `autorizacao.pdf` → `AUTORIZACAO_RESPONSAVEL`. Se o gabarito indicar outro, vale o gabarito.

### 5.4 Procedimentos-base (usados pelos casos)

Defina `API=http://localhost:8080`. Corpo e cabeçalhos seguem [docs/api/contratos-dados.md](../api/contratos-dados.md).

| Sigla | Procedimento |
|---|---|
| **PB-1 Entrar** | `curl -s -X POST $API/auth/login -H "Content-Type: application/json" -d '{"email":"<conta>","senha":"<senha de demonstração>"}'` → guardar o `token` (aparece só uma vez). Demais chamadas: `-H "Authorization: Bearer <token>"` |
| **PB-2 Pedido novo** | `POST $API/solicitacoes` com `{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1"}` → 201, guardar `id` (pedido em `RASCUNHO`, `revisao` 1) |
| **PB-3 Enviar arquivo** | `curl -X POST $API/solicitacoes/<id>/documentos -H "Authorization: Bearer <token>" -F "arquivo=@<caminho>" -F "tipoDocumental=<TIPO>"` (opcional `-H "X-Chave-Envio: <8 a 64 caracteres [A-Za-z0-9_-]>"`). Um arquivo por requisição |
| **PB-4 Ler (simulada)** | `POST $API/solicitacoes/<id>/extracoes` (sem corpo) → 201 (ou 200 se já existe para a versão atual); guardar `id` da leitura |
| **PB-5 Conferir** | `GET …/extracoes/<extracaoId>/conferencia`; depois `PUT` no mesmo caminho com `{"decisoes":[{"documentoId":"<id>","codigo":"<CAMPO>","decisao":"CONFIRMADA"}, …]}` para **cada campo `IDENTIFICADO`** (até 18 itens). Isso é a conferência `TUDO_CONFIRMADO`. Só campo confirmado vale como evidência |
| **PB-6 Analisar** | `POST $API/solicitacoes/<id>/analises` (sem corpo) → 201 (ou 200 se a vigente já vale); comparar `resultados[]` e `resultadoGeral` com o `gabarito.md`. Resumo simulado: na análise ou por `POST …/analises/<analiseId>/resumo` |
| **PB-7 Percurso do conjunto NN** | PB-1 (paciente A) → PB-2 → PB-3 para cada arquivo do conjunto → PB-4 → PB-5 (exceto no conjunto 15, que **não** tem conferência) → PB-6 → comparar com `samples/conjuntos/<NN>-<nome>/gabarito.md` |

### 5.5 Falhas controladas e restauração

| Situação | Como provocar | Como desfazer |
|---|---|---|
| Falha do provedor de IA / saída fora do contrato (T12) | **Só com dublê**: teste automatizado (`ResumoEndpointTest`). Não há como provocar pela API em execução; isso é limitação declarada, não falta de caso | nada a desfazer (teste usa repositório em memória) |
| Falha do leitor simulado (T12) | `LeituraMalSucedidaTest` (dublê de leitor) | idem |
| Sessão expirada (T01, T15) | subir a API com `SESSION_TTL_MINUTES=1`, entrar e esperar mais de 1 min | reiniciar com o valor padrão |
| Corte de rede do app (T15) | no emulador: `adb shell svc wifi disable` e `adb shell svc data disable` (usado nas evidências RC-18 e RC-19) | `adb shell svc wifi enable` e `adb shell svc data enable` |
| API sem banco (T15-04) | subir a API **sem** `DB_*` | exportar `DB_*` e reiniciar |
| Sensor "antigo" (T16-04) | enviar a sequência e esperar mais de 60 s sem enviar | enviar nova sequência |
| Sensor sem chave (T16) | omitir ou alterar `X-Chave-Simulador` na chamada | — |

**Restauração dos dados entre execuções:**
- Pedidos, documentos e leituras criados por um caso são desse caso; cada repetição cria um pedido novo (PB-2). Não reutilize pedido de execução anterior.
- A carga `02_carga_sintetica.sql` falha com ORA-00001 se reaplicada e **não** deve ser reaplicada sem limpeza.
- `90_limpeza_DESTRUTIVA.sql` apaga as tabelas `RC_*` e **só roda com autorização específica** do responsável.
- Para limpar o que um caso criou, exclua o pedido (rascunho) pelo caso T14-01; pedidos submetidos ou analisados não são removíveis pela API e ficam como histórico.
- Registre na evidência o `id` do pedido criado.

## 6. Convenção de evidências

Pasta `docs/evidencias/<ID-DO-CASO>/` (ex.: `docs/evidencias/T06-02/`), criada só na execução (RC-25). Conteúdo mínimo:

1. data e hora; 2. versão ou commit da API e do app; 3. comando ou passos executados (com `id` do pedido); 4. resultado bruto (saída, resposta HTTP, captura); 5. status final e executor.

Nunca registrar token, senha, chave do simulador, `DB_*` nem conteúdo de documento real. Evidência reaproveitada de outra data (por exemplo, [RC-18](../evidencias/2026-11-17_rc18-app-android.md) e [RC-19](../evidencias/2026-11-17_rc19-app-documentos-resultado.md)) **não** substitui a execução do caso.

## 7. Catálogo de casos

Legenda do **Tipo de execução**: `API real + Oracle` · `API em memória (sem banco)` (= jar real da API iniciado **sem** `DB_*`; nenhum dado é gravado) · `Teste automatizado` · `Dublê (não é integração real)` · `App no emulador + API real`. Em todo caso, **Ambiente** = `commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais)` (a preencher na execução). Teste automatizado: a partir de `backend/`, `.\mvnw.cmd -Dtest=<Classe>#<método> test`; os `*OracleTest` são opt-in (`RC_TESTE_ORACLE=SIM` e `DB_*`) e sem isso ficam ignorados (isso **não** é aprovação).

---

### T01 — Autenticação (RC-04, RC-18)

#### T01-01 — Login com credenciais válidas
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; carga sintética aplicada; conta `paciente.a@exemplo.invalid` ativa |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | e-mail da conta A e a senha de demonstração (seção 5.2) |
| Passos | 1. PB-1 com a conta A. 2. `GET $API/auth/sessao` com o token |
| Resultado esperado | Login: **200** com `token`, `tipo` "Bearer", `expiraEm` e `usuario.perfil` = `PACIENTE`. Sessão: 200 com a mesma identidade e `expiraEm`. A resposta **não** traz senha, hash nem id interno |
| Impedimento conhecido | nenhum |
| Resultado obtido | Login 200 (Bearer, expiraEm, PACIENTE) e sessão 200 com a mesma identidade; sem senha nem hash na resposta. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T01-01/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T01-02 — Senha incorreta
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; conta A ativa |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | e-mail da conta A com senha errada |
| Passos | 1. `POST $API/auth/login` com a senha errada. 2. Guardar status e corpo. 3. Tentar `GET $API/auth/sessao` sem token |
| Resultado esperado | **401** `CREDENCIAIS_INVALIDAS`, mensagem "E-mail ou senha inválidos."; nenhum token emitido; corpo sem senha, hash ou detalhe interno; sessão sem token → 401 `NAO_AUTENTICADO` |
| Impedimento conhecido | nenhum |
| Resultado obtido | 401 CREDENCIAIS_INVALIDAS sem token; sessão sem token 401 NAO_AUTENTICADO. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T01-02/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T01-03 — E-mail inexistente (resposta idêntica à senha errada)
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; resposta de T01-02 guardada |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | e-mail `ninguem@exemplo.invalid` (inexistente) e qualquer senha |
| Passos | 1. `POST $API/auth/login` com esse e-mail. 2. Comparar status e corpo com o de T01-02 |
| Resultado esperado | **401** `CREDENCIAIS_INVALIDAS` com **corpo idêntico** ao de T01-02 (nenhuma diferença que revele se a conta existe) |
| Impedimento conhecido | nenhum |
| Resultado obtido | 401 CREDENCIAIS_INVALIDAS com corpo idêntico ao da senha errada. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T01-03/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T01-04 — Login com campos ausentes ou vazios
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API no ar |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | (a) `{}`; (b) `{"email":"","senha":""}`; (c) corpo que não é JSON; (d) `Content-Type: text/plain` |
| Passos | 1. Enviar cada variação a `POST $API/auth/login`. 2. Guardar status e corpo |
| Resultado esperado | (a) e (b): **400** `REQUISICAO_INVALIDA` com `detalhes` por campo; (c): 400; (d): **415** `TIPO_NAO_SUPORTADO`; em nenhuma há sessão aberta |
| Impedimento conhecido | nenhum |
| Resultado obtido | (a),(b) 400 REQUISICAO_INVALIDA com detalhes; (c) 400; (d) 415 TIPO_NAO_SUPORTADO; nenhuma sessão aberta. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T01-04/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T01-05 — Rota privada sem sessão, com token inválido e depois do logout
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; conta A com token válido |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | requisições a `GET $API/solicitacoes` |
| Passos | 1. Chamar sem `Authorization`. 2. Chamar com token inventado. 3. Entrar (PB-1), `POST $API/auth/logout` e chamar de novo com o mesmo token |
| Resultado esperado | Passos 1 e 2: **401** `NAO_AUTENTICADO` com a mesma resposta; logout: **204**; repetir com o token encerrado: 401 `NAO_AUTENTICADO` |
| Impedimento conhecido | nenhum |
| Resultado obtido | 401 sem token e com token inventado (iguais); logout 204; token encerrado 401. (TTL de 1 min não exercitado aqui: fica para T15-03.) |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T01-05/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T01-06 — Resposta e log não vazam senha, hash ou token
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco (repositórios em memória) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | testes `AuthEndpointLoginTest` e `HigieneDeLogTest` |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=AuthEndpointLoginTest,HigieneDeLogTest test`. 2. Registrar a saída |
| Resultado esperado | Testes passam, inclusive `respostaNuncaTemHashDeSenhaNemIdInterno` e `quatroFalhasDeCredencialTemCorpoIdenticoENaoAbremSessao`; nenhum teste ignorado sem registro |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T01-07 — App: login com senha incorreta
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-04, RC-18 |
| Regra | não se aplica |
| Tipo de execução | App no emulador + API real |
| Pré-condições | API com Oracle; APK instalado (`mobile/README.md`); emulador com `10.0.2.2:8080` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | e-mail da conta A com senha errada |
| Passos | 1. Abrir o app. 2. Entrar com a senha errada |
| Resultado esperado | Mensagem "E-mail ou senha incorretos."; e-mail mantido e senha limpa; o app não guarda sessão; nenhuma senha, token ou e-mail no `adb logcat` do processo do app |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T01-08 — App: conta administrador não entra no app
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-04, RC-05, RC-18 |
| Regra | não se aplica |
| Tipo de execução | App no emulador + API real |
| Pré-condições | API com Oracle; APK instalado |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `admin@exemplo.invalid` e a senha de demonstração |
| Passos | 1. No app, entrar com a conta administrador. 2. Fechar e reabrir o app |
| Resultado esperado | Mensagem "Esta conta não é de paciente. Entre com uma conta de paciente."; nada guardado; ao reabrir, o app cai no login |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T02 — Isolamento entre pacientes (RC-05, RC-06)

Preparação comum: com a conta **B**, criar um pedido (PB-2) e enviar o conjunto 01 (PB-3, `ENCAMINHAMENTO` e `IDENTIFICACAO`), PB-4 e PB-6; guardar `idB`, `docIdB`, `analiseIdB`. Com a conta **A**, guardar também um id inexistente, por exemplo `999999999`. O esperado de T02 é sempre "**404 `NAO_ENCONTRADO` igual ao de recurso inexistente**" (diferença da issue: seção 3.3).

#### T02-01 — A não vê pedidos de B na listagem nem no detalhe
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Preparação comum; contas A e B |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | token de A; `idB` |
| Passos | 1. `GET /solicitacoes` como A. 2. `GET /solicitacoes/<idB>` como A. 3. `GET /solicitacoes/999999999` como A. 4. Comparar os passos 2 e 3 |
| Resultado esperado | Listagem de A **sem** o pedido `idB`; passo 2: **404** `NAO_ENCONTRADO` com mensagem "Recurso nao encontrado."; passos 2 e 3 com corpo **idêntico** |
| Impedimento conhecido | nenhum |
| Resultado obtido | Listagem de A sem o pedido de B; detalhe alheio 404 NAO_ENCONTRADO idêntico ao de inexistente. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T02-01/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T02-02 — A não altera nem exclui pedido de B
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-06 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Preparação comum; um **rascunho** de B (PB-2) chamado `rascB` |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | token de A; `rascB` |
| Passos | 1. `PUT /solicitacoes/<rascB>` como A com o serviço válido. 2. `DELETE /solicitacoes/<rascB>` como A com `X-Confirmar-Exclusao: EXCLUIR`. 3. Como B, `GET /solicitacoes/<rascB>` |
| Resultado esperado | Passos 1 e 2: **404** `NAO_ENCONTRADO` idêntico ao de inexistente; passo 3: o rascunho de B continua existindo, sem alteração (`revisao`, `atualizadaEm` iguais) |
| Impedimento conhecido | nenhum |
| Resultado obtido | PUT e DELETE de A no rascunho de B → 404 idêntico ao de inexistente; rascunho de B intacto. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T02-02/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T02-03 — A não baixa nem lista documento de B
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-06 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Preparação comum; `DOCUMENT_STORAGE_PATH` definido (sem ele o download responde 503) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | token de A; `idB`, `docIdB` |
| Passos | 1. `GET /solicitacoes/<idB>/documentos` como A. 2. `GET /solicitacoes/<idB>/documentos/<docIdB>` como A. 3. Como B, baixar o mesmo documento (controle) |
| Resultado esperado | Passos 1 e 2: **404** `NAO_ENCONTRADO` idêntico ao de inexistente, **sem nenhum byte do arquivo**; passo 3 (B): 200 com o arquivo como anexo |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T02-04 — A não acessa leitura, conferência, análise, resumo nem submissão de B
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-06 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Preparação comum, com leitura, conferência e análise de B |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | token de A; `idB`, `analiseIdB` |
| Passos | Como A: 1. `GET /solicitacoes/<idB>/extracoes`. 2. `GET …/analises`. 3. `POST …/analises`. 4. `POST …/analises/<analiseIdB>/resumo`. 5. `POST …/submissoes-simuladas`. 6. `GET …/submissoes-simuladas`. 7. `POST …/sessoes-captura` |
| Resultado esperado | Todos: **404** `NAO_ENCONTRADO` idêntico ao de inexistente, sem conteúdo do pedido de B; nada é criado para B (conferir como B: `GET …/analises` continua com a análise original e uma só) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T02-05 — Corpo inválido em pedido alheio continua 404
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-06 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Preparação comum |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | token de A; `idB`; corpo inválido (`{"protocoloId":""}`) e arquivo de texto |
| Passos | 1. `PUT /solicitacoes/<idB>` como A com corpo inválido. 2. `POST …/documentos` em `idB` como A com o arquivo `samples/conjuntos/12-formato-invalido/documento-sintetico.txt` |
| Resultado esperado | **404** `NAO_ENCONTRADO` nos dois (a dona é conferida antes do corpo): não vira 400 nem 415, e não revela se o pedido existe |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T02-06 — Matriz automatizada de isolamento
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-06 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `MatrizPacienteTest`, `OrdemDeVerificacaoTest`, `MatrizPerfisDocumentoTest`, `MatrizPerfisAnaliseTest` |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=MatrizPacienteTest,OrdemDeVerificacaoTest,MatrizPerfisDocumentoTest,MatrizPerfisAnaliseTest test`. 2. Registrar a saída |
| Resultado esperado | Todos passam: 401 sem token, 403 com perfil indevido, 404 para alheio ou inexistente, sucesso para a dona |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T03 — Permissões administrativas (RC-05, RC-20)

#### T03-01 — Paciente em rota de indicadores administrativos
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-20 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; token da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | rota de indicadores administrativos (`/admin/indicadores` no CONTRATO_TESTES.md; confirmar o caminho real na RC-20) |
| Passos | 1. Chamar a rota como paciente A |
| Resultado esperado | **403** `ACESSO_NEGADO`, mensagem "Voce nao tem permissao para esta operacao.", sem nenhum dado agregado |
| Impedimento conhecido | **RC-20 (P1, aberta): a rota não existe**. Hoje a resposta seria 404 `NAO_ENCONTRADO`, que **não** satisfaz este caso |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T03-02 — Administrador recebe somente agregados
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-05, RC-20 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; token de `admin@exemplo.invalid` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | rota de indicadores (RC-20) |
| Passos | 1. Chamar a rota como administrador. 2. Procurar na resposta qualquer dado individual (nome, documento, análise, resumo) |
| Resultado esperado | **200** com indicadores **agregados**; nenhum dado individual de paciente na resposta |
| Impedimento conhecido | **RC-20 (P1, aberta): rota inexistente** |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T03-03 — Administrador não acessa pedido individual de paciente
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; token do administrador; pedido da conta A (PB-2) e um id inexistente |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `GET /solicitacoes`, `GET /solicitacoes/<idA>`, `GET /solicitacoes/999999999` com token do administrador |
| Passos | 1. Chamar as três rotas como administrador. 2. Comparar as respostas dos dois últimos |
| Resultado esperado | **403** `ACESSO_NEGADO` nos três, **inclusive** para o id inexistente (o perfil é conferido antes de consultar o recurso); corpo sem dado do pedido |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T03-04 — Administrador não baixa documento, análise nem submissão
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-06 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido da conta A com documento, análise e submissão (conjunto 01 até PB-6 e `POST …/submissoes-simuladas`); token do administrador |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `idA`, `docIdA`, `analiseIdA` |
| Passos | Como administrador: 1. `GET …/documentos/<docIdA>`. 2. `GET …/analises`. 3. `POST …/analises`. 4. `GET …/submissoes-simuladas`. 5. `DELETE /solicitacoes/<idA>` |
| Resultado esperado | **403** `ACESSO_NEGADO` em todos; nada é lido, criado ou apagado (conferir como A que o pedido está intacto) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T04 — Criação e edição de rascunho (RC-07)

#### T04-01 — Criar rascunho válido
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-07 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; token da conta A; `GET /protocolos` lista `PROTOCOLO-ACADEMICO-FICTICIO-v0.1` |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | `{"protocoloId":"PROTOCOLO-ACADEMICO-FICTICIO-v0.1"}` |
| Passos | 1. PB-2. 2. `GET /solicitacoes/<id>`. 3. `GET /solicitacoes` |
| Resultado esperado | **201** com `Location: /solicitacoes/<id>`, `estado` `RASCUNHO`, `revisao` 1, `simulacao` true, `servico` com o aviso fictício; o GET devolve os mesmos dados (persistidos); o pedido aparece na listagem da paciente A e **só** nela |
| Impedimento conhecido | nenhum |
| Resultado obtido | 201 + Location, RASCUNHO, revisao 1, simulacao true; GET igual; aparece só na listagem de A. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T04-01/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T04-02 — Campo obrigatório ausente, vazio ou inválido
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-07 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API com Oracle; token da conta A; contagem de pedidos de A antes (`GET /solicitacoes`) |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | `POST /solicitacoes` e `PUT /solicitacoes/<id>` (rascunho existente) com `{}`, `{"protocoloId":""}`, `{"protocoloId":null}`, `{"protocoloId":123}` e `{"protocoloId":"<101 caracteres>"}` |
| Passos | 1. Enviar cada corpo por `POST`. 2. Enviar cada corpo por `PUT` no rascunho existente. 3. Conferir a contagem de pedidos de A e o `GET` do rascunho (`protocoloId`, `revisao`, `atualizadaEm`) |
| Resultado esperado | **400** `REQUISICAO_INVALIDA` com `detalhes[].campo` = `protocoloId` em todos; **nenhum pedido criado** (contagem igual); no `PUT`, a alteração inválida **não é persistida** (serviço, `revisao` e `atualizadaEm` iguais aos de antes) |
| Impedimento conhecido | nenhum |
| Resultado obtido | 10 corpos inválidos (5 POST e 5 PUT) → 400 REQUISICAO_INVALIDA com campo protocoloId; nada criado nem alterado. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T04-02/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T04-03 — Serviço inexistente
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-07 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A (PB-2) |
| Ambiente | commit 7532440 · API: regulacheck-backend.jar + Oracle da faculdade · dispositivo: n/a |
| Entrada | `{"protocoloId":"SERVICO-QUE-NAO-EXISTE-v9"}` |
| Passos | 1. `POST /solicitacoes` com esse valor. 2. `PUT /solicitacoes/<id>` com esse valor |
| Resultado esperado | **422** `REGRA_DE_NEGOCIO` com `detalhes[].campo` = `protocoloId` (a mesma resposta para serviço inexistente ou inativo); o rascunho existente permanece com o serviço e a `revisao` anteriores |
| Impedimento conhecido | nenhum |
| Resultado obtido | POST e PUT com serviço inexistente → 422 REGRA_DE_NEGOCIO (protocoloId); rascunho intacto. |
| Status | Aprovado |
| Evidência | [registro](../evidencias/T04-03/registro.md) |
| Responsável e data | grupo CodeMasters (com apoio do Claude Code) · 2026-11-19 |

#### T04-04 — Trocar o serviço do rascunho (único campo editável)
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-07 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco. A carga tem **um** serviço ativo, então a troca para um serviço *diferente* só é reproduzível por teste (limitação declarada) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `TrocarServicoServiceTest` (`servicoDiferenteIncrementaARevisaoERegistraOMotivo`, `mesmoServicoNaoMudaRevisaoNemDataNemHistorico`) e `ConcorrenciaTrocaServicoTest` |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=TrocarServicoServiceTest,ConcorrenciaTrocaServicoTest test`. 2. Em execução manual, também: `PUT /solicitacoes/<id>` com o **mesmo** serviço |
| Resultado esperado | Serviço diferente → `revisao` +1, `atualizadaEm` = agora, revisão com motivo `PROTOCOLO_ALTERADO`. Mesmo serviço → 200 **sem** mudar `revisao` nem `atualizadaEm`. Alterações simultâneas do mesmo rascunho são serializadas |
| Impedimento conhecido | nenhum para o teste automatizado; o caso manual de "serviço diferente" depende de um segundo serviço ativo, que não existe (lacuna registrada na seção 9) |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T04-05 — Editar pedido que já saiu de rascunho
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-07 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido da conta A fora de `RASCUNHO` (conjunto 01 até PB-6) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `PUT /solicitacoes/<id>` com o serviço válido |
| Passos | 1. Chamar o `PUT`. 2. `GET /solicitacoes/<id>` |
| Resultado esperado | **409** `CONFLITO_ESTADO`, mensagem "Este pedido não pode mais ser alterado."; `revisao` e `estado` inalterados |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T05 — Upload e extração válidos (RC-08, RC-10)

#### T05-01 — Enviar PDF válido
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A (PB-2); `DOCUMENT_STORAGE_PATH` definido |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `samples/encaminhamento-sintetico.pdf` (1 página), `ENCAMINHAMENTO` |
| Passos | 1. PB-3. 2. `GET /solicitacoes/<id>/documentos`. 3. `GET /solicitacoes/<id>` |
| Resultado esperado | **201** com `Location`; `formato` `PDF`, `paginas` 1, `estadoEnvio` `ENVIADO`, `tipoDocumental` `ENCAMINHAMENTO`, `simulacao` true; a lista mostra 1 documento (`maximo` 3); o pedido ganha `revisao` +1 (motivo `DOCUMENTO_ENVIADO`), continua `RASCUNHO` e `analise.situacao` = `NAO_REALIZADA`; **nenhum caminho interno** de armazenamento aparece |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T05-02 — Enviar PNG válido
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A com o PDF de T05-01 |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `samples/identificacao-sintetica.png`, `IDENTIFICACAO` |
| Passos | 1. PB-3. 2. `GET …/documentos` |
| Resultado esperado | **201**; `formato` `PNG`; lista com 2 documentos; `versaoConjuntoDocumental` = `revisao` do pedido |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T05-03 — Enviar JPEG válido
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-08 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco. **`samples/` não tem amostra JPEG** (só PDF, PNG e TXT); o JPEG do teste é gerado em código e não deve ser copiado para `samples/` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `EnviarDocumentoEndpointTest#jpegEPngTambemSaoAceitos` |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=EnviarDocumentoEndpointTest#jpegEPngTambemSaoAceitos test` |
| Resultado esperado | O teste passa: JPEG aceito com 201 e `formato` `JPEG`; PNG também |
| Impedimento conhecido | Sem amostra JPEG sintética versionada: a execução **manual** por HTTP é lacuna (seção 9); este caso só vale como teste automatizado |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T05-04 — Leitura simulada do conjunto 01 com origem dos campos
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-10 |
| Regra | RC-R01 a RC-R05 (campos usados pelas regras) |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido da conta A com `encaminhamento-sintetico.pdf` e `identificacao-sintetica.png` (T05-01 e T05-02) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 01 ([gabarito](../../samples/conjuntos/01-completo/gabarito.md)) |
| Passos | 1. PB-4. 2. Comparar `documentos[].campos[]` com a tabela "Campos esperados" do gabarito. 3. `GET …/extracoes` |
| Resultado esperado | **201**; `simulada` true e `aviso` "Leitura simulada com dados sintéticos; não é leitura real dos documentos."; campos iguais ao gabarito (por exemplo `NOME` `IDENTIFICADO` "Maria Exemplo da Silva" com `origem` pág. 1; `AUTORIZACAO_ASSINADA` `NAO_LOCALIZADO` sem valor nem origem); `situacao` `ATUAL`; o pedido **não** muda de estado nem de revisão |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T05-05 — Repetir a leitura não duplica
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-10 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Leitura de T05-04 feita |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | PB-4 de novo, sem mudar documentos |
| Passos | 1. PB-4 outra vez. 2. Comparar o `id` da leitura |
| Resultado esperado | **200** com a **mesma** leitura (mesmo `id`); nenhuma leitura nova criada |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T05-06 — Ler pedido sem documentos
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-10 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho novo da conta A **sem** documento |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | PB-4 |
| Passos | 1. PB-4. 2. `GET …/extracoes` |
| Resultado esperado | **422** `SEM_DOCUMENTOS`; nada é criado; `GET …/extracoes` → 404 |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T06 — Documentos não aceitos (RC-06, RC-08, RC-10)

Para todos os casos de T06: antes e depois, guardar `GET …/documentos` (`total`, `versaoConjuntoDocumental`) e `GET /solicitacoes/<id>` (`revisao`, `analise.situacao`).

#### T06-01 — Formato inválido (texto)
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A; conjunto 12 (recusa no envio: nada é gravado, não há leitura nem análise e nenhuma regra é avaliada) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `samples/conjuntos/12-formato-invalido/documento-sintetico.txt` ([gabarito](../../samples/conjuntos/12-formato-invalido/gabarito.md)) |
| Passos | 1. PB-3 com o arquivo, tipo `OUTRO`. 2. Conferir lista, revisão e análise |
| Resultado esperado | **415** com código `TIPO_NAO_SUPORTADO` **ou** `EXTENSAO_INCOMPATIVEL` (o gabarito do conjunto 12 aceita qualquer dos dois: a documentação não fixa qual verificação responde primeiro; registrar o código observado); mensagem com orientação clara; arquivo **não** entra na lista, **não** cria revisão, não deixa arquivo guardado; `analise.situacao` segue `NAO_REALIZADA` e nenhum resultado de completude existe |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T06-02 — PDF com mais de 5 páginas
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `samples/conjuntos/12-formato-invalido/pdf-seis-paginas.pdf` |
| Passos | 1. PB-3 com o arquivo. 2. Conferir lista e revisão |
| Resultado esperado | **400** `REQUISICAO_INVALIDA` com `detalhes[].campo` = `arquivo` e orientação sobre o limite de 5 páginas; nada gravado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T06-03 — PDF protegido por senha
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08, RC-10 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A. **Não há PDF protegido em `samples/`**: gerar um fora do repositório a partir da amostra sintética, por exemplo `qpdf --encrypt senha-de-teste senha-de-teste 256 -- samples/encaminhamento-sintetico.pdf "$TMP/protegido.pdf"` (se `qpdf` não estiver instalado, salvar a amostra com senha em um leitor de PDF; a senha é descartável e não vai para a evidência). Alternativa: `ConferenciaDeArquivoTest` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | PDF sintético protegido por senha |
| Passos | 1. PB-3 com esse PDF. 2. Conferir lista e revisão |
| Resultado esperado | **HTTP 400** `REQUISICAO_INVALIDA` com `detalhes[].campo` = `arquivo` e orientação clara (contrato `docs/api/erros.md`); nada gravado; não é 415 nem 422 |
| Impedimento conhecido | Sem amostra versionada de PDF protegido: a RC-25 precisa criá-la, ou registrar a lacuna |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T06-04 — Arquivo vazio ou corrompido
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | (a) arquivo de 0 bytes `vazio.pdf`; (b) `truncado.pdf`. Criar fora do repositório (bash, `TMP` = pasta temporária): `: > "$TMP/vazio.pdf"` e `head -c 500 samples/encaminhamento-sintetico.pdf > "$TMP/truncado.pdf"` |
| Passos | 1. PB-3 com (a). 2. PB-3 com (b). 3. Conferir lista e revisão |
| Resultado esperado | **400** `REQUISICAO_INVALIDA` (`detalhes[].campo` = `arquivo`) nos dois; nada gravado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T06-05 — Arquivo acima de 5 MB
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A; gerar fora do repositório um PDF sintético com mais de 5 MB: `{ cat samples/encaminhamento-sintetico.pdf; head -c 5300000 /dev/zero; } > "$TMP/grande.pdf"` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `grande.pdf` (mais de 5 242 880 bytes). Se a API responder 400 ou 415 em vez de 413 por conferir o conteúdo antes do tamanho, registrar como achado |
| Passos | 1. PB-3 com o arquivo. 2. Conferir lista e revisão |
| Resultado esperado | **413** `ARQUIVO_MUITO_GRANDE` ("O arquivo excede 5 MB."); a leitura para no limite; nada gravado. (Diferença: imagem com pixels demais é **400**, não 413) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T06-06 — Extensão incompatível com o conteúdo
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | cópia de `samples/identificacao-sintetica.png` salva como `foto.pdf` (fora do repositório) |
| Passos | 1. PB-3 com esse arquivo. 2. Conferir lista e revisão |
| Resultado esperado | **415** `EXTENSAO_INCOMPATIVEL`; nada gravado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T06-07 — Quarto documento e várias partes na mesma requisição
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido da conta A com **3** documentos válidos (por exemplo, conjunto 08: PDF, PNG e PDF de autorização) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | (a) um 4º arquivo válido; (b) em outro rascunho, requisição `multipart` com 2 partes `arquivo` |
| Passos | 1. PB-3 do 4º arquivo. 2. `curl` com duas partes `-F "arquivo=@…"` |
| Resultado esperado | (a) **422** `REGRA_DE_NEGOCIO` com `detalhes[].campo` = `documentos` (já há 3); (b) **400** `REQUISICAO_INVALIDA` com `detalhes[].campo` = `arquivos`; em ambos nada gravado e a lista segue com os documentos anteriores |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T06-08 — Recusas por teste automatizado (matriz de motivos)
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-08 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `RecusaDeArquivoEndpointTest`, `ConferenciaDeArquivoTest`, `LimitesTest`, `MotivoRecusaTest` |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=RecusaDeArquivoEndpointTest,ConferenciaDeArquivoTest,LimitesTest,MotivoRecusaTest test` |
| Resultado esperado | Passam; `cadaArquivoInvalidoEhRecusadoSemDeixarRastro` confirma que nenhum arquivo recusado é persistido |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T07 — Documento completo (RC-11, RC-12, RC-14, RC-15)

#### T07-01 — Conjunto 01: análise sem pendências
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-11, RC-12, RC-13, RC-15 |
| Regra | RC-R01 ATENDE, RC-R02 ATENDE, RC-R03 ATENDE, RC-R04 ATENDE, RC-R05 ATENDE, RC-R06 NÃO APLICÁVEL |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A; arquivos do conjunto 01 |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 01 ([gabarito](../../samples/conjuntos/01-completo/gabarito.md)), conferência `TUDO_CONFIRMADO` |
| Passos | 1. PB-7 com o conjunto 01. 2. `GET …/analises` |
| Resultado esperado | Análise **201**, `vigente` true, `simulada` true, `aviso` dizendo que "Sem pendências identificadas" não é aprovação do SUS; `resultados[]` e `resultadoGeral` = **`SEM_PENDENCIAS_IDENTIFICADAS`** como no gabarito; `pendencias` vazia; o pedido vai a `SEM_PENDENCIAS_IDENTIFICADAS` |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T07-02 — Resumo simulado, coerente e sem promessa oficial
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-14, RC-15 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Análise de T07-01 |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | resumo da análise (na resposta ou `POST …/analises/<analiseId>/resumo`) |
| Passos | 1. Obter o resumo. 2. Ler o texto e os campos de origem |
| Resultado esperado | Resumo **identificado como simulado** (origem de adaptador simulado), coerente com `SEM_PENDENCIAS_IDENTIFICADAS`; sem diagnóstico, urgência, vaga, prazo ou promessa de atendimento; não altera regra, resultado nem estado do pedido |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T07-03 — Só campo confirmado vale como evidência
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-11, RC-12 |
| Regra | RC-R01 a RC-R06 NÃO VERIFICÁVEL (sem conferência) |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 15 ([gabarito](../../samples/conjuntos/15-nada-conferido/gabarito.md)): mesmos arquivos do 01, **sem** PB-5 |
| Passos | 1. PB-1, PB-2, PB-3 (os dois arquivos do 01), PB-4. 2. **Não** confirmar nada. 3. PB-6 |
| Resultado esperado | Análise com as seis regras `NAO_VERIFICAVEL` (motivo `TRANSCRICAO_NAO_CONFERIDA`) e `resultadoGeral` **`REQUER_CONFERENCIA`**, como no gabarito do conjunto 15; o resultado **não** é `SEM_PENDENCIAS_IDENTIFICADAS` |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T08 — Informações ou documentos ausentes (RC-12, RC-14, RC-15)

#### T08-01 — Documento de identificação ausente
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-12, RC-14, RC-15 |
| Regra | RC-R01 ATENDE, RC-R02 NÃO VERIFICÁVEL, RC-R03 PENDENTE (responsável PACIENTE), RC-R04 ATENDE, RC-R05 ATENDE, RC-R06 NÃO VERIFICÁVEL |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 02 ([gabarito](../../samples/conjuntos/02-sem-identificacao/gabarito.md)) |
| Passos | 1. PB-7 com o conjunto 02. 2. Ler `pendencias[]` e o resumo |
| Resultado esperado | `resultadoGeral` **`COM_PENDENCIAS`**; regras como no gabarito; a pendência da RC-R03 traz regra, explicação, `responsavel` `PACIENTE` e orientação para enviar o documento; **nenhum valor lido** aparece na pendência; resumo simulado cita a pendência |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T08-02 — Motivo do encaminhamento ausente (responsável: unidade emissora)
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-12, RC-14, RC-15 |
| Regra | RC-R04 PENDENTE (responsável UNIDADE_EMISSORA); RC-R01, R02, R03, R05 ATENDE; RC-R06 NÃO APLICÁVEL |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 03 ([gabarito](../../samples/conjuntos/03-sem-motivo/gabarito.md)) |
| Passos | 1. PB-7 com o conjunto 03. 2. Ler `pendencias[]`. 3. Tentar `PUT` da conferência com um campo `valor` preenchendo o motivo |
| Resultado esperado | `resultadoGeral` **`COM_PENDENCIAS`**; a pendência da RC-R04 tem `responsavel` `UNIDADE_EMISSORA` e orientação de pedir o documento corrigido à unidade emissora; o passo 3 é recusado (**400**: nenhum valor digitado é aceito) — a paciente não altera o documento clínico |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T08-03 — Várias pendências de uma vez
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-12, RC-14, RC-15 |
| Regra | RC-R01 ATENDE; RC-R02 NÃO VERIFICÁVEL; RC-R03 PENDENTE (PACIENTE); RC-R04 PENDENTE (UNIDADE_EMISSORA); RC-R05 PENDENTE (UNIDADE_EMISSORA); RC-R06 NÃO VERIFICÁVEL |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 04 ([gabarito](../../samples/conjuntos/04-varias-pendencias/gabarito.md)) |
| Passos | 1. PB-7 com o conjunto 04. 2. Conferir cada regra e cada pendência |
| Resultado esperado | `resultadoGeral` **`COM_PENDENCIAS`**; **todas** as regras foram avaliadas (uma pendência não interrompe as demais); três pendências com os responsáveis do gabarito; resumo simulado cita todas |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T09 — Regras condicionais (RC-01, RC-12)

Regra condicional: **RC-R06** (autorização do responsável legal, aplicável a paciente com menos de 18 anos na data do envio).

#### T09-01 — Aplicável e atendida (menor com autorização)
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-01, RC-12 |
| Regra | RC-R06 ATENDE; RC-R01 a RC-R05 ATENDE |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 08 ([gabarito](../../samples/conjuntos/08-menor-com-autorizacao/gabarito.md)): `encaminhamento.pdf`, `identificacao.png`, `autorizacao.pdf` (`AUTORIZACAO_RESPONSAVEL`) |
| Passos | 1. PB-7 com o conjunto 08 |
| Resultado esperado | `resultadoGeral` **`SEM_PENDENCIAS_IDENTIFICADAS`**; RC-R06 `ATENDE` |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T09-02 — Aplicável e pendente (menor sem autorização)
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-01, RC-12 |
| Regra | RC-R06 PENDENTE (responsável PACIENTE); RC-R01 a RC-R05 ATENDE |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 09 ([gabarito](../../samples/conjuntos/09-menor-sem-autorizacao/gabarito.md)) |
| Passos | 1. PB-7 com o conjunto 09 |
| Resultado esperado | `resultadoGeral` **`COM_PENDENCIAS`**; RC-R06 `PENDENTE`, `responsavel` `PACIENTE`, orientação de enviar a autorização do responsável |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T09-03 — Não aplicável (maioridade no limite)
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-01, RC-12 |
| Regra | RC-R06 NÃO APLICÁVEL; RC-R01 a RC-R05 ATENDE |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A. Em execução manual vale o dia do envio (a fronteira dos 18 anos em 12/11/2026 só é exercitada na regressão automática) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 10 ([gabarito](../../samples/conjuntos/10-maioridade-limite/gabarito.md)) |
| Passos | 1. PB-7 com o conjunto 10. 2. Para a fronteira: `.\mvnw.cmd -Dtest=ConjuntosDemonstracaoTest test` a partir de `backend/` |
| Resultado esperado | `resultadoGeral` **`SEM_PENDENCIAS_IDENTIFICADAS`**; RC-R06 `NAO_APLICAVEL` (não é exigida autorização); a regressão automática confirma o mesmo resultado na data de referência 2026-11-12 |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T09-04 — Condição impossível de verificar exige conferência
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-01, RC-12 |
| Regra | RC-R06 NÃO VERIFICÁVEL (data de nascimento ilegível: não se sabe se a paciente é menor); RC-R02 e RC-R03 NÃO VERIFICÁVEL; RC-R01, R04, R05 ATENDE |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 05 ([gabarito](../../samples/conjuntos/05-foto-ilegivel/gabarito.md)): `DATA_NASCIMENTO` `ILEGIVEL` na identificação. Variante sem conferência: conjunto 15 (T07-03) |
| Passos | 1. Reaproveitar a análise de T10-01 e ler `resultados[]` |
| Resultado esperado | RC-R06 `NAO_VERIFICAVEL` (a exigência nem é feita nem dispensada); `resultadoGeral` `REQUER_CONFERENCIA`; o pedido **não** é liberado automaticamente (uma tentativa de submissão responde 409 `CONFLITO_ESTADO`) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |


---

### T10 — Leitura não confiável (RC-10, RC-11, RC-15)

#### T10-01 — Foto ilegível: requer conferência, sem afirmar ausência
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-10, RC-11, RC-15 |
| Regra | RC-R02 NÃO VERIFICÁVEL; RC-R03 NÃO VERIFICÁVEL; RC-R01, R04, R05 ATENDE; RC-R06 NÃO VERIFICÁVEL |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 05 ([gabarito](../../samples/conjuntos/05-foto-ilegivel/gabarito.md)) |
| Passos | 1. PB-1, PB-2, PB-3, PB-4. 2. Tentar `PUT` da conferência com `CONFIRMADA` no campo `ILEGIVEL`. 3. Confirmar os demais campos e PB-6 |
| Resultado esperado | Passo 2: **422** `DECISAO_NAO_APLICAVEL` (campo ilegível não aceita decisão; nada é gravado). Análise: `resultadoGeral` **`REQUER_CONFERENCIA`** com as regras do gabarito; o campo ilegível **não** é apresentado como "ausente" nem gera pendência de "não encontrado" |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T10-02 — Foto cortada
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-10, RC-11, RC-15 |
| Regra | RC-R02 NÃO VERIFICÁVEL; RC-R03 PENDENTE (PACIENTE); RC-R06 NÃO VERIFICÁVEL; RC-R01, R04, R05 ATENDE |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 06 ([gabarito](../../samples/conjuntos/06-foto-cortada/gabarito.md)) |
| Passos | 1. PB-7 com o conjunto 06 |
| Resultado esperado | `resultadoGeral` **`COM_PENDENCIAS`** como no gabarito; a pendência da RC-R03 orienta a paciente a enviar nova foto; sem diagnóstico nem promessa |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T10-03 — Documento de outra pessoa
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-10, RC-11, RC-15 |
| Regra | RC-R02 PENDENTE (PACIENTE); RC-R01, R03, R04, R05 ATENDE; RC-R06 NÃO APLICÁVEL |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 07 ([gabarito](../../samples/conjuntos/07-outra-pessoa/gabarito.md)) |
| Passos | 1. PB-7 com o conjunto 07 |
| Resultado esperado | `resultadoGeral` **`COM_PENDENCIAS`**; RC-R02 `PENDENTE` (nome ou data de nascimento diferem entre documentos); o texto **não** repete o valor lido |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T10-04 — Paciente aponta divergência na transcrição
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-11, RC-15 |
| Regra | regras que dependem do campo divergente ficam NÃO VERIFICÁVEL (motivo `DIVERGENCIA_APONTADA`) — **sem gabarito da RC-23** |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido da conta A com o conjunto 01 lido (PB-1 a PB-4) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `PUT …/conferencia` com `{"decisoes":[{"documentoId":"<id do PDF>","codigo":"NOME","decisao":"DIVERGENTE","motivo":"VALOR_DIFERENTE_DO_DOCUMENTO"}, <CONFIRMADA nos demais campos IDENTIFICADO>]}` e depois PB-6 |
| Passos | 1. Registrar a divergência. 2. `GET …/conferencia`. 3. PB-6. 4. Ler `resultados[]` e `pendencias[]` |
| Resultado esperado | A conferência mostra o campo `NOME` com `naoVerificado` true e a decisão `DIVERGENTE`, **mantendo** o valor lido; `situacao` da conferência `REQUER_CONFERENCIA`. Na análise, as regras que usam o `NOME` (RC-R02) **não afirmam ausência** e ficam `NAO_VERIFICAVEL` com motivo `DIVERGENCIA_APONTADA`; `resultadoGeral` não é `SEM_PENDENCIAS_IDENTIFICADAS`. Esperado derivado de `docs/api/contratos-dados.md` (pendência/motivos); o gabarito da RC-23 trata a divergência como reservada. Se a execução diferir, registrar achado e submeter o esperado ao responsável — **não** editar para coincidir |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T10-05 — Valor digitado pela paciente é recusado
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-11 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Leitura do conjunto 01 feita (PB-1 a PB-4) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `PUT …/conferencia` com `{"decisoes":[{"documentoId":"<id>","codigo":"NOME","decisao":"CONFIRMADA","valor":"Outro Nome"}]}` |
| Passos | 1. Enviar. 2. `GET …/conferencia` e `GET …/extracoes` |
| Resultado esperado | **400** `REQUISICAO_INVALIDA` (qualquer chave além de `documentoId`, `codigo`, `decisao` e `motivo` é recusada); nada gravado; a leitura original (valores, situações, origens) permanece **intacta** |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T11 — Reenvio e revisão documental (RC-09, RC-13, RC-16)

#### T11-01 — Corrigir o conjunto 04 com o conjunto 13
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-09, RC-13, RC-16 |
| Regra | após a correção: RC-R04 e RC-R05 ATENDE (RESOLVIDA); RC-R02 NÃO VERIFICÁVEL, RC-R03 PENDENTE e RC-R06 NÃO VERIFICÁVEL (CONTINUA); RC-R01 ATENDE |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido do conjunto 04 já analisado (T08-03), com `COM_PENDENCIAS` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | [Conjunto 13](../../samples/conjuntos/13-corrigido-de-04/gabarito.md): `encaminhamento.pdf` substitui o do conjunto 04 |
| Passos | 1. Guardar `revisao` e `analiseId` anteriores. 2. `PUT …/documentos/<docId>` (multipart com `samples/conjuntos/13-corrigido-de-04/encaminhamento.pdf`, `ENCAMINHAMENTO`). 3. `GET …/analises` e `GET …/extracoes`. 4. PB-4, PB-5, PB-6. 5. Ler `comparacao` |
| Resultado esperado | Passo 2: **201**, `revisao` +1 (motivo `DOCUMENTO_SUBSTITUIDO`), contagem de documentos inalterada; a análise anterior perde a vigência (`vigente` false, **histórico preservado**) e a leitura fica `DESATUALIZADA`; a nova leitura nasce sem decisões herdadas; a nova análise segue o gabarito do conjunto 13 e `comparacao` marca RC-R04 e RC-R05 como `RESOLVIDA` (de `PENDENTE` para `ATENDE`) e RC-R02, RC-R03 e RC-R06 como `CONTINUA` (R02 e R06 `NAO_VERIFICAVEL`, R03 `PENDENTE` nas duas análises); nenhuma pendência é dada como resolvida antes da nova leitura, conferência e análise; "resolvida" não é aprovação do SUS |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T11-02 — Corrigir o conjunto 09 com o conjunto 14
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-09, RC-13, RC-16 |
| Regra | RC-R06 PENDENTE → ATENDE; resultado geral `SEM_PENDENCIAS_IDENTIFICADAS` |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido do conjunto 09 já analisado (T09-02) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | [Conjunto 14](../../samples/conjuntos/14-corrigido-de-09/gabarito.md): `encaminhamento.pdf` com autorização assinada substitui o do conjunto 09 |
| Passos | 1. `PUT …/documentos/<docId>` com `samples/conjuntos/14-corrigido-de-09/encaminhamento.pdf`. 2. PB-4, PB-5, PB-6. 3. Ler `comparacao` |
| Resultado esperado | Nova análise com `resultadoGeral` **`SEM_PENDENCIAS_IDENTIFICADAS`** e RC-R06 `ATENDE` como no gabarito; `comparacao` com RC-R06 `RESOLVIDA` (anterior `PENDENTE`, atual `ATENDE`) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T11-03 — Resultado antigo não libera o pedido atual
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-13, RC-16 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido com análise `SEM_PENDENCIAS_IDENTIFICADAS` (T07-01); nenhum documento alterado ainda |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | substituir um documento e tentar submeter sem nova análise |
| Passos | 1. Substituir o PDF por outro válido (`PUT …/documentos/<docId>`). 2. `GET …/analises` (vigente?). 3. `POST …/submissoes-simuladas` |
| Resultado esperado | A análise antiga deixa de ser vigente (`analise.situacao` `NAO_REALIZADA`); a submissão responde **409** `CONFLITO_ESTADO` e **nada** é registrado (`GET …/submissoes-simuladas` → 404) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T11-04 — Reenvio guiado (verificar a versão corrigida)
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-16 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido do conjunto 09 analisado e documento substituído pelo do 14 (T11-02 até o passo 1), **sem** PB-5 |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `POST /solicitacoes/<id>/reenvio` |
| Passos | 1. `POST …/reenvio`. 2. Conferir os campos sem decisão (PB-5). 3. `POST …/reenvio` de novo. 4. Repetir uma terceira vez |
| Resultado esperado | Passo 1: **200** `etapa` `CONFERENCIA` (há campo sem decisão; nenhuma análise registrada). Passo 3: **201** `etapa` `CONCLUIDA` com análise nova vigente e `comparacao`. Passo 4: **200** com a vigente (nada novo criado). Em todos `simulada` true |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T11-05 — Edição durante a análise
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-13, RC-16 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco. Reproduzir "editar durante análise em processamento" manualmente **não é controlável** (a análise é síncrona e rápida): limitação declarada |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `AlteracaoDuranteAnaliseTest` e `ConcorrenciaAnaliseTest` |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=AlteracaoDuranteAnaliseTest,ConcorrenciaAnaliseTest test`. 2. Opcional (opt-in): `AnaliseOracleTest` e `ReenvioOracleTest` com `RC_TESTE_ORACLE=SIM` e `DB_*` |
| Resultado esperado | Passam: substituir documento ou mudar decisão durante a análise **não grava** o resultado e o pedido volta ao estado da última análise concluída ou a `RASCUNHO`; resposta 409 `CONFLITO_ESTADO` ("Os documentos ou a conferência mudaram durante a análise…"); uma nova análise da versão atual é gravada depois. Se os `*OracleTest` ficarem ignorados, registrar "não verificado no Oracle" |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T12 — Falhas e limites da IA (RC-06, RC-14)

Não existe provedor de IA real: o adaptador de resumo é **simulado**. Os casos de falha usam **dublê** e **não** comprovam provedor real.

#### T12-01 — Resumo indisponível: resultado determinístico preservado
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-14 |
| Regra | não se aplica |
| Tipo de execução | Dublê (não é integração real) |
| Pré-condições | JDK 21; sem banco |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | adaptador de resumo que falha (dublê em `ResumoEndpointTest`) |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=ResumoEndpointTest#saidaInvalidaEFalhaUsamPadraoEPermitemNovaTentativa test`. 2. Conferir no teste o trecho da **falha** do adaptador |
| Resultado esperado | A análise e as pendências **permanecem** (resultado determinístico intacto); o resumo vem **padrão e identificado**; não há erro HTTP para a paciente; nova tentativa é permitida; nenhum detalhe do provedor vaza |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T12-02 — Saída do resumo fora do contrato é rejeitada
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-06, RC-14 |
| Regra | não se aplica |
| Tipo de execução | Dublê (não é integração real) |
| Pré-condições | JDK 21; sem banco |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | adaptador que devolve saída inválida (código de regra inexistente ou pendência sem vínculo) |
| Passos | 1. Mesmo método do T12-01 (`saidaInvalidaEFalhaUsamPadraoEPermitemNovaTentativa`). 2. Conferir no teste o trecho da **saída inválida**. 3. Opcional: `ResumoEndpointTest#mudancaDaConferenciaDuranteGeracaoNaoPublicaResumoObsoleto` |
| Resultado esperado | A saída inválida é **descartada**; não é apresentada como conferência confiável; vale o resultado determinístico com resumo padrão identificado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T12-03 — Instrução escrita no documento é só dado
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-06, RC-14 |
| Regra | RC-R01 a RC-R05 ATENDE; RC-R06 NÃO APLICÁVEL |
| Tipo de execução | API real + Oracle |
| Pré-condições | Conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | Conjunto 11 ([gabarito](../../samples/conjuntos/11-instrucao-no-documento/gabarito.md)): o motivo do encaminhamento contém texto que tenta dar ordens ao sistema |
| Passos | 1. PB-7 com o conjunto 11. 2. Ler resultados, pendências e resumo simulado |
| Resultado esperado | Resultado igual ao gabarito (`SEM_PENDENCIAS_IDENTIFICADAS`): o texto é tratado como **dado** do campo de motivo, não altera regra, resultado, prompt nem estado, não suprime exigência e não é reproduzido como instrução no resumo |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T12-04 — Falha do leitor simulado
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-10, RC-14 |
| Regra | não se aplica |
| Tipo de execução | Dublê (não é integração real) |
| Pré-condições | JDK 21; sem banco |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `LeituraMalSucedidaTest` (leitor falso) |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=LeituraMalSucedidaTest test` |
| Resultado esperado | `falhaDoLeitorDa503SemDetalheENadaEGravado`: **503** `LEITURA_INDISPONIVEL`, sem detalhe técnico, nada gravado; a repetição funciona e o resultado parcial não é deixado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T12-05 — Falha da análise e nova tentativa
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-13, RC-14 |
| Regra | não se aplica |
| Tipo de execução | Dublê (não é integração real) |
| Pré-condições | JDK 21; sem banco |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `PrazoEFalhaAnaliseTest` |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=PrazoEFalhaAnaliseTest test` |
| Resultado esperado | Falha na avaliação → **503** `ANALISE_INDISPONIVEL`, nada gravado, pedido em `FALHA_ANALISE`; nova tentativa grava; análise parada além do limite de 1 minuto não prende o pedido |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T13 — Submissão simulada (RC-13, RC-17)

#### T13-01 — Submissão válida
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-13, RC-17 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido da conta A com análise vigente `SEM_PENDENCIAS_IDENTIFICADAS` (conjunto 01, T07-01) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `POST /solicitacoes/<id>/submissoes-simuladas` |
| Passos | 1. Submeter. 2. `GET …/submissoes-simuladas`. 3. `GET /solicitacoes/<id>` |
| Resultado esperado | **201** com comprovante: `referencia` `SIM-2026-<id com ≥6 dígitos>`, `simulada` true, `destaque` "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", `versaoAnalisada` (análise e revisão), `nota` "Este comprovante é de demonstração…"; o pedido vai a `SUBMETIDA_SIMULACAO`; o GET devolve o **mesmo** comprovante |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T13-02 — Submissão repetida é idempotente
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-17 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido já submetido (T13-01) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `POST …/submissoes-simuladas` de novo |
| Passos | 1. Repetir a submissão duas vezes. 2. Comparar `referencia` e `submetidaEm` |
| Resultado esperado | **200** com o **mesmo** comprovante; **uma única** submissão por pedido |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T13-03 — Submissão com pendência é recusada
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-13, RC-17 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido analisado `COM_PENDENCIAS` (conjunto 03, T08-02) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `POST …/submissoes-simuladas` |
| Passos | 1. Submeter. 2. `GET /solicitacoes/<id>` e `GET …/submissoes-simuladas` |
| Resultado esperado | **409** `CONFLITO_ESTADO`; a mensagem diz que a restrição é do **simulador acadêmico** e não impede atendimento em serviço de saúde; nada registrado; estado do pedido inalterado; `GET …/submissoes-simuladas` → 404 |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T13-04 — Submissão com análise antiga, sem análise ou em andamento
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-13, RC-17 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco. Análise "em andamento" não é reproduzível à mão (análise síncrona) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `RecusasDaSubmissaoTest`; manual: rascunho sem análise e T11-03 |
| Passos | 1. A partir de `backend/`: `.\mvnw.cmd -Dtest=RecusasDaSubmissaoTest test`. 2. Manual: submeter um rascunho nunca analisado |
| Resultado esperado | Os testes passam (análise com pendência, não verificável, sem análise vigente, falha de análise, análise sem pendências que perdeu a vigência, análise em andamento): **409** `CONFLITO_ESTADO` e nada registrado. Manual: rascunho nunca analisado → 409 |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T13-05 — Submeter pedido de outra paciente
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-17 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido analisado da conta B; token da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `POST /solicitacoes/<idB>/submissoes-simuladas` como A |
| Passos | 1. Submeter como A. 2. Conferir como B que nada foi registrado |
| Resultado esperado | **404** `NAO_ENCONTRADO` idêntico ao de inexistente; nada registrado para B |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T13-06 — Pedido submetido é só consulta
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-09, RC-17 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido submetido (T13-01) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | tentativas de alterar o pedido submetido |
| Passos | 1. `POST …/documentos` (arquivo válido). 2. `PUT` e `DELETE` de documento. 3. `PUT /solicitacoes/<id>`. 4. `POST …/analises` |
| Resultado esperado | **409** `CONFLITO_ESTADO` em todos ("Este pedido já foi submetido e só pode ser consultado."/"Este pedido não pode mais ser alterado."); `GET` continua funcionando; nada mudou |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T13-07 — Concorrência na conclusão
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-17 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | Opt-in: `DB_*` exportadas e `RC_TESTE_ORACLE=SIM`; scripts até o `10_` aplicados |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `SubmissaoOracleTest` |
| Passos | 1. Seguir o comando de `docs/ambiente.md`: `(set -a; . ./.env; set +a; cd backend && RC_TESTE_ORACLE=SIM sh ./mvnw test -Dtest=SubmissaoOracleTest)` |
| Resultado esperado | Passa: quatro conclusões simultâneas geram **uma** submissão; repetição e consulta devolvem o mesmo comprovante; a submissão da carga (`SIM-2026-0001`) não é alterada. Sem a variável, o teste fica ignorado e o caso segue "Não executado" |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T14 — Exclusão permitida e bloqueada (RC-09)

A exclusão de rascunho exige o cabeçalho `X-Confirmar-Exclusao: EXCLUIR`.

#### T14-01 — Excluir o próprio rascunho
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-09 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A com 1 documento (PB-2, PB-3); `DOCUMENT_STORAGE_PATH` definido |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `DELETE /solicitacoes/<id>` com `X-Confirmar-Exclusao: EXCLUIR` |
| Passos | 1. Excluir. 2. `GET /solicitacoes/<id>`. 3. `GET /solicitacoes` |
| Resultado esperado | **204** sem corpo; `GET` do pedido → 404; sai da listagem; documentos, revisões e arquivos guardados são apagados |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T14-02 — Excluir sem a confirmação
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-09 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `DELETE` sem o cabeçalho, e com `X-Confirmar-Exclusao: SIM` |
| Passos | 1. Chamar as duas variações. 2. `GET /solicitacoes/<id>` |
| Resultado esperado | **400** `REQUISICAO_INVALIDA` com `detalhes[].campo` = `confirmacao`; nada apagado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T14-03 — Excluir pedido submetido
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-09 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido submetido da conta A (T13-01) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `DELETE` com `X-Confirmar-Exclusao: EXCLUIR` |
| Passos | 1. Excluir. 2. `GET /solicitacoes/<id>` e `GET …/submissoes-simuladas` |
| Resultado esperado | **409** `CONFLITO_ESTADO` (a mensagem pode ser "Este pedido já foi submetido e só pode ser consultado." ou "Somente rascunhos podem ser excluídos."; registrar a observada); pedido, documentos, análise e comprovante intactos |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T14-04 — Excluir pedido que não é rascunho
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-09 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido da conta A analisado `COM_PENDENCIAS` (T08-02) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `DELETE` com `X-Confirmar-Exclusao: EXCLUIR` |
| Passos | 1. Excluir. 2. Conferir o pedido |
| Resultado esperado | **409** `CONFLITO_ESTADO`, "Somente rascunhos podem ser excluídos."; nada apagado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T14-05 — Excluir pedido de outra paciente
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-09 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta B; token da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `DELETE /solicitacoes/<rascB>` como A com a confirmação correta |
| Passos | 1. Excluir como A. 2. Conferir como B |
| Resultado esperado | **404** `NAO_ENCONTRADO` idêntico ao de inexistente; o rascunho de B segue intacto |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T14-06 — Remover um documento do rascunho
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-09 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A com 2 documentos |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `DELETE /solicitacoes/<id>/documentos/<docId>` |
| Passos | 1. Remover. 2. `GET …/documentos` e download do removido. 3. Remover de novo |
| Resultado esperado | **200** com `versaoConjuntoDocumental` +1 e `documentosDisponiveis` 1; o documento some da lista e do download (404); o arquivo é apagado; a vaga é liberada (novo envio aceito); repetir a remoção → 404 sem mudar versão nem contagem |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T15 — Conectividade e sessão (RC-04, RC-08, RC-13, RC-19)

#### T15-01 — Repetir um envio interrompido não duplica
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `samples/encaminhamento-sintetico.pdf` com `X-Chave-Envio: caso-t15-01` |
| Passos | 1. PB-3 com a chave. 2. Repetir **a mesma** chamada duas vezes. 3. `GET …/documentos` |
| Resultado esperado | 1ª: **201**; repetições: **200** com o **mesmo** documento; a lista tem **1** documento e o pedido ganhou **uma** revisão |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-02 — Mesma chave com outro conteúdo
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-08 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho com o envio de T15-01 |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `samples/identificacao-sintetica.png` com a mesma chave `caso-t15-01` |
| Passos | 1. PB-3 com a chave reutilizada. 2. `GET …/documentos` |
| Resultado esperado | **409** `CHAVE_ENVIO_REUTILIZADA`; nada criado nem alterado |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-03 — Sessão expirada na API
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | API reiniciada com `SESSION_TTL_MINUTES=1` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | token da conta A |
| Passos | 1. PB-1. 2. `GET /auth/sessao` (200). 3. Esperar mais de 1 min. 4. `GET /auth/sessao` e `GET /solicitacoes` |
| Resultado esperado | Passo 2: 200; passo 4: **401** `NAO_AUTENTICADO` ("Sessão ausente ou expirada. Entre novamente.") com a mesma resposta de token inválido; entrar de novo funciona |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-04 — Banco indisponível
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-04, RC-08 |
| Regra | não se aplica |
| Tipo de execução | API em memória (sem banco) |
| Pré-condições | API iniciada **sem** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `GET /health`, `POST /auth/login` e `GET /solicitacoes` |
| Passos | 1. Chamar as três rotas. 2. Opcional: `.\mvnw.cmd -Dtest=AuthEndpointLoginTest#semBancoConfiguradoOLoginDevolve503ENenhumaSessaoMasOHealthFunciona test` |
| Resultado esperado | `/health` 200; login e demais rotas **503** `DEPENDENCIA_INDISPONIVEL` sem detalhe técnico (sem SQL, caminho ou stack) e sem sessão aberta |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-05 — Pedir a análise duas vezes não duplica
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-13 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Pedido do conjunto 01 lido e conferido (PB-1 a PB-5), sem análise |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `POST …/analises` duas vezes seguidas |
| Passos | 1. PB-6. 2. PB-6 de novo. 3. `GET …/analises` |
| Resultado esperado | 1ª: **201**; 2ª: **200** com a mesma vigente; a lista tem **uma** análise (nada mudou entre as chamadas) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-06 — App: sem conexão ao abrir um pedido
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-18, RC-19 |
| Regra | não se aplica |
| Tipo de execução | App no emulador + API real |
| Pré-condições | App instalado; paciente A logada; lista de pedidos carregada |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | corte de rede do emulador |
| Passos | 1. `adb shell svc wifi disable` e `adb shell svc data disable`. 2. Tocar num pedido. 3. Religar a rede (`svc wifi enable`, `svc data enable`) e tocar em "Tentar de novo" |
| Resultado esperado | Passo 2: "Sem conexão com a internet." com "Tentar de novo"; o app continua aberto; passo 3: o pedido carrega |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-07 — App: sessão expirada
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-04, RC-18 |
| Regra | não se aplica |
| Tipo de execução | App no emulador + API real |
| Pré-condições | API com `SESSION_TTL_MINUTES=1`; app logado |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | espera de mais de 1 min |
| Passos | 1. Entrar no app. 2. Esperar mais de 1 min. 3. Tocar num pedido |
| Resultado esperado | O app volta ao login com "Sua sessão terminou. Entre novamente." (e "Por segurança, entre de novo para continuar."); nenhum dado sensível no logcat |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-08 — App: envio interrompido por falta de rede e retentativa sem duplicar
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-08, RC-19 |
| Regra | não se aplica |
| Tipo de execução | App no emulador + API real |
| Pré-condições | App logado; rascunho aberto; `DOCUMENT_STORAGE_PATH` na API; amostra copiada para o emulador (`adb push samples/encaminhamento-sintetico.pdf /sdcard/Download/`; no Git Bash use `MSYS_NO_PATHCONV=1`) |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `encaminhamento-sintetico.pdf` |
| Passos | 1. Cortar a rede do emulador. 2. Enviar o arquivo pelo app. 3. Religar a rede. 4. Tocar **duas vezes** em "Tentar de novo". 5. `GET /solicitacoes/<id>/documentos` pela API |
| Resultado esperado | Passo 2: "Não enviado" com orientação e "Tentar de novo"; passo 4: "Enviado"; a API mostra **1** documento (sem duplicata) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T15-09 — App: queda durante a análise não presume resultado
| Campo | Conteúdo |
|---|---|
| Grupo | falha |
| Histórias | RC-13, RC-19 |
| Regra | não se aplica |
| Tipo de execução | App no emulador + API real |
| Pré-condições | App logado; pedido com documentos lidos e conferidos |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | corte de rede antes de confirmar a verificação |
| Passos | 1. Tocar em "Verificar critérios" e cortar a rede imediatamente. 2. Religar a rede e tocar em "Consultar de novo". 3. Tocar em "Tentar novamente" apenas se o histórico mostrar que nada foi gravado |
| Resultado esperado | "Não foi possível confirmar a operação…" com "Consultar de novo"; **nenhum resultado é presumido**; o histórico é conferido antes de repetir; ao final o pedido tem **uma** análise para a versão atual. (Janela curta: se a análise concluir antes do corte, registrar como não reproduzido, não como aprovado) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

### T16 — Sensores simulados (RC-21, RC-22)

Backend (T16-01 a T16-05, T16-07 a T16-09) é testável hoje. **Alertas na interface (T16-06) são RC-22 pendente**; um grupo não aprova o outro. Preparação: a paciente abre a sessão (`POST /solicitacoes/<id>/sessoes-captura` → guardar `id` da sessão) e a API/simulador usam a **mesma** `IOT_API_KEY`. Consulta: `GET /solicitacoes/<id>/leituras`.

#### T16-01 — Sequência normal
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-21, RC-22 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Sessão de captura aberta; `IOT_API_KEY` igual nos dois lados |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `java simulador-iot/SimuladorSensores.java --sessao <N> --sequencia normal` |
| Passos | 1. Rodar o simulador. 2. `GET …/leituras` como a paciente |
| Resultado esperado | 5 pares (luminosidade e distância) enviados, todos `-> 201`, saída do simulador com código 0; telemetria com `simulada` true; último par (550 lux, 35 cm): ambos `DENTRO_DA_FAIXA` com `orientacao.codigo` `CONDICAO_ADEQUADA`; faixas 300–1000 lux e 20–40 cm |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-02 — Fora da faixa
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-21, RC-22 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Sessão de captura aberta |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `java simulador-iot/SimuladorSensores.java --sessao <N> --sequencia fora-da-faixa` |
| Passos | 1. Rodar o simulador. 2. `GET …/leituras` |
| Resultado esperado | Leituras aceitas (201); último par (110 lux, 58 cm): luminosidade `ABAIXO_DA_FAIXA` com `MELHORAR_ILUMINACAO`; distância `ACIMA_DA_FAIXA` com `APROXIMAR_DOCUMENTO`; valor fora da faixa **não** é erro |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-03 — Sequência de recuperação
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-21, RC-22 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Sessão de captura aberta |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `java simulador-iot/SimuladorSensores.java --sessao <N> --sequencia recuperacao --intervalo-ms 3000` |
| Passos | 1. Rodar o simulador; durante a execução consultar `GET …/leituras` ao menos duas vezes (início e fim). 2. Consultar no fim |
| Resultado esperado | Início com orientação de correção (par 100 lux/60 cm: `MELHORAR_ILUMINACAO` e `APROXIMAR_DOCUMENTO`); ao fim (650 lux, 28 cm) ambos `DENTRO_DA_FAIXA` com `CONDICAO_ADEQUADA` |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-04 — Leitura desatualizada
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-21, RC-22 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Leituras de T16-01 ou T16-02 enviadas |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | espera de mais de 60 s sem enviar |
| Passos | 1. Esperar mais de 60 s. 2. `GET …/leituras` |
| Resultado esperado | Os dois sensores com `situacao` `DESATUALIZADA` e `orientacao.codigo` `VERIFICAR_ESTACAO` (sem orientação de correção); `prazoAtualidadeSegundos` 60 |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-05 — Envio sem credencial ou com chave errada
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-21 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Sessão de captura aberta; contagem de leituras anotada |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `POST /sessoes-captura/<N>/leituras` com `{"sessaoId":<N>,"tipo":"LUMINOSIDADE","valor":350,"unidade":"LUX","medidaEm":"<agora em UTC>"}` |
| Passos | 1. Enviar sem `X-Chave-Simulador`. 2. Enviar com chave errada. 3. Rodar o simulador **sem** `IOT_API_KEY` definida. 4. Conferir `GET …/leituras` |
| Resultado esperado | Passos 1 e 2: **401** `NAO_AUTENTICADO`; passo 3: o simulador termina com código **2** (chave ausente) sem enviar nada; nenhuma leitura gravada (a telemetria não muda). Em API subida sem `IOT_API_KEY`, todo envio é 401 |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-06 — Alertas da estação na interface do app
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-22 |
| Regra | não se aplica |
| Tipo de execução | App no emulador + API real |
| Pré-condições | App com a tela da estação de captura; simulador enviando leituras |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | sequências `normal`, `fora-da-faixa` e espera de 60 s |
| Passos | 1. Abrir a tela da estação no app. 2. Rodar cada sequência. 3. Esperar 60 s sem enviar |
| Resultado esperado | O app mostra o alerta correspondente a cada `orientacao.codigo` (texto e ícone, não só cor) e a indicação de leitura desatualizada; a captura segue sem sensor se não houver leitura |
| Impedimento conhecido | **RC-22 (interface) aberta**: o app não consome `GET /solicitacoes/{id}/leituras` e não tem a tela da estação. Só a RC-25, ao tentar executar, muda o caso para "Bloqueado" com esse motivo |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-07 — Chave do simulador não vale fora da telemetria; conta não envia telemetria
| Campo | Conteúdo |
|---|---|
| Grupo | segurança |
| Histórias | RC-05, RC-21 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | `IOT_API_KEY` conhecida pela pessoa que executa; sessão aberta; token da conta A |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | chave do simulador em rotas de paciente; token da paciente na telemetria |
| Passos | 1. `GET /solicitacoes` com `X-Chave-Simulador: <chave>` e sem token. 2. `GET /solicitacoes/<id>/documentos` com a chave. 3. `POST /sessoes-captura/<N>/leituras` com o **token da paciente** (sem chave) |
| Resultado esperado | Passos 1 e 2: **403** `ACESSO_NEGADO` (a chave só vale na telemetria); passo 3: **403** `ACESSO_NEGADO` (conta autenticada não envia telemetria); nenhuma leitura gravada |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-08 — Sensor indisponível não bloqueia envio de documento
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-21 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | Rascunho com sessão de captura aberta **sem** nenhuma leitura |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `samples/encaminhamento-sintetico.pdf` |
| Passos | 1. `GET …/leituras`. 2. PB-3 com o PDF. 3. Excluir o rascunho (T14-01) |
| Resultado esperado | Passo 1: 200 com os dois sensores `SEM_LEITURA` e `orientacao.codigo` `CAPTURA_SEGUE_SEM_SENSOR`; passos 2 e 3: **201** e **204** — o salvamento, o envio e a exclusão **não** dependem dos sensores |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

#### T16-09 — Simulador reproduzível (`--seco`) e testes automatizados do backend
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-21 |
| Regra | não se aplica |
| Tipo de execução | Teste automatizado |
| Pré-condições | JDK 21; sem banco. Se `JAVA_TOOL_OPTIONS` estiver exportada (contorno do loopback), o teste `SimuladorSensoresProcessoTest` pode falhar por contar a linha "Picked up JAVA_TOOL_OPTIONS" (`docs/ambiente.md`); registrar isso em vez de aprovar ou reprovar às cegas |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | `java simulador-iot/SimuladorSensores.java --sessao 1 --sequencia normal --seco` e `EnviarLeituraEndpointTest`, `ConsultarTelemetriaEndpointTest`, `ClassificadorDeLeituraTest` |
| Passos | 1. Rodar o comando `--seco` duas vezes. 2. A partir de `backend/`: `.\mvnw.cmd -Dtest=EnviarLeituraEndpointTest,ConsultarTelemetriaEndpointTest,ClassificadorDeLeituraTest test` |
| Resultado esperado | O `--seco` imprime, nas duas vezes, as mesmas 10 linhas (`LUMINOSIDADE 450 LUX`, `DISTANCIA 30 CM`, … na ordem da tabela da sequência `normal`), sem conectar e sem chave; os testes passam (inclusive recusa de leitura fora do limite físico com 400 e sessão inexistente com 404 antes do corpo) |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

---

## 8. Percurso integrado P01 — paciente do cadastro à submissão simulada

Leitura e resumo **simulados** em todo o caminho. Não substitui os casos individuais: cada etapa remete ao caso que a verifica. Conta A; conjuntos 09 e 14.

### P01 — Login → pedido → upload → leitura → conferência → análise → resumo → correção → reanálise → submissão
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento |
| Histórias | RC-04, RC-07, RC-08, RC-10, RC-11, RC-12, RC-13, RC-14, RC-15, RC-16, RC-17 |
| Regra | RC-R06 PENDENTE → ATENDE |
| Tipo de execução | API real + Oracle (opcionalmente repetido no app) |
| Pré-condições | Ambiente da seção 5; conta A; conjuntos 09 e 14 |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | [Conjunto 09](../../samples/conjuntos/09-menor-sem-autorizacao/gabarito.md) e correção do [conjunto 14](../../samples/conjuntos/14-corrigido-de-09/gabarito.md) |
| Passos | Etapa · caso de referência · estado esperado do pedido ao fim: **1.** Entrar (T01-01) · ainda não há pedido. **2.** Criar o pedido (T04-01) · `RASCUNHO`, `revisao` 1. **3.** Enviar `encaminhamento.pdf` e `identificacao.png` do 09 (T05-01, T05-02) · `RASCUNHO`, 2 documentos. **4.** Ler os documentos, **simulado** (T05-04) · sem mudar o estado. **5.** Conferir e confirmar os campos (T07-03 contrasta com "nada conferido") · conferência `CONFERIDA`. **6.** Analisar (T09-02) · `COM_PENDENCIAS`, RC-R06 `PENDENTE`. **7.** Ler o resumo, **simulado** (T07-02, T12-01) · mesmo estado. **8.** Corrigir: substituir o encaminhamento pelo do 14 (T11-02) · nova revisão, análise anterior sem vigência. **9.** Reler, conferir e reanalisar (T11-02, T11-04) · `SEM_PENDENCIAS_IDENTIFICADAS`, `comparacao` RC-R06 `RESOLVIDA`. **10.** Submeter (T13-01) · `SUBMETIDA_SIMULACAO`. **11.** Repetir a submissão (T13-02) · mesmo comprovante |
| Resultado esperado | Cada etapa dá o resultado do caso de referência; estado final `SUBMETIDA_SIMULACAO` com comprovante "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS"; nenhuma etapa chama IA ou leitor real; "sem pendências" não é aprovação do SUS |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |

## 9. Matriz de rastreabilidade

### 9.1 História → casos

| História | Casos | Observação |
|---|---|---|
| RC-01 (protocolo/regras) | T09-01 a T09-04 | RC-R06 condicional |
| RC-04 (login) | T01-01 a T01-08, T15-03, T15-04, T15-07, P01 | |
| RC-05 (perfis e isolamento) | T02-01 a T02-06, T03-03, T03-04, T13-05, T14-05, T16-07, T01-08 | T03-01 e T03-02 dependem da RC-20 |
| RC-06 (proteção de arquivos e dados) | T02-02 a T02-06, T06-01 a T06-08, T12-01 a T12-03 | |
| RC-07 (rascunho) | T04-01 a T04-05, P01 | |
| RC-08 (upload) | T05-01 a T05-03, T06-*, T15-01, T15-02, T15-08 | |
| RC-09 (alterar/excluir) | T11-01, T11-02, T13-06, T14-01 a T14-06 | |
| RC-10 (leitura simulada) | T05-04 a T05-06, T06-03, T10-01 a T10-03, T12-04 | |
| RC-11 (conferência) | T07-01, T07-03, T10-01, T10-04, T10-05 | |
| RC-12 (motor de regras) | T07-*, T08-*, T09-* | |
| RC-13 (análise e histórico) | T07-01, T08-*, T11-*, T12-05, T13-*, T15-05, T15-09 | |
| RC-14 (resumo simulado) | T07-02, T08-01 a T08-03, T12-01 a T12-05 | |
| RC-15 (orientação ao resultado) | T07-*, T08-*, T10-* | |
| RC-16 (reenvio) | T11-01 a T11-05, P01 | |
| RC-17 (submissão simulada) | T13-01 a T13-07, P01 | |
| RC-18 / RC-19 (app) | T01-07, T01-08, T15-06 a T15-09 | |
| RC-20 (indicadores) | T03-01, T03-02 | **Lacuna:** rota inexistente, P1 |
| RC-21 (sensores, backend) | T16-01 a T16-05, T16-07 a T16-09 | |
| RC-22 (alertas na interface) | T16-06 | **Lacuna:** interface pendente |

### 9.2 Regra e resultado → casos

| Regra | atende | pendente | não verificável | não aplicável |
|---|---|---|---|---|
| RC-R01 | T07-01 | — (nenhum conjunto da RC-23 a deixa pendente) | T07-03 (conjunto 15) | — |
| RC-R02 | T07-01 | T10-03 | T08-01, T08-03, T10-01, T10-02, T10-04 | — |
| RC-R03 | T07-01 | T08-01, T08-03, T10-02 | T10-01, T07-03 | — |
| RC-R04 | T07-01 | T08-02, T08-03 | T07-03 | — |
| RC-R05 | T07-01 | T08-03 | T07-03 | — |
| RC-R06 | T09-01 | T09-02 | T09-04, T07-03, T08-01 | T07-01, T09-03 |

Lacuna da matriz: não existe conjunto sintético com RC-R01 `PENDENTE`; a regra só aparece como `ATENDE` ou `NAO_VERIFICAVEL`. Não foi criado gabarito novo.

### 9.3 Conjunto RC-23 → casos

| Conjunto | Casos |
|---|---|
| 01 | T05-04, T07-01, T07-02, T13-01, T10-04, T10-05 |
| 02 | T08-01 |
| 03 | T08-02, T13-03 |
| 04 | T08-03, T11-01 |
| 05 | T10-01, T09-04 |
| 06 | T10-02 |
| 07 | T10-03 |
| 08 | T09-01, T06-07 |
| 09 | T09-02, T11-02, P01 |
| 10 | T09-03 |
| 11 | T12-03 |
| 12 | T06-01, T06-02, T02-05 |
| 13 | T11-01 |
| 14 | T11-02, T11-04, P01 |
| 15 | T07-03 |

### 9.4 Tipo de execução e impedimentos

| Tipo | Casos |
|---|---|
| API real + Oracle | maioria dos casos de T01 a T16 e P01 |
| API em memória (sem banco) | T15-04 |
| Teste automatizado | T01-06, T02-06, T04-04, T05-03, T06-08, T11-05, T13-04, T13-07, T16-09 |
| Dublê (não é integração real) | T12-01, T12-02, T12-04, T12-05 |
| App no emulador + API real | T01-07, T01-08, T15-06 a T15-09, T16-06 |

| Impedimento conhecido | Casos |
|---|---|
| RC-20 (P1, rotas de indicadores inexistentes) | T03-01, T03-02 |
| RC-22 (interface de alertas) | T16-06 |
| Sem amostra JPEG ou PDF protegido em `samples/` | T05-03 (manual), T06-03 |

### 9.5 Lacunas declaradas

1. **Provedor de IA real:** não existe; só adaptador simulado e dublê. Nenhum caso afirma "IA real".
2. **JPEG por HTTP:** sem amostra sintética em `samples/`; só o teste automatizado T05-03.
3. **PDF protegido:** sem amostra versionada; a RC-25 precisa criá-lo (T06-03).
4. **Troca para serviço diferente:** só há um serviço ativo; coberto só por teste automatizado (T04-04).
5. **Edição durante análise em andamento:** só por teste de concorrência (T11-05).
6. **RC-R01 `PENDENTE`:** nenhum conjunto da RC-23 a produz.
7. **Divergência apontada pela paciente:** sem gabarito RC-23; esperado derivado do contrato (T10-04).
8. **Indicadores do administrador e alertas na interface:** funcionalidades pendentes (seção 3.2).

## 10. Pendências do plano

| Pendência | Responsável | Situação |
|---|---|---|
| Revisão do plano por outro integrante da equipe | equipe | **Pendente** |
| Preencher commit do plano e versão testável | RC-25 | Pendente |
| Executar os casos, guardar evidências em `docs/evidencias/<ID>/` e preencher resultado, status, responsável e data | RC-25 | Não iniciado |
| Gerar PDF protegido sintético (T06-03) | RC-25 | Pendente |
| Definir caminho real da rota de indicadores quando a RC-20 existir (T03-01 e T03-02) | RC-20 / RC-25 | Pendente |
| Mensagens das três telas do app (`Textos.kt`) e o texto literal da issue | RC-25 | Observar durante T01-07 e T15-06 a T15-09 |
