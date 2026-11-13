# Data Model: Enviar e consultar documentos (RC-08)

## Tabelas usadas

| Tabela | Uso nesta história |
|---|---|
| `RC_SOLICITACAO` | bloqueada com `FOR UPDATE` no envio; `REVISAO_ATUAL` e `ATUALIZADA_EM` atualizados; `ESTADO` deve ser `RASCUNHO` |
| `RC_REVISAO` | ganha uma linha por envio concluído, motivo `DOCUMENTO_ENVIADO` (já aceito pela restrição existente) |
| `RC_DOCUMENTO` | uma linha por arquivo enviado; **ganha 3 colunas** (abaixo) |

## Mudança de esquema (script `07_documento_tipo_e_tentativa.sql`)

Aditiva: só `ALTER TABLE … ADD`. Nenhuma linha é apagada ou reescrita.

| Coluna nova em `RC_DOCUMENTO` | Tipo | Regra |
|---|---|---|
| `TIPO_DOCUMENTAL` | `VARCHAR2(30 CHAR) DEFAULT 'OUTRO' NOT NULL` | um de `ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL`, `OUTRO` (`RC_DOCUMENTO_CK_TIPO_DOC`). O `DEFAULT` cobre só as linhas da carga sintética já existentes; a aplicação sempre informa o valor |
| `CHAVE_ENVIO` | `VARCHAR2(64 CHAR)` | identificação de tentativa; opcional; única por pedido (`RC_DOCUMENTO_UK_CHAVE`, índice único funcional `(CASE WHEN CHAVE_ENVIO IS NOT NULL THEN SOLICITACAO_ID END, CHAVE_ENVIO)`; vários envios sem chave no mesmo pedido são permitidos. Uma `UNIQUE (SOLICITACAO_ID, CHAVE_ENVIO)` comum **não** serve: no Oracle chave composta com uma só coluna nula conta como valor, e foi o ORA-02299 ao aplicar o `07_` na conta do grupo que revelou isso) |
| `HASH_CONTEUDO` | `VARCHAR2(64 CHAR)` | SHA-256 do arquivo em hexadecimal minúsculo; nulo nas linhas antigas; quando presente tem 64 hexadecimais (`RC_DOCUMENTO_CK_HASH`) |

Sem este script, a API sobe e as rotas de pedido continuam funcionando, mas o **envio** de documento falha ao gravar (ORA-00904 coluna inexistente → erro interno genérico, com orientação no log para executar o `07_`). O `01_criar_esquema.sql` recebe as mesmas colunas para instalações novas.

Reaproveitado sem mudança: `NOME_ORIGINAL` (nome de exibição saneado), `TIPO` (`PDF`/`JPEG`/`PNG`, pelo conteúdo), `TAMANHO_BYTES` (1 a 5 MB), `PAGINAS` (PDF até 5), `CAMINHO_INTERNO` (nome aleatório, nunca exposto), `REVISAO_ENVIO`, `REVISAO_REMOCAO` (nulo = disponível; sem uso de escrita nesta história), `ENVIADO_EM`.

## Entidades (Java)

| Tipo | Campos | Observação |
|---|---|---|
| `Documento` (record) | `id`, `solicitacaoId`, `nomeArquivo`, `tipoDocumental`, `formato` (`TipoArquivo`), `tamanhoBytes`, `paginas`, `revisaoEntrada`, `enviadoEm`, `caminhoInterno` (`IdInterno`), `hashConteudo`, `chaveEnvio` | `caminhoInterno`, `hashConteudo` e `chaveEnvio` ficam só na camada de repositório/serviço; o DTO não os tem. `caminhoInterno` é **nulo** nas linhas da carga sintética (nomes fictícios como `sim/doc-0001.dat` não têm o formato de identificador interno): elas aparecem na lista, mas o download responde 503 |
| `TipoDocumental` (enum) | `ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL`, `OUTRO` | `de(String)` devolve vazio para valor fora do conjunto (sem `valueOf` que lance) |
| `EstadoEnvio` (enum) | `ENVIADO` | derivado; novos estados entram nas histórias de processamento |
| `ResultadoRegistro` | `NAO_ENCONTRADO`, `FORA_DE_RASCUNHO`, `LIMITE`, `CONFLITO_CHAVE`, `REPETIDO(documento, versao, contagem)`, `REGISTRADO(documento, versao, contagem)` | espelha `ResultadoTroca` da RC-07; a versão e a contagem são lidas na própria transação |
| `Solicitacao` (RC-07) | `+ documentosDisponiveis` | contagem de documentos com `REVISAO_REMOCAO IS NULL` |

## DTOs

**`DocumentoResponse`** (saída; nenhum caminho, hash ou chave):

```json
{
  "id": "41",
  "solicitacaoId": "12",
  "nomeArquivo": "encaminhamento-sintetico.pdf",
  "tipoDocumental": "ENCAMINHAMENTO",
  "formato": "PDF",
  "tamanhoBytes": 120400,
  "paginas": 1,
  "estadoEnvio": "ENVIADO",
  "revisaoEntrada": 2,
  "enviadoEm": "2026-11-13T15:30:12.345Z",
  "simulacao": true
}
```

**`ListaDocumentosResponse`**: `{ "documentos": [ … ], "total": 2, "maximo": 3, "versaoConjuntoDocumental": 3 }`.

**`SolicitacaoResponse`** (acréscimos aditivos): `versaoConjuntoDocumental` (igual a `revisao`), `documentosDisponiveis` e
`"analise": { "situacao": "NAO_REALIZADA", "mensagem": "Nenhuma análise foi realizada para esta versão dos documentos." }`.

**Entrada do envio** (`EnvioDocumento`, já validada): `tipoDocumental`, `chaveEnvio` (opcional), nome e fluxo do arquivo. Não tem campo de dono, estado, revisão ou caminho.

## Invariantes (verificadas por teste)

1. Um documento só existe junto com a revisão que o criou (`REVISAO_ENVIO` aponta uma linha de `RC_REVISAO` do mesmo pedido, motivo `DOCUMENTO_ENVIADO`), gravados na mesma transação.
2. `documentosDisponiveis` ≤ 3, mesmo sob envios simultâneos.
3. Numeração de revisões do pedido sem salto nem repetição.
4. Recusa (arquivo inválido, limite, estado, chave reutilizada) e repetição não alteram revisão, contagem nem deixam arquivo na área privada.
5. `(SOLICITACAO_ID, CHAVE_ENVIO)` nunca se repete; mesma chave só devolve o mesmo documento se hash e tipo documental forem iguais.
6. Todo `CAMINHO_INTERNO` tem arquivo correspondente na área privada, exceto as linhas da carga sintética (que apontam para arquivos fictícios).
7. Documento só é lido/baixado pelo pedido ao qual pertence e pela dona desse pedido.

## Estado do pedido

Sem mudança de máquina de estados: enviar documento mantém `RASCUNHO` (`RASCUNHO → RASCUNHO`, revisão + 1, como já descrito em `docs/api/estados-e-revisoes.md`). Pedido fora de rascunho → 409 `CONFLITO_ESTADO`.
