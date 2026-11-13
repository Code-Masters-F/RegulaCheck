# Contrato da API: documentos do pedido (RC-08)

Rotas privadas, **somente perfil `PACIENTE`** (a RC-05 devolve 401 sem sessão e 403 para administrador e simulador). A paciente é sempre a da sessão. Dados sintéticos; o sistema só guarda e devolve o arquivo, sem lê-lo, interpretá-lo ou avaliá-lo.

## Ordem de verificação (todas as rotas)

401 sem sessão → 403 perfil indevido → 404 pedido (e documento) inexistente, alheio ou de outro pedido → 415/413/400 contrato (corpo, campos, arquivo) → 409 estado ou chave reutilizada → 422 regra de negócio (limite) → 503 indisponibilidade. A existência do pedido é conferida **antes** de ler o corpo.

## `POST /solicitacoes/{id}/documentos`

Envia **um** arquivo ao rascunho.

Requisição: `multipart/form-data`.

| Parte / cabeçalho | Obrigatório | Regra |
|---|---|---|
| `arquivo` (parte de arquivo) | sim | uma só; PDF, JPEG ou PNG pelo **conteúdo**; extensão coerente; ≤ 5 MB; PDF ≤ 5 páginas e sem senha; imagem ≤ 25 milhões de pixels |
| `tipoDocumental` (parte de texto) | sim | `ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL` ou `OUTRO` |
| `X-Chave-Envio` (cabeçalho) | não | 8 a 64 caracteres `[A-Za-z0-9_-]`; identifica a tentativa e permite repetir o envio sem duplicar |

Respostas:

| Código | Quando | Corpo |
|---|---|---|
| 201 | documento novo gravado; `Location: /solicitacoes/{id}/documentos/{docId}` | `DocumentoResponse` + `versaoConjuntoDocumental` e `documentosDisponiveis` |
| 200 | repetição da mesma tentativa (mesma chave, mesmo conteúdo, mesmo tipo documental) | o `DocumentoResponse` já existente; nada novo é gravado |
| 400 `REQUISICAO_INVALIDA` | `tipoDocumental` ausente/inválido (detalhe `tipoDocumental`); `arquivo` ausente, vazio, ilegível, PDF com senha ou com mais de 5 páginas (detalhe `arquivo`); mais de um arquivo (detalhe `arquivos`); `X-Chave-Envio` malformada (detalhe `chaveEnvio`); multipart malformado | erro padrão com `detalhes` |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido inexistente ou de outra paciente | idêntico nos dois casos |
| 409 `CONFLITO_ESTADO` | o pedido já saiu de rascunho | "Este pedido não pode mais receber documentos." |
| 409 `CHAVE_ENVIO_REUTILIZADA` | mesma chave com conteúdo ou tipo documental diferentes | nada é criado nem alterado |
| 413 `ARQUIVO_MUITO_GRANDE` | arquivo acima de 5 MB | a leitura para no limite |
| 415 `TIPO_NAO_SUPORTADO` | requisição que não é `multipart/form-data`, ou conteúdo que não é PDF/JPEG/PNG | |
| 415 `EXTENSAO_INCOMPATIVEL` | extensão ausente, não permitida ou diferente do conteúdo | |
| 422 `REGRA_DE_NEGOCIO` | o pedido já tem 3 documentos disponíveis (detalhe `documentos`) | nada é gravado |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco ou área de arquivos indisponível/não configurada | sem detalhe técnico; a paciente pode repetir |

Garantias: recusa, repetição e falha **não** alteram revisão nem contagem e **não** deixam arquivo na área privada. Sucesso só depois do `COMMIT`. Cada envio novo cria a revisão seguinte com o motivo `DOCUMENTO_ENVIADO` e **não** inicia análise: o pedido continua `RASCUNHO`.

Exemplo de sucesso (201):

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
  "simulacao": true,
  "versaoConjuntoDocumental": 2,
  "documentosDisponiveis": 1
}
```

## `GET /solicitacoes/{id}/documentos`

Lista os documentos **disponíveis** do pedido, na ordem de envio.

- 200: `{ "documentos": [DocumentoResponse…], "total": 2, "maximo": 3, "versaoConjuntoDocumental": 3 }`; lista vazia é sucesso (`"documentos": []`, `"total": 0`).
- 401, 403, 404 (pedido inexistente ou alheio), 503.

## `GET /solicitacoes/{id}/documentos/{docId}`

Baixa o arquivo, como **anexo**.

- 200: conteúdo exatamente como guardado; `Content-Type` vindo do formato real conferido (`application/pdf`, `image/jpeg`, `image/png`), `Content-Disposition: attachment` com o nome saneado, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`, `Content-Length`. O nome interno de armazenamento não aparece em lugar algum.
- 404 `NAO_ENCONTRADO`: documento inexistente, alheio, de outro pedido ou com `docId` mal formado — resposta idêntica em todos os casos.
- 401, 403, 503 (área de arquivos não configurada, arquivo ausente ou erro de leitura).

## Acréscimos à resposta do pedido (`GET`, `POST` e `PUT /solicitacoes…`)

Aditivos; nenhum campo existente muda:

```json
{
  "revisao": 3,
  "versaoConjuntoDocumental": 3,
  "documentosDisponiveis": 2,
  "analise": {
    "situacao": "NAO_REALIZADA",
    "mensagem": "Nenhuma análise foi realizada para esta versão dos documentos."
  }
}
```

`versaoConjuntoDocumental` é a revisão atual do pedido. Upload concluído **não** significa conferência nem análise.

## Métodos

Outros métodos nas rotas acima respondem 405 com `Allow`: `POST, GET` na coleção; `GET` no documento. Remoção e substituição de documento **não** existem nesta história (`DELETE` responde 405).

## Limites

3 documentos disponíveis por pedido · 5 MB por arquivo · PDF até 5 páginas · JPEG/PNG até 25 milhões de pixels · nome de exibição até 100 caracteres · lista sem paginação (no máximo 3 itens).
