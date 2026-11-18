# Contratos de dados: solicitação, upload, análise e pendência

Formato JSON, campos em camelCase, instantes em ISO-8601 UTC. Todos os exemplos são fictícios. A paciente é identificada pela sessão autenticada, nunca por campo no corpo. Rotas completas em [`CONTRATO_API.md`](../../CONTRATO_API.md); além de `GET /health`, estão implementadas as rotas `/auth/*` (RC-04), `GET /protocolos` e o rascunho da solicitação (`GET/POST /solicitacoes`, `GET/PUT /solicitacoes/{id}`, RC-07); as demais são planejadas. Fonte detalhada da RC-07: [specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md](../../specs/008-rascunho-selecionar-servico/contracts/api-solicitacoes.md).

## Serviços disponíveis (RC-07)
`GET /protocolos` (200), só `PACIENTE`. Lista os serviços ativos; no MVP existe um, o protocolo acadêmico fictício.

```json
{
  "itens": [
    {
      "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1",
      "identificador": "PROTOCOLO-ACADEMICO-FICTICIO",
      "versao": "v0.1",
      "localidade": "Município Exemplo",
      "especialidade": "Consulta ambulatorial em cardiologia",
      "avisoFicticio": "Documento acadêmico fictício. Não é exigência oficial de nenhum serviço de saúde ou do SUS.",
      "simulacao": true
    }
  ]
}
```
Sem nenhum serviço ativo a resposta é `{ "itens": [] }` (200). `protocoloId` é `identificador` + `-` + `versao`; o identificador interno do banco não sai pela API.

## Solicitação
`POST /solicitacoes` (201, com `Location: /solicitacoes/{id}`) · `GET /solicitacoes` (200) · `GET /solicitacoes/{id}` (200) · `PUT /solicitacoes/{id}` (200). Todas só `PACIENTE`; a paciente é sempre a da sessão.

Entrada (criação e troca de serviço): `{ "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1" }`. Outros campos enviados (dono, estado, revisão) são ignorados e nunca lidos. `protocoloId` deve ser texto JSON, não vazio e com até 100 caracteres.

Saída:
```json
{
  "id": "12",
  "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1",
  "servico": {
    "localidade": "Município Exemplo",
    "especialidade": "Consulta ambulatorial em cardiologia",
    "avisoFicticio": "Documento acadêmico fictício. Não é exigência oficial de nenhum serviço de saúde ou do SUS."
  },
  "estado": "RASCUNHO",
  "revisao": 1,
  "criadaEm": "2026-11-13T12:00:00Z",
  "atualizadaEm": "2026-11-13T12:00:00Z",
  "simulacao": true
}
```
| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| `id` | string | sim (saída) | número decimal opaco gerado pelo servidor (ex.: `"12"`); id fora de `^[1-9][0-9]{0,17}$` responde 404 |
| `protocoloId` | string | sim | serviço ativo (fictício); inexistente ou inativo → 422 |
| `servico` | objeto | sim (saída) | localidade, especialidade e aviso de protocolo fictício |
| `estado` | enum | sim (saída) | ver [estados-e-revisoes.md](estados-e-revisoes.md); na RC-07 só `RASCUNHO` é produzido |
| `revisao` | inteiro ≥ 1 | sim (saída) | incrementa a cada alteração relevante (na RC-07: trocar para outro serviço) |
| `criadaEm`, `atualizadaEm` | string | sim (saída) | ISO-8601 UTC, em milissegundos |
| `simulacao` | booleano | sim (saída) | sempre `true`; indica envio apenas simulado |

- `GET /solicitacoes`: `{ "itens": [ … ], "limite": 50 }`, só os pedidos da paciente, do mais recente ao mais antigo (`criadaEm` e depois `id`, decrescente), no máximo 50; vazio é 200 com `itens` vazio.
- `PUT /solicitacoes/{id}`: só em `RASCUNHO`. Serviço diferente → `revisao` +1, `atualizadaEm` = agora e revisão registrada com o motivo `PROTOCOLO_ALTERADO`; mesmo serviço → 200 sem mudar `revisao` nem `atualizadaEm`. Fora de rascunho → 409 `CONFLITO_ESTADO`. A dona é conferida antes do corpo (404 antes de 400). Alterações simultâneas do mesmo rascunho são serializadas no banco (`SELECT … FOR UPDATE`).
- Cada `POST` bem-sucedido cria um pedido novo: o servidor **não** deduplica toques duplos; a proteção é do cliente.
- Erros: 400 `REQUISICAO_INVALIDA` (corpo ilegível/grande, `protocoloId` ausente, nulo, em branco, não textual ou longo, com `detalhes[].campo = "protocoloId"`), 401, 403, 404, 405, 409, 415, 422 `REGRA_DE_NEGOCIO` (serviço inexistente/inativo, mesma resposta), 503. Sem dado clínico: o rascunho guarda só o serviço escolhido.

## Documentos do pedido (RC-08)
Contrato completo: [specs/009-enviar-consultar-documentos/contracts/api-documentos.md](../../specs/009-enviar-consultar-documentos/contracts/api-documentos.md). Rotas só de `PACIENTE`; o sistema guarda e devolve o arquivo, **sem lê-lo, interpretá-lo ou avaliá-lo**.

**Enviar:** `POST /solicitacoes/{id}/documentos`, `multipart/form-data` com **um** arquivo por requisição: parte `arquivo` (PDF, JPEG ou PNG, tipo real verificado pelo conteúdo, não pela extensão) e parte `tipoDocumental` (`ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL` ou `OUTRO`). Cabeçalho opcional `X-Chave-Envio` (8 a 64 caracteres `[A-Za-z0-9_-]`): a **identificação de tentativa**, que permite repetir um envio interrompido sem duplicar. Aceito em qualquer estado exceto `SUBMETIDA_SIMULACAO` (até a RC-15 era só em `RASCUNHO`; a RC-16 liberou a inclusão do documento que faltava depois da análise, ver abaixo). Limites: 3 documentos disponíveis por pedido, 5 MB cada, PDF até 5 páginas, imagem até 25 milhões de pixels.

- **201** documento novo (com `Location`); **200** repetição da mesma tentativa (mesma chave, mesmo conteúdo, mesmo tipo documental): devolve o documento existente, sem gravar nada e sem nova revisão.
- Erros: 400 (`tipoDocumental`, `arquivo`, `arquivos` ou `chaveEnvio` ausente/inválido, PDF com mais de 5 páginas ou protegido, arquivo ilegível), 404, 409 `CONFLITO_ESTADO` (pedido submetido: "Este pedido já foi submetido e só pode ser consultado.") ou `CHAVE_ENVIO_REUTILIZADA` (mesma chave com outro conteúdo ou tipo), 413, 415, 422 `REGRA_DE_NEGOCIO` (já há 3 documentos), 503. Arquivo recusado **não** é persistido, não cria revisão e não deixa arquivo guardado.
- Sem `X-Chave-Envio`, cada envio é novo: o limite de 3 é a única proteção contra duplicata.

Saída do envio (os campos de `versaoConjuntoDocumental` e `documentosDisponiveis` descrevem o pedido no instante do envio):
```json
{ "id": "41", "solicitacaoId": "12", "nomeArquivo": "encaminhamento-sintetico.pdf", "tipoDocumental": "ENCAMINHAMENTO",
  "formato": "PDF", "tamanhoBytes": 120400, "paginas": 1, "estadoEnvio": "ENVIADO", "revisaoEntrada": 2,
  "enviadoEm": "2026-11-13T15:30:12.345Z", "simulacao": true, "versaoConjuntoDocumental": 2, "documentosDisponiveis": 1 }
```
`estadoEnvio` é o estado do **envio**, nunca da análise; hoje só existe `ENVIADO`. Cada envio novo cria a revisão seguinte do pedido (motivo `DOCUMENTO_ENVIADO`) e **não** inicia leitura nem análise: o estado do pedido não muda. Depois de uma análise (RC-16), o envio também torna a análise vigente não vigente na mesma transação (histórico preservado), como remover e substituir.

**Listar:** `GET /solicitacoes/{id}/documentos` → `{ "documentos": [ … ], "total": 2, "maximo": 3, "versaoConjuntoDocumental": 3 }` (cada item igual ao do envio, sem os dois últimos campos); lista vazia é 200.

**Baixar:** `GET /solicitacoes/{id}/documentos/{docId}` → o arquivo como **anexo** (`Content-Type` do formato real, `nosniff`, `no-store`), só pela dona e só pelo pedido ao qual o documento pertence; documento alheio, de outro pedido ou inexistente → o mesmo 404. O nome interno de armazenamento nunca aparece. Sem `DOCUMENT_STORAGE_PATH`, ou se o arquivo não existir na área privada (como os documentos da carga sintética), responde 503.

**No pedido** (`GET`, `POST` e `PUT /solicitacoes…`, campos aditivos): `versaoConjuntoDocumental` (igual a `revisao`), `documentosDisponiveis` e `"analise": { "situacao": "NAO_REALIZADA", "mensagem": "…" }`. Upload concluído **não** é análise concluída. Desde a RC-09, remover e substituir mudam também `versaoConjuntoDocumental` e `documentosDisponiveis`, e `analise.situacao` segue `NAO_REALIZADA` para a nova versão (seção abaixo). Desde a RC-13 `analise.situacao` é calculada: `NAO_REALIZADA` (nenhuma análise vale para a versão atual), `EM_ANDAMENTO` (pedido em `PROCESSANDO`) ou `CONCLUIDA` com `analise.resultadoGeral` (há análise vigente da revisão atual).

## Remover, substituir e excluir (RC-09)
Contrato completo: [specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md](../../specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md). Rotas só de `PACIENTE`; a paciente é a da sessão; o sistema só guarda, troca e apaga arquivos, **sem lê-los**. Pedido ou documento inexistente, alheio, de outro pedido, já removido ou com id malformado → o mesmo 404. Pedido em `SUBMETIDA_SIMULACAO` é só para consulta: remover, substituir e excluir → 409 `CONFLITO_ESTADO` ("Este pedido já foi submetido e só pode ser consultado.").

- **Remover:** `DELETE /solicitacoes/{id}/documentos/{docId}` → 200 `{ "versaoConjuntoDocumental": 4, "documentosDisponiveis": 2 }`. Revisão +1 (`DOCUMENTO_REMOVIDO`); o documento some da lista e do download; o arquivo é apagado **depois** do `COMMIT`; a vaga é liberada. Repetir → 404, sem mudar versão nem contagem.
- **Substituir:** `PUT /solicitacoes/{id}/documentos/{docId}`, mesmo multipart do envio (`arquivo`, `tipoDocumental`, `X-Chave-Envio` opcional). Uma transação: o antigo sai, o novo entra, **uma** revisão (`DOCUMENTO_SUBSTITUIDO`); a contagem não muda (sem 422 de limite). 201 com `Location` do documento **novo**; 200 se repetição (a chave é reconhecida **antes** de procurar o `{docId}`, que depois do sucesso já não existe); 409 `CHAVE_ENVIO_REUTILIZADA` com conteúdo ou tipo diferente. Arquivo recusado (400/413/415) deixa o antigo, a versão e os arquivos como estavam.
- **Excluir rascunho:** `DELETE /solicitacoes/{id}` com o cabeçalho **`X-Confirmar-Exclusao: EXCLUIR`** (valor exato) → 204 sem corpo. Sem o cabeçalho ou com outro valor → 400 `REQUISICAO_INVALIDA` (detalhe `confirmacao`) e nada é apagado. Só `RASCUNHO`; outro estado → 409 ("Somente rascunhos podem ser excluídos."). Apaga documentos (disponíveis e removidos), revisões e pedido numa transação; os arquivos saem depois do commit.
- **Ordem de verificação:** 401 → 403 → 404 (dona) → 400 (confirmação ou contrato) → 413/415 → 409 → 503. A existência do pedido é conferida antes do corpo.
- **Política de arquivos:** o arquivo de documento removido ou substituído é **apagado**; o registro fica como histórico enquanto o pedido existir (com `REVISAO_REMOCAO` preenchida e a chave de envio anulada); a exclusão do rascunho apaga tudo. Não há lixeira nem desfazer.
- **Conferência:** remover e substituir tornam não vigente a análise anterior (`RC_ANALISE.VIGENTE = 0`, histórico preservado), **não** iniciam análise e **não** mudam o estado do pedido. Desde a RC-13 isso aparece em `GET …/analises` (nenhuma vigente) e em `analise.situacao = NAO_REALIZADA`.

## Extração das informações dos arquivos (RC-10)
Contrato completo: [specs/011-extrair-informacoes-arquivos/contracts/api-extracao.md](../../specs/011-extrair-informacoes-arquivos/contracts/api-extracao.md). Rotas só de `PACIENTE`. **A leitura é simulada** (decisão de 12/11/2026): o sistema não lê o arquivo; reconhece os documentos sintéticos de demonstração e devolve dados predefinidos. Toda resposta traz `simulada: true` e um aviso fixo.

- **Ler:** `POST /solicitacoes/{id}/extracoes` (sem corpo) → 201 com a leitura nova, ou 200 se já existe leitura para a versão atual dos documentos. Lê os documentos disponíveis; um documento por vez, sem misturar campos.
- **Consultar:** `GET /solicitacoes/{id}/extracoes` → a leitura mais recente, `situacao` `ATUAL` ou `DESATUALIZADA` (depois de enviar, remover ou substituir documento). 404 se nunca houve leitura.
- **Campos** (só os usados pelas regras R01 a R06): `TIPO_DOCUMENTO`, `NOME`, `DATA_NASCIMENTO`, `MOTIVO_ENCAMINHAMENTO`, `DATA_EMISSAO`, `AUTORIZACAO_ASSINADA`. Cada um em **uma** de: `IDENTIFICADO` (com `valor` e, quando disponível, `origem` com `pagina` e `trecho`), `NAO_LOCALIZADO` (documento lido, campo ausente) ou `ILEGIVEL`; os dois últimos nunca trazem valor nem origem. "Ilegível" não significa "ausente".
- **Documento não lido:** `situacao` `PROTEGIDO` ou `LEITURA_VAZIA`, com `motivo`, `reenviar: true`, `orientacao` e `campos` vazio. Nunca declara campo `NAO_LOCALIZADO` e **não** gera resultado de completude nem pendência.
- **Falha do leitor** (ou saída fora do contrato): 503 `LEITURA_INDISPONIVEL`, sem detalhe técnico e **sem gravar nada**; a paciente pode tentar de novo.
- **Ordem de verificação:** 401 → 403 → 404 (dona) → 409 (submetido, mesmo com leitura existente) → 422 `SEM_DOCUMENTOS` → 200 (já existe) → leitura → 409 (documentos mudaram durante a leitura) → 503.
- **Efeitos:** uma leitura por versão do conjunto documental (repetir não duplica, nem sob concorrência); não muda o estado do pedido, não cria revisão, não inicia análise.

```json
{ "id": "12", "situacao": "ATUAL", "versaoConjuntoDocumental": 3, "simulada": true,
  "aviso": "Leitura simulada com dados sintéticos; não é leitura real dos documentos.", "criadaEm": "2026-11-13T16:00:00.000Z",
  "documentos": [
    { "documentoId": "41", "tipoDocumental": "ENCAMINHAMENTO", "situacao": "LIDO",
      "campos": [ { "codigo": "NOME", "situacao": "IDENTIFICADO", "valor": "Maria Exemplo da Silva",
                    "origem": { "pagina": 1, "trecho": "Paciente: Maria Exemplo da Silva" } },
                  { "codigo": "AUTORIZACAO_ASSINADA", "situacao": "NAO_LOCALIZADO" } ] },
    { "documentoId": "42", "tipoDocumental": "OUTRO", "situacao": "LEITURA_VAZIA", "reenviar": true,
      "motivo": "Não foi possível extrair informações deste arquivo.", "orientacao": "Isso não significa que as informações estejam faltando no documento. Envie um arquivo mais nítido.", "campos": [] } ] }
```


## Conferência da transcrição (RC-11)
Contrato completo: [specs/012-conferir-transcricao/contracts/api-conferencia.md](../../specs/012-conferir-transcricao/contracts/api-conferencia.md). Rotas só de `PACIENTE`. **A leitura é simulada** e a conferência só registra o que a paciente decide: **confirmar** ou **apontar divergência**, campo a campo. **Nenhum valor digitado é aceito**: informação clínica incorreta ou ausente é corrigida pela unidade emissora, nunca pela paciente (CLAUDE.md, seção 11).

- **Consultar:** `GET /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` → `extracaoId`, `situacao`, `liberadoParaSubmissao` (**sempre `false`**), `simulada: true`, `aviso`, `atualizadaEm`, `resumo` (`camposNaoVerificados`, `camposSemDecisao`, `divergencias`) e `documentos[]`. Cada campo traz `valor` e `origem` só se identificado, `naoVerificado` (`true` para ilegível, não localizado ou com divergência apontada), `decisao` (`tipo`, `motivo`, `decididaEm`) e `orientacao` (texto fixo do servidor). Um campo identificado com divergência continua mostrando o que foi lido, marcado como não verificado.
- **Decidir:** `PUT` na mesma rota, `Content-Type: application/json`, até 4 KB, `{"decisoes":[{"documentoId":"41","codigo":"NOME","decisao":"CONFIRMADA"}]}` com 1 a 18 itens. Só as chaves `documentoId`, `codigo`, `decisao` e `motivo` existem; **qualquer outra (como `valor`) é 400**, inclusive na raiz. `decisao` é `CONFIRMADA` (sem `motivo`) ou `DIVERGENTE` (com `motivo`: `VALOR_DIFERENTE_DO_DOCUMENTO` em campo identificado, `CAMPO_EXISTE_E_NAO_FOI_LIDO` em campo não localizado). Par (documento, campo) repetido no corpo é 400.
- **Aplicabilidade:** campo `IDENTIFICADO` aceita confirmar ou divergir por valor diferente; `NAO_LOCALIZADO` aceita só divergir por "o campo existe e não foi lido"; `ILEGIVEL` não aceita decisão (resolve-se enviando um arquivo mais nítido); documento não lido não tem campos. Fora disso: 422 `DECISAO_NAO_APLICAVEL`.
- **Situação** (calculada, nunca gravada; precedência): `DESATUALIZADA` (a leitura não é a da versão atual dos documentos) > `REQUER_CONFERENCIA` (documento não lido, campo ilegível em documento lido ou alguma divergência) > `PENDENTE` (campo identificado sem decisão) > `CONFERIDA`. Campo não localizado sem divergência não pesa. `CONFERIDA` só diz que a paciente conferiu a leitura: **não libera submissão**, não é resultado de regra, pendência nem aprovação.
- **Efeitos:** uma decisão vigente por campo (decidir de novo substitui; repetir a mesma não muda nada, nem `decididaEm`); desde a RC-13, gravar uma decisão nova ou diferente tira a vigência da análise do pedido (histórico preservado, estado mantido); lote **tudo ou nada**; a leitura original (valores, situações, origens) e os documentos ficam intactos; **não muda o estado nem a revisão do pedido** (desvio justificado do princípio VI, ver [estados-e-revisoes.md](estados-e-revisoes.md)). `atualizadaEm` é o instante da decisão mais recente (ou o da leitura, se não há decisão).
- **Leitura desatualizada** (depois de enviar, remover ou substituir documento): `GET` mostra `DESATUALIZADA`, somente leitura, com as decisões anteriores; `PUT` → 409 "Os documentos mudaram; leia os documentos de novo."; nova leitura (`POST …/extracoes`) nasce **sem** decisões herdadas. Pedido `SUBMETIDA_SIMULACAO`: `GET` 200 e `PUT` 409.
- **Ordem de verificação:** 401 → 403 → 404 (pedido ou leitura do pedido, antes de ler o corpo) → 415/400 (corpo, só no `PUT`) → 409 (submetido; leitura desatualizada) → 404 (documento fora da leitura) → 422 → 503. Pedido ou leitura inexistente, alheio, de outro pedido ou id mal formado: o mesmo 404.
- **Textos:** as orientações são fixas e não repetem valor ou trecho do documento; não há diagnóstico, urgência, prioridade nem promessa de atendimento.

```json
{
  "extracaoId": "12",
  "situacao": "REQUER_CONFERENCIA",
  "liberadoParaSubmissao": false,
  "simulada": true,
  "aviso": "Leitura simulada com dados sintéticos; não é leitura real dos documentos.",
  "atualizadaEm": "2026-11-13T16:00:00.000Z",
  "resumo": { "camposNaoVerificados": 3, "camposSemDecisao": 2, "divergencias": 1 },
  "documentos": [ { "documentoId": "41", "tipoDocumental": "ENCAMINHAMENTO", "situacao": "LIDO", "campos": [
    { "codigo": "NOME", "situacao": "IDENTIFICADO", "valor": "Maria Exemplo da Silva",
      "origem": { "pagina": 1, "trecho": "Paciente: Maria Exemplo da Silva" }, "naoVerificado": false,
      "decisao": { "tipo": "CONFIRMADA", "decididaEm": "2026-11-13T16:00:00.000Z" } },
    { "codigo": "AUTORIZACAO_ASSINADA", "situacao": "NAO_LOCALIZADO", "naoVerificado": true,
      "orientacao": "Esta informação não consta no documento lido. Se ela for necessária, peça um documento corrigido à unidade emissora. Não é possível preencher aqui." } ] } ]
}
```
## Análise (RC-13)
Contrato completo: [specs/014-persistir-analises-historico/contracts/api-analises.md](../../specs/014-persistir-analises-historico/contracts/api-analises.md). Rotas só de `PACIENTE`. **A leitura é simulada e o protocolo é acadêmico e fictício**: toda análise traz `simulada: true` e o `aviso`; não há resumo por IA nesta história (RC-14).

- **Pedir:** `POST /solicitacoes/{id}/analises` (sem corpo) → 201 com a análise gravada agora (vigente; pedido no resultado geral) ou 200 com a vigente que já vale para a versão atual dos documentos e a conferência atual. **Síncrona**: o pedido fica em `PROCESSANDO` só enquanto a análise roda.
- **Consultar:** `GET /solicitacoes/{id}/analises` → `{ solicitacaoId, estado, versaoConjuntoDocumental, execucao?, vigenteId?, limite: 50, analises: [...] }`, da mais recente para a mais antiga. `execucao` só em `PROCESSANDO`: `EM_ANDAMENTO` ou `INTERROMPIDA` (passou do limite de 1 minuto; pode pedir de novo). Pedido nunca analisado → lista vazia.
- **Erros:** 404 idêntico para pedido alheio ou inexistente; 409 `CONFLITO_ESTADO` (submetido, sem leitura atual, entrada mudou durante a análise, análise assumida depois do limite); 409 `ANALISE_EM_ANDAMENTO` com `Retry-After`; 503 `ANALISE_INDISPONIVEL` (pedido em `FALHA_ANALISE`, pode tentar de novo) ou `DEPENDENCIA_INDISPONIVEL`.
- **Histórico:** cada análise é gravada uma vez e nunca reescrita; só `vigente` muda (para `false`) quando documentos ou conferência mudam ou quando outra análise vira a vigente. Mais de uma análise da mesma `revisao` é possível (a conferência não cria revisão), distinguidas por `conferenciaConsiderada`.

```json
{
  "id": "31", "revisao": 6, "vigente": true, "executadaEm": "2026-11-14T13:05:10.123Z",
  "protocolo": { "identificador": "PROTOCOLO-ACADEMICO-FICTICIO", "versao": "v0.1" },
  "conferenciaConsiderada": "2026-11-14T13:04:58.001Z", "dataReferencia": "2026-11-14",
  "resultadoGeral": "COM_PENDENCIAS", "simulada": true,
  "aviso": "Análise simulada: leitura simulada dos documentos e protocolo acadêmico fictício. \"Sem pendências identificadas\" não é aprovação do SUS.",
  "documentos": [ { "documentoId": "41", "nomeArquivo": "encaminhamento-sintetico.pdf", "tipoDeclarado": "ENCAMINHAMENTO", "disponivel": true } ],
  "resultados": [ { "codigoRegra": "RC-R01", "descricao": "…", "resultado": "ATENDE" },
                  { "codigoRegra": "RC-R04", "descricao": "…", "resultado": "PENDENTE" } ],
  "pendencias": [ { "codigoRegra": "RC-R04", "descricaoRegra": "…", "tipo": "PENDENTE",
                    "explicacao": "O encaminhamento não traz o motivo…", "responsavel": "UNIDADE_EMISSORA",
                    "evidencias": [ { "documentoId": "41", "campo": "MOTIVO_ENCAMINHAMENTO", "situacao": "NAO_LOCALIZADO" } ] } ]
}
```
| Campo | Valores |
|---|---|
| `resultadoGeral` | `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS` (regras em [protocolo-academico.md](../protocolo-academico.md)); **não** é aprovação do SUS |
| `resultados[].resultado` | `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL`; `motivo` só em `NAO_VERIFICAVEL` |
| `conferenciaConsiderada`, `dataReferencia` | omitidos só nas análises da carga sintética (`02_`) |
| `documentos[]` | documentos da revisão avaliada; `disponivel: false` se depois foram substituídos ou removidos |
| `simulada` | sempre `true` enquanto extração e IA forem mockadas |

Campos nulos ficam fora do JSON (convenção da API).

## Reenvio e verificação da versão corrigida (RC-16)
Contrato completo: [specs/020-reenviar-versao-corrigida/contracts/api-reenvio.md](../../specs/020-reenviar-versao-corrigida/contracts/api-reenvio.md). Rotas só de `PACIENTE`. Leitura e resumo são **simulados**; o protocolo é acadêmico e fictício.

- **Corrigir:** substituir ou remover (RC-09) e, desde a RC-16, **incluir** documento (`POST …/documentos`) também depois da análise, exceto em pedido submetido. Cada mudança cria a revisão seguinte e tira a vigência da análise, sem mudar o estado e **sem marcar nada como resolvido**.
- **Verificar:** `POST /solicitacoes/{id}/reenvio` (sem corpo) refaz, para a revisão atual, leitura (reaproveita a existente) → conferência → análise (RC-13) → resumo (RC-14). Resposta `{ solicitacaoId, etapa, revisao, extracaoId, leituraCriada, camposSemDecisao, analiseCriada, simulada: true, orientacao, analise? }`:
  - `etapa: "CONFERENCIA"` (200): há campo lido sem decisão da paciente; nenhuma análise registrada. Conferir (`PUT …/extracoes/{extracaoId}/conferencia`) e verificar de novo.
  - `etapa: "CONCLUIDA"`: 201 com a análise nova (vigente, com `resumo` e `comparacao`) ou 200 com a vigente que já valia (nada mudou; nenhuma análise nem resumo novo).
  - `etapa: "RESUMO"`: a análise foi registrada (201) ou já existia (200), mas o resumo não pôde ser gravado agora; `analise` vem sem `resumo` e não é desfeita.
- **Erros da verificação:** mesmo `status`/`codigo` da etapa que falhou (`SEM_DOCUMENTOS`, `LEITURA_INDISPONIVEL`, `CONFLITO_ESTADO`, `ANALISE_EM_ANDAMENTO` com `Retry-After`, `ANALISE_INDISPONIVEL`, `DEPENDENCIA_INDISPONIVEL`), `mensagem` com o que repetir e `detalhes: [{ "campo": "etapa", "mensagem": "LEITURA" | "CONFERENCIA" | "ANALISE" }]`. A última análise concluída fica intacta e nada parcial é gravado. 404 idêntico, sem detalhe; pedido submetido → 409 sem etapa.
- **Comparação** (`comparacao`, campo aditivo da análise em `POST`/`GET …/analises`, `POST …/resumo` e `POST …/reenvio`): compara a análise com a registrada **imediatamente antes** dela no mesmo pedido; omitida na primeira análise e entre versões de protocolo diferentes. Calculada a partir das análises gravadas, que não mudam.

```json
"comparacao": { "analiseAnteriorId": "12", "revisaoAnterior": 4, "executadaEmAnterior": "2026-11-17T13:05:10.123Z",
  "itens": [ { "codigoRegra": "RC-R04", "descricaoRegra": "…", "situacao": "RESOLVIDA",
               "resultadoAnterior": "PENDENTE", "resultadoAtual": "ATENDE" } ] }
```
| `situacao` | Anterior → atual |
|---|---|
| `RESOLVIDA` | `PENDENTE`/`NAO_VERIFICAVEL` → `ATENDE`/`NAO_APLICAVEL` |
| `CONTINUA` | `PENDENTE`/`NAO_VERIFICAVEL` → `PENDENTE`/`NAO_VERIFICAVEL` |
| `NOVA` | `ATENDE`/`NAO_APLICAVEL` → `PENDENTE`/`NAO_VERIFICAVEL` |

Regras sem pendência nas duas análises não aparecem. "Resolvida" é só a leitura da regra no novo resultado simulado; **não** é aprovação do SUS.

## Estação de captura simulada (RC-21 + RC-22)
Contrato completo: [specs/016-iot-simulada/contracts/api-iot.md](../../specs/016-iot-simulada/contracts/api-iot.md). Simulador: [simulador-iot/README.md](../../simulador-iot/README.md). **Os sensores são simulados e os limites são demonstrativos, sem calibração**: toda telemetria traz `simulada: true` e o `aviso`; as leituras **não comprovam a legibilidade** do arquivo e nunca bloqueiam envio, salvamento ou exclusão do rascunho.

- **Abrir sessão (paciente):** `POST /solicitacoes/{id}/sessoes-captura` (sem corpo) → 201 `{ id, solicitacaoId, iniciadaEm, aberta: true, criada: true, simulada: true }` ou 200 com a sessão já aberta (`criada: false`). No máximo uma sessão aberta por pedido; não há rota para encerrar nesta entrega.
- **Enviar leitura (simulador, `X-Chave-Simulador`):** `POST /sessoes-captura/{id}/leituras` com `{ "sessaoId": 42, "tipo": "LUMINOSIDADE"|"DISTANCIA", "valor": 350, "unidade": "LUX"|"CM", "medidaEm": "2026-11-16T14:00:05Z" }` → 201 com a leitura gravada (`recebidaEm` do servidor). Limites físicos: 0–200000 lux; >0–500 cm; `medidaEm` até 30 s no futuro.
- **Consultar telemetria (paciente):** `GET /solicitacoes/{id}/leituras` → `{ solicitacaoId, sessao?, consultadaEm, prazoAtualidadeSegundos: 60, simulada, aviso, sensores: [luminosidade, distância] }`; cada sensor com `tipo`, `unidade`, `faixa {minimo, maximo}`, `ultimaLeitura? {valor, medidaEm}`, `situacao` e `orientacao {codigo, mensagem}`.

| Campo | Valores |
|---|---|
| `situacao` | `DENTRO_DA_FAIXA`, `ABAIXO_DA_FAIXA`, `ACIMA_DA_FAIXA`, `DESATUALIZADA` (medida há mais de 60 s), `SEM_LEITURA` |
| `orientacao.codigo` | `CONDICAO_ADEQUADA`, `MELHORAR_ILUMINACAO`, `REDUZIR_ILUMINACAO_OU_REFLEXO`, `AFASTAR_DOCUMENTO`, `APROXIMAR_DOCUMENTO`, `VERIFICAR_ESTACAO`, `CAPTURA_SEGUE_SEM_SENSOR` (o código é o contrato com a tela do Épico 6; a mensagem é texto fixo) |
| `faixa` | luminosidade 300–1000 lux; distância 20–40 cm; limites inclusivos e demonstrativos |

Sem sessão ou sem leitura a resposta é 200 com os sensores em `SEM_LEITURA` (`sessao` e `ultimaLeitura` omitidos).

## Pendência
Uma por regra `PENDENTE` ou `NAO_VERIFICAVEL` da análise: `codigoRegra` e `descricaoRegra` (da versão do protocolo da análise), `tipo`, `explicacao` (mensagem da regra, se pendente; mensagem padrão de não verificável do protocolo, se não verificável), `motivo` (lista fechada, só não verificável: `CAMPO_ILEGIVEL`, `DOCUMENTO_NAO_LIDO`, `TRANSCRICAO_NAO_CONFERIDA`, `DIVERGENCIA_APONTADA`, `VALOR_NAO_INTERPRETAVEL`, `CONDICAO_INDETERMINADA`, `DOCUMENTOS_INSUFICIENTES`, `DEPENDE_DE_DOCUMENTO_AUSENTE`), `responsavel` ∈ {`PACIENTE`, `UNIDADE_EMISSORA`} e `evidencias[]` (`documentoId`, `campo`, `situacao` no momento da análise e `origem` `{pagina, trecho}` lida da leitura daquela revisão, quando houver). **Nenhum valor lido aparece na pendência.** Mensagens não diagnosticam, não definem urgência, não autorizam atendimento nem prometem vaga.

## Submissão simulada (RC-17)
Contrato completo: [specs/021-submeter-simulacao/contracts/api-submissao.md](../../specs/021-submeter-simulacao/contracts/api-submissao.md). Rotas só de `PACIENTE`. **A submissão é simulada**: nada é enviado ao SUS, não há fila real nem número oficial de protocolo.

- **Concluir:** `POST /solicitacoes/{id}/submissoes-simuladas` (sem corpo) → 201 com o comprovante e o pedido em `SUBMETIDA_SIMULACAO` (só consulta), ou 200 com o **mesmo** comprovante se o pedido já tinha submissão (uma por pedido, também sob pedidos simultâneos). Exige, conferidos sob o bloqueio do pedido: análise **vigente** da revisão atual com `SEM_PENDENCIAS_IDENTIFICADAS`, nenhum critério `PENDENTE` ou `NAO_VERIFICAVEL` e o pedido em `SEM_PENDENCIAS_IDENTIFICADAS`. `NAO_APLICAVEL` e resumo ausente não impedem.
- **Consultar:** `GET /solicitacoes/{id}/submissoes-simuladas` → 200 com o mesmo comprovante; pedido ainda não submetido tem o mesmo 404 de pedido inexistente ou alheio.
- **Recusas:** 409 `CONFLITO_ESTADO`, nada registrado e estado igual: análise com pendência ou não verificável; sem análise vigente da versão atual (rascunho, nunca analisado, falha, ou documentos/conferência mudaram); análise em andamento; pedido já só para consulta sem registro. As recusas de completude dizem que **a restrição é do simulador acadêmico e não impede atendimento em serviço de saúde**.

```json
{ "solicitacaoId": "12", "referencia": "SIM-2026-000012", "submetidaEm": "2026-11-17T14:45:03.120Z", "simulada": true,
  "destaque": "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS",
  "servico": { "localidade": "Município Exemplo", "especialidade": "Consulta ambulatorial em cardiologia", "avisoFicticio": "…" },
  "versaoAnalisada": { "analiseId": "31", "revisao": 6, "protocolo": { "identificador": "PROTOCOLO-ACADEMICO-FICTICIO", "versao": "v0.1" } },
  "nota": "Este comprovante é de demonstração. Não é documento clínico, não é protocolo oficial e não coloca você em fila real." }
```
`referencia` = `SIM-{ano em Salvador}-{id do pedido com no mínimo 6 dígitos}`: única porque cada pedido tem no máximo uma submissão (`RC_SUBMISSAO_UK_SOLIC`, `RC_SUBMISSAO_UK_REF`) e diferente do formato da carga sintética (`SIM-2026-0001`). `destaque` e `nota` são textos fixos do servidor.
