# Research: Plano de testes do demonstrador (RC-24)

Levantamento feito por leitura do repositório em 18/11/2026 (nada foi executado). Onde o fato ainda precisa de conferência no momento de escrever o caso, está marcado **conferir na T-xxx**.

## Decisões

### D1 — Um arquivo, IDs `Txx-yy`, um caso por resultado distinto
**Decisão**: `docs/testes/plano-de-testes.md` com os 16 cenários da issue e casos `Txx-yy`. **Racional**: a issue exige variações com resultado distinto em casos separados e o entregável nomeado. **Alternativas**: um arquivo por cenário (descartado: dificulta a visão geral e a matriz).

### D2 — Status inicial único; impedimento conhecido não é status
**Decisão**: todo caso nasce "Não executado". Funcionalidade pendente vira uma anotação **Impedimento conhecido** no caso e na matriz; só a RC-25 muda para "Bloqueado", depois de tentar e registrar. **Racional**: ajuste do responsável (18/11/2026) e Princípio VII. **Alternativa**: marcar "Bloqueado" já no plano (descartada: afirmaria uma execução que não houve).

### D3 — Decisão de escopo aprovada × funcionalidade pendente
O plano tem uma tabela que separa:

| Tipo | Itens |
|---|---|
| **Decisão de escopo aprovada** | Extração/OCR e resumo de IA **simulados** (decisão de 12/11/2026, CLAUDE.md §4 e §11, constituição); sensores por script; submissão **simulada**; protocolo acadêmico fictício |
| **Funcionalidade pendente** | RC-20 (indicadores do administrador, **P1**, rotas inexistentes; a issue #43 já a classifica como P1, então não houve reclassificação a registrar além dessa constatação); RC-22 (alertas na interface, aberta); app RC-18 (#71) e RC-19 (#74) **já mergeados** (sem impedimento por PR); RC-33, RC-34, RC-35 (P1) |

**Consequência**: casos de extração e resumo dizem "simulado" no título ou no ambiente; nenhum diz "extração real" ou "resumo real por IA", ainda que a issue use esses termos (registrado como diferença da issue).

### D4 — Resultado esperado vem do contrato real
Fontes: `docs/api/erros.md`, `contratos-dados.md`, `autorizacao.md`, `estados-e-revisoes.md`, `protecoes.md`, `openapi.yaml` e gabaritos de `samples/conjuntos/*/gabarito.md`. Quando a issue e o contrato diferirem, vale o contrato e a diferença é anotada no caso.

### D5 — Separar o que cada caso prova (sem aprovação parcial)
Casos compostos são divididos: por exemplo, T16 em **backend** (T16-01..05) e **interface** (T16-06, depende da RC-22); T15 em casos de API e de app; T03 em paciente e administrador. Cada caso tem um único critério de aprovação; "Aprovado" exige **todos** os itens do resultado esperado daquele caso.

### D6 — Dublês identificados
Casos de T12 (provedor de IA indisponível, saída fora do contrato) e os de queda de conexão com dublê usam teste automatizado com dublê ou corte de rede no emulador. O campo **Tipo de execução** de cada caso é um de: `API real + Oracle`, `API em memória (sem banco)`, `Teste automatizado`, `Dublê (não é integração real)`, `App no emulador + API real`. Dublê nunca é apresentado como integração real.

## Levantamento por cenário (contrato real)

| Cenário | O que o repositório oferece | Observações para o caso |
|---|---|---|
| **T01** Autenticação (RC-04) | `POST /auth/login`; 401 `CREDENCIAIS_INVALIDAS` com resposta idêntica para senha errada, e-mail inexistente, conta desativada (erros.md:25); contas `paciente.a/b@exemplo.invalid`, `admin@exemplo.invalid` (database/scripts/README.md) | Exige Oracle ou repositório em memória dos testes; resposta não pode trazer senha, hash ou detalhe interno |
| **T02** Isolamento (RC-05, RC-06) | Pedido, documento e análise alheios respondem o **mesmo 404** de inexistente (contratos-dados.md; `MatrizPacienteTest`); download `GET /solicitacoes/{id}/documentos/{docId}` | A issue diz "negadas conforme o contrato": o contrato escolhe 404, não 403 (registrar) |
| **T03** Permissões (RC-05, RC-20) | Rotas de indicadores **não existem**; só `/protocolos` (paciente). Autorização: administrador vê só agregados (autorizacao.md:9) | **Impedimento conhecido** (RC-20, P1). Expectativa de 403 da issue só vale quando a rota existir; antes disso a resposta seria 404 |
| **T04** Rascunho (RC-07) | `POST /solicitacoes`; troca de serviço (`trocarProtocolo`, com motivo `PROTOCOLO_ALTERADO`) | Teste da **troca de serviço**, único campo editável; campo obrigatório vazio: **conferir na T-xxx** se o contrato responde 400 ou 422 |
| **T05** Upload e leitura (RC-08, RC-10) | `POST …/documentos` 201/200; leitura simulada `POST …/extracoes`; amostras em `samples/` (PDF, PNG) e conjuntos | Leitura **simulada** identificada por hash; **conferir na T-xxx** se existe amostra JPEG válida (a RC-23 usa PDF/PNG/TXT) e, se não, registrar lacuna do JPEG em vez de inventar arquivo |
| **T06** Não aceitos (RC-06, RC-08, RC-10) | Mais de 5 páginas, **PDF protegido por senha**, vazio, corrompido, imagem grande e mais de 3 arquivos → `400 REQUISICAO_INVALIDA` (erros.md:62); tipo não suportado → 415 `TIPO_NAO_SUPORTADO`/`EXTENSAO_INCOMPATIVEL`; conjunto `12-formato-invalido` | **PDF protegido: HTTP 400 `REQUISICAO_INVALIDA`**. Acima do limite de tamanho: **conferir na T-xxx** entre 400 e 413. Arquivo recusado não aparece na lista nem entra na análise |
| **T07** Completo (RC-11, RC-12, RC-14, RC-15) | Conjunto `01-completo`; resultado `SEM_PENDENCIAS_IDENTIFICADAS`; resumo simulado | Exige conferência confirmada (só campo confirmado é evidência); "não é aprovação do SUS" |
| **T08** Ausências (RC-12, RC-14, RC-15) | Conjuntos `02`, `03` (uma pendência), `04` (várias) | Orientação diferencia paciente × unidade emissora (campo `responsavel` e `orientacao`, RC-15) |
| **T09** Condicionais (RC-01, RC-12) | RC-R06 (maioridade/autorização): conjuntos `08` (aplicável, atendida), `09` (aplicável, pendente), `10` (não aplicável: 18 anos exatos), `15` (nada conferido → não verificável) | Data de referência 2026-11-12 só na regressão automática; em execução manual vale o dia do envio (gabarito) — o caso 10 depende disso |
| **T10** Leitura não confiável (RC-10, RC-11, RC-15) | Conjuntos `05` (ilegível), `06` (cortada), `07` (outra pessoa); conferência `PUT` com decisões; `DIVERGENCIA` marcada como reservada no gabarito | **Conferir na T-xxx** se divergência apontada pela paciente existe como caso executável pelo contrato; senão, lacuna declarada |
| **T11** Reenvio (RC-09, RC-13, RC-16) | Conjuntos `13` e `14` (substituição); histórico por revisão; `AnaliseOracleTest`, `ReenvioOracleTest` (concorrência real, opt-in) | "Alterar durante análise em processamento" só é reproduzível de forma controlada pelo teste de concorrência; manual = limitação declarada |
| **T12** IA (RC-06, RC-14) | Adaptador de resumo **simulado** (`TextoSimulado`); `ResumoEndpointTest` com falha/saída inválida; conjunto `11-instrucao-no-documento` | Falha de provedor e saída inválida: **só com dublê**; "provedor real" não existe e não é testado |
| **T13** Submissão (RC-13, RC-17) | `POST /solicitacoes/{id}/submissoes-simuladas`; 200 na repetição; 409 `CONFLITO_ESTADO`; `SubmissaoOracleTest` | Comprovante com "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" |
| **T14** Exclusão (RC-09) | `DELETE /solicitacoes/{id}` rascunho próprio; pedido submetido → conflito | Código exato do conflito: **conferir na T-xxx** |
| **T15** Conexão e sessão (RC-04, RC-08, RC-13, RC-19) | `SESSION_TTL_MINUTES=1` (roteiro RC-18); corte de rede do emulador (`svc wifi/data disable`, evidência RC-19); chave `X-Chave-Envio` evita duplicação | App já mergeado (#71, #74): casos de app exigem emulador e SDK, sem impedimento por PR; idempotência da API é testável sem app |
| **T16** Sensores (RC-21, RC-22) | `POST /sessoes-captura/{id}/leituras` com `X-Chave-Simulador`; faixas 300–1000 lux e 20–40 cm; leitura > 60 s `DESATUALIZADA`; `--seco`; sem chave → 401; chave em outra rota → 403 | **Backend** testável hoje; **alertas na interface** = RC-22 (aberta): impedimento conhecido |
| **P01** Percurso integrado | Login → pedido → upload → leitura simulada → conferência → análise → resumo simulado → substituição → reanálise → submissão | Reutiliza casos; todo o caminho com leitura/resumo **simulados** |

## Pontos a conferir ao escrever (viram tarefas)

1. Contrato de campo obrigatório vazio no rascunho (400 × 422) — T04-03.
2. Existência de amostra JPEG; senão, lacuna — T05-02.
3. Resposta para arquivo acima de 5 MB (400 × 413) — T06-01.
4. Caso executável de divergência na conferência — T10-02.
5. Código do conflito ao excluir pedido submetido — T14-02.
6. Nomes exatos dos testes automatizados com dublê (resumo, conexão) — T12 e T15.
7. Como restaurar dados entre execuções: pedidos criados por casos são do próprio caso; a carga `02_carga_sintetica.sql` não deve ser reaplicada sem limpeza; `90_limpeza_DESTRUTIVA.sql` só com autorização específica (CLAUDE.md §2.9).

## Itens fora do plano

Execução, resultados, evidências, retestes (RC-25); avaliação com pessoas (RC-26); medição (RC-27); automação de regressão (RC-33, P1); download de comprovante (RC-34, P1).

## Pontos resolvidos na implementação (T004, 18/11/2026, por leitura)

1. **T04, campo obrigatório**: `protocoloId` ausente, nulo, em branco, não textual ou com mais de 100 caracteres → **400** `REQUISICAO_INVALIDA` (`detalhes[].campo = protocoloId`); serviço inexistente/inativo → **422** `REGRA_DE_NEGOCIO` (docs/api/contratos-dados.md). Troca para serviço *diferente* só por teste automatizado (um serviço ativo).
2. **JPEG**: `samples/` não tem amostra JPEG. Cobertura: `EnviarDocumentoEndpointTest#jpegEPngTambemSaoAceitos` (JPEG gerado em código). Manual = lacuna declarada (T05-03).
3. **> 5 MB**: **413** `ARQUIVO_MUITO_GRANDE` (docs/api/erros.md, protecoes.md); imagem com pixels demais é 400. PDF protegido: 400 `REQUISICAO_INVALIDA`; sem amostra versionada (T06-03).
4. **Divergência**: existe pelo contrato (`PUT …/conferencia` com `DIVERGENTE`); o gabarito RC-23 a trata como reservada, então T10-04 deriva o esperado do contrato e diz isso.
5. **Excluir pedido submetido**: **409** `CONFLITO_ESTADO`; a mensagem pode ser "Este pedido já foi submetido e só pode ser consultado." ou "Somente rascunhos podem ser excluídos." (T14-03 registra a observada). A exclusão exige `X-Confirmar-Exclusao: EXCLUIR` (sem ele, 400 `confirmacao`).
6. **Testes com dublê**: `ResumoEndpointTest#saidaInvalidaEFalhaUsamPadraoEPermitemNovaTentativa`, `LeituraMalSucedidaTest#falhaDoLeitorDa503SemDetalheENadaEGravado`, `PrazoEFalhaAnaliseTest`. Idempotência: `IdempotenciaEnvioTest`. App: testes JUnit + MockWebServer em `mobile/app/src/test` (não usados como caso).
7. **Restauração**: ver seção 5.5 do plano.

**Correção de fato (18/11/2026):** os PRs #71 (RC-18) e #74 (RC-19) já estão mergeados na história da branch (`4e7a3f6`, `8ea60f8`); o app não tem impedimento por PR aberto. **T03 admin**: administrador recebe **403** `ACESSO_NEGADO` em recurso individual de paciente, inclusive para id inexistente (testável hoje). **Fonte dos cenários: issue #50 (lida em 18/11/2026 pelo `gh`).** O CONTRATO_TESTES.md (raiz) é só o resumo dos mesmos T01–T16; o caminho `/admin/indicadores` vem dele, não da issue.
