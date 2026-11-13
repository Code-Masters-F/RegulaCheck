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

**Enviar:** `POST /solicitacoes/{id}/documentos`, `multipart/form-data` com **um** arquivo por requisição: parte `arquivo` (PDF, JPEG ou PNG, tipo real verificado pelo conteúdo, não pela extensão) e parte `tipoDocumental` (`ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL` ou `OUTRO`). Cabeçalho opcional `X-Chave-Envio` (8 a 64 caracteres `[A-Za-z0-9_-]`): a **identificação de tentativa**, que permite repetir um envio interrompido sem duplicar. Só em `RASCUNHO`. Limites: 3 documentos disponíveis por pedido, 5 MB cada, PDF até 5 páginas, imagem até 25 milhões de pixels.

- **201** documento novo (com `Location`); **200** repetição da mesma tentativa (mesma chave, mesmo conteúdo, mesmo tipo documental): devolve o documento existente, sem gravar nada e sem nova revisão.
- Erros: 400 (`tipoDocumental`, `arquivo`, `arquivos` ou `chaveEnvio` ausente/inválido, PDF com mais de 5 páginas ou protegido, arquivo ilegível), 404, 409 `CONFLITO_ESTADO` (pedido fora de rascunho) ou `CHAVE_ENVIO_REUTILIZADA` (mesma chave com outro conteúdo ou tipo), 413, 415, 422 `REGRA_DE_NEGOCIO` (já há 3 documentos), 503. Arquivo recusado **não** é persistido, não cria revisão e não deixa arquivo guardado.
- Sem `X-Chave-Envio`, cada envio é novo: o limite de 3 é a única proteção contra duplicata.

Saída do envio (os campos de `versaoConjuntoDocumental` e `documentosDisponiveis` descrevem o pedido no instante do envio):
```json
{ "id": "41", "solicitacaoId": "12", "nomeArquivo": "encaminhamento-sintetico.pdf", "tipoDocumental": "ENCAMINHAMENTO",
  "formato": "PDF", "tamanhoBytes": 120400, "paginas": 1, "estadoEnvio": "ENVIADO", "revisaoEntrada": 2,
  "enviadoEm": "2026-11-13T15:30:12.345Z", "simulacao": true, "versaoConjuntoDocumental": 2, "documentosDisponiveis": 1 }
```
`estadoEnvio` é o estado do **envio**, nunca da análise; hoje só existe `ENVIADO`. Cada envio novo cria a revisão seguinte do pedido (motivo `DOCUMENTO_ENVIADO`) e **não** inicia análise: o pedido continua `RASCUNHO`.

**Listar:** `GET /solicitacoes/{id}/documentos` → `{ "documentos": [ … ], "total": 2, "maximo": 3, "versaoConjuntoDocumental": 3 }` (cada item igual ao do envio, sem os dois últimos campos); lista vazia é 200.

**Baixar:** `GET /solicitacoes/{id}/documentos/{docId}` → o arquivo como **anexo** (`Content-Type` do formato real, `nosniff`, `no-store`), só pela dona e só pelo pedido ao qual o documento pertence; documento alheio, de outro pedido ou inexistente → o mesmo 404. O nome interno de armazenamento nunca aparece. Sem `DOCUMENT_STORAGE_PATH`, ou se o arquivo não existir na área privada (como os documentos da carga sintética), responde 503.

**No pedido** (`GET`, `POST` e `PUT /solicitacoes…`, campos aditivos): `versaoConjuntoDocumental` (igual a `revisao`), `documentosDisponiveis` e `"analise": { "situacao": "NAO_REALIZADA", "mensagem": "…" }`. Upload concluído **não** é análise concluída. Desde a RC-09, remover e substituir mudam também `versaoConjuntoDocumental` e `documentosDisponiveis`, e `analise.situacao` segue `NAO_REALIZADA` para a nova versão (seção abaixo).

## Remover, substituir e excluir (RC-09)
Contrato completo: [specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md](../../specs/010-substituir-excluir-documentos/contracts/api-alteracao-documentos.md). Rotas só de `PACIENTE`; a paciente é a da sessão; o sistema só guarda, troca e apaga arquivos, **sem lê-los**. Pedido ou documento inexistente, alheio, de outro pedido, já removido ou com id malformado → o mesmo 404. Pedido em `SUBMETIDA_SIMULACAO` é só para consulta: remover, substituir e excluir → 409 `CONFLITO_ESTADO` ("Este pedido já foi submetido e só pode ser consultado.").

- **Remover:** `DELETE /solicitacoes/{id}/documentos/{docId}` → 200 `{ "versaoConjuntoDocumental": 4, "documentosDisponiveis": 2 }`. Revisão +1 (`DOCUMENTO_REMOVIDO`); o documento some da lista e do download; o arquivo é apagado **depois** do `COMMIT`; a vaga é liberada. Repetir → 404, sem mudar versão nem contagem.
- **Substituir:** `PUT /solicitacoes/{id}/documentos/{docId}`, mesmo multipart do envio (`arquivo`, `tipoDocumental`, `X-Chave-Envio` opcional). Uma transação: o antigo sai, o novo entra, **uma** revisão (`DOCUMENTO_SUBSTITUIDO`); a contagem não muda (sem 422 de limite). 201 com `Location` do documento **novo**; 200 se repetição (a chave é reconhecida **antes** de procurar o `{docId}`, que depois do sucesso já não existe); 409 `CHAVE_ENVIO_REUTILIZADA` com conteúdo ou tipo diferente. Arquivo recusado (400/413/415) deixa o antigo, a versão e os arquivos como estavam.
- **Excluir rascunho:** `DELETE /solicitacoes/{id}` com o cabeçalho **`X-Confirmar-Exclusao: EXCLUIR`** (valor exato) → 204 sem corpo. Sem o cabeçalho ou com outro valor → 400 `REQUISICAO_INVALIDA` (detalhe `confirmacao`) e nada é apagado. Só `RASCUNHO`; outro estado → 409 ("Somente rascunhos podem ser excluídos."). Apaga documentos (disponíveis e removidos), revisões e pedido numa transação; os arquivos saem depois do commit.
- **Ordem de verificação:** 401 → 403 → 404 (dona) → 400 (confirmação ou contrato) → 413/415 → 409 → 503. A existência do pedido é conferida antes do corpo.
- **Política de arquivos:** o arquivo de documento removido ou substituído é **apagado**; o registro fica como histórico enquanto o pedido existir (com `REVISAO_REMOCAO` preenchida e a chave de envio anulada); a exclusão do rascunho apaga tudo. Não há lixeira nem desfazer.
- **Conferência:** remover e substituir tornam não vigente a análise anterior (`RC_ANALISE.VIGENTE = 0`, histórico preservado), **não** iniciam análise e **não** mudam o estado do pedido. Nenhuma rota produz análise ainda, então isso é exercitado só com pedidos montados em memória nos testes.

## Análise
`POST /solicitacoes/{id}/analises` (202 se assíncrona; 200 se concluída na chamada) · `GET /solicitacoes/{id}/analises` (200)

```json
{
  "id": "ana-0001",
  "revisao": 2,
  "versaoProtocolo": "v0.1",
  "resultadoGeral": "COM_PENDENCIAS",
  "vigente": true,
  "simulado": true,
  "resultados": [
    { "codigoRegra": "RC-R01", "resultado": "ATENDE" },
    { "codigoRegra": "RC-R04", "resultado": "PENDENTE" },
    { "codigoRegra": "RC-R06", "resultado": "NAO_APLICAVEL" }
  ],
  "pendencias": [ /* ver Pendência */ ],
  "resumo": { "texto": "…", "simulado": true, "origem": "ADAPTADOR_MOCKADO" }
}
```
| Campo | Valores |
|---|---|
| `resultadoGeral` | `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS` (regras em [protocolo-academico.md](../protocolo-academico.md)) |
| `resultados[].resultado` | `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL` |
| `simulado` / `resumo.simulado` | sempre `true` enquanto IA e extração forem mockadas; a interface deve mostrar a identificação |

O resultado só é `vigente` se `revisao` da análise for igual à revisão atual da solicitação. `SEM_PENDENCIAS_IDENTIFICADAS` não é aprovação do SUS.

## Pendência
```json
{
  "codigoRegra": "RC-R04",
  "resultado": "PENDENTE",
  "mensagem": "O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento.",
  "responsavelCorrecao": "UNIDADE_EMISSORA"
}
```
`codigoRegra` deve existir no protocolo vigente; `responsavelCorrecao` ∈ {`PACIENTE`, `UNIDADE_EMISSORA`}. Mensagens não diagnosticam, não definem urgência, não autorizam atendimento nem prometem vaga.

## Submissão simulada
`POST /solicitacoes/{id}/submissoes-simuladas` (201): só a partir de `SEM_PENDENCIAS_IDENTIFICADAS` vigente; repetir devolve a mesma submissão (idempotente). Resposta traz referência interna, data e o aviso `"SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS"`.
