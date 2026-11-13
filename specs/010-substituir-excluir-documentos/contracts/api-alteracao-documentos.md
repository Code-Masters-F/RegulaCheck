# Contrato da API: remover, substituir e excluir (RC-09)

Rotas privadas, **somente perfil `PACIENTE`** (401 sem sessão, 403 para administrador e simulador). A paciente é sempre a da sessão. Dados sintéticos; o sistema só guarda, troca e apaga arquivos, sem lê-los.

## Ordem de verificação (todas as rotas)

401 → 403 → 404 (pedido e documento inexistentes, alheios, de outro pedido, já removidos ou com id mal formado: resposta idêntica) → 415/413/400 contrato (corpo, campos, arquivo, confirmação) → 409 estado ou chave reutilizada → 503. A existência do pedido é conferida **antes** de ler o corpo; no `PUT`, o documento do caminho também (corpo ruim ou arquivo inválido em documento removido, inexistente ou alheio continua 404, e nada é guardado). **Única exceção:** se `X-Chave-Envio` já corresponde a um documento vivo do pedido, a repetição é reconhecida sem procurar o `{docId}` (que, depois de uma substituição bem-sucedida, já não existe) e a decisão entre repetição (200) e conflito (409) vem depois da leitura do corpo.

## `DELETE /solicitacoes/{id}/documentos/{docId}` — remover documento

Sem corpo. O documento deixa de estar disponível, o arquivo guardado é apagado e a vaga é liberada.

| Código | Quando | Corpo |
|---|---|---|
| 200 | removido | `{ "versaoConjuntoDocumental": 4, "documentosDisponiveis": 2 }` |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido ou documento inexistente, alheio, de outro pedido, **já removido** ou `docId` malformado | idêntico em todos os casos |
| 409 `CONFLITO_ESTADO` | pedido em `SUBMETIDA_SIMULACAO` | "Este pedido já foi submetido e só pode ser consultado." |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco indisponível | sem detalhe técnico |

Garantias: uma revisão nova com motivo `DOCUMENTO_REMOVIDO`; análise vigente (se houver) deixa de ser vigente; o estado do pedido não muda; nenhuma análise é iniciada. Repetir o `DELETE` responde 404 e não altera versão nem contagem. Falha ao apagar o arquivo não desfaz a remoção.

## `PUT /solicitacoes/{id}/documentos/{docId}` — substituir documento

`multipart/form-data`, mesmas partes e regras do envio (RC-08):

| Parte / cabeçalho | Obrigatório | Regra |
|---|---|---|
| `arquivo` | sim | uma só; PDF, JPEG ou PNG pelo conteúdo; ≤ 5 MB; PDF ≤ 5 páginas e sem senha; imagem ≤ 25 milhões de pixels |
| `tipoDocumental` | sim | `ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL` ou `OUTRO` |
| `X-Chave-Envio` | não | 8 a 64 caracteres `[A-Za-z0-9_-]`; repetir a mesma tentativa não duplica |

| Código | Quando | Corpo |
|---|---|---|
| 201 | substituído; `Location: /solicitacoes/{id}/documentos/{novoDocId}` | `DocumentoResponse` do **novo** documento + `versaoConjuntoDocumental` + `documentosDisponiveis` |
| 200 | repetição da mesma tentativa (mesma chave, mesmo conteúdo e tipo) | o documento já existente; nada novo é gravado |
| 400 `REQUISICAO_INVALIDA` | `tipoDocumental` ausente/inválido (detalhe `tipoDocumental`); `arquivo` ausente, vazio, ilegível, PDF com senha ou > 5 páginas (detalhe `arquivo`); mais de um arquivo (`arquivos`); chave malformada (`chaveEnvio`); multipart malformado | erro padrão com `detalhes` |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido ou documento inexistente, alheio, de outro pedido, já removido ou `docId` malformado | idêntico em todos os casos |
| 409 `CONFLITO_ESTADO` | pedido em `SUBMETIDA_SIMULACAO` | "Este pedido já foi submetido e só pode ser consultado." |
| 409 `CHAVE_ENVIO_REUTILIZADA` | mesma chave com conteúdo ou tipo diferentes | nada é criado nem alterado |
| 413 `ARQUIVO_MUITO_GRANDE` | acima de 5 MB | a leitura para no limite |
| 415 `TIPO_NAO_SUPORTADO` / `EXTENSAO_INCOMPATIVEL` | não é `multipart/form-data`; conteúdo não é PDF/JPEG/PNG; extensão ausente ou incoerente | |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco ou área de arquivos indisponível | sem detalhe técnico |

Garantias: o documento antigo sai e o novo entra **na mesma transação**; **uma** revisão nova com motivo `DOCUMENTO_SUBSTITUIDO`; a contagem de disponíveis não muda (não há erro 422 de limite aqui); recusa ou falha deixa o antigo, a versão e os arquivos como estavam; o arquivo antigo é apagado só depois do `COMMIT`. Com chave, a repetição é reconhecida **antes** de procurar o `docId` (que, depois do sucesso, já não existe).

## `DELETE /solicitacoes/{id}` — excluir rascunho

Cabeçalho obrigatório: **`X-Confirmar-Exclusao: EXCLUIR`** (valor exato). Sem corpo.

| Código | Quando | Corpo |
|---|---|---|
| 204 | pedido, revisões, documentos e arquivos apagados | sem corpo |
| 400 `REQUISICAO_INVALIDA` | cabeçalho ausente, em branco ou com valor diferente (detalhe `confirmacao`) | **nada é apagado** |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido inexistente, já excluído ou de outra paciente | idêntico |
| 409 `CONFLITO_ESTADO` | pedido fora de `RASCUNHO`: "Este pedido já foi submetido e só pode ser consultado." (submetido) ou "Somente rascunhos podem ser excluídos." (demais); ou algo ainda ligado ao pedido impede a exclusão | nada é apagado |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco indisponível | sem detalhe técnico |

Garantias: a exclusão é uma transação (documentos → revisões → pedido); os arquivos são apagados **depois** do `COMMIT`; depois dela, `GET /solicitacoes/{id}` e as rotas de documento respondem 404.

## Métodos

`/solicitacoes/{id}`: `GET, PUT, DELETE` (`Allow`). `/solicitacoes/{id}/documentos`: `POST, GET` (sem mudança). `/solicitacoes/{id}/documentos/{docId}`: `GET, PUT, DELETE`. Demais métodos → 405 com `Allow`.

## Resposta do pedido

Sem campos novos. A partir desta história, `versaoConjuntoDocumental` e `documentosDisponiveis` mudam também em remover/substituir, e `analise.situacao` continua `NAO_REALIZADA` para a nova versão.
