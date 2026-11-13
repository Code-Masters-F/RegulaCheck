# Data Model: Substituir documento e excluir rascunho (RC-09)

## Mudança de esquema

**Nenhuma.** Não há script `08_`. Tudo o que a história precisa já existe:

| Necessidade | Já existe em |
|---|---|
| Documento removido/substituído deixa de estar disponível | `RC_DOCUMENTO.REVISAO_REMOCAO` (nula = disponível), FK composta para `RC_REVISAO` e `CHECK (REVISAO_REMOCAO IS NULL OR REVISAO_REMOCAO > REVISAO_ENVIO)` |
| Motivos de revisão | `RC_REVISAO_CK_MOTIVO` já aceita `DOCUMENTO_SUBSTITUIDO` e `DOCUMENTO_REMOVIDO` |
| Invalidar análise anterior | `RC_ANALISE.VIGENTE` |
| Apagar um rascunho | FKs `RC_DOCUMENTO → RC_REVISAO → RC_SOLICITACAO`; apagar nessa ordem |

## Tabelas usadas

| Tabela | Operação |
|---|---|
| `RC_SOLICITACAO` | bloqueada com `FOR UPDATE` em toda operação; `REVISAO_ATUAL` e `ATUALIZADA_EM` atualizados em remover/substituir; linha apagada em excluir |
| `RC_REVISAO` | uma linha nova por remover (`DOCUMENTO_REMOVIDO`) ou substituir (`DOCUMENTO_SUBSTITUIDO`); todas apagadas em excluir |
| `RC_DOCUMENTO` | remover: `REVISAO_REMOCAO = nova revisão` e `CHAVE_ENVIO = NULL` na linha; substituir: remove a antiga (igual) e insere a nova; excluir: todas as linhas do pedido apagadas |
| `RC_ANALISE` | `UPDATE … SET VIGENTE = 0 WHERE SOLICITACAO_ID = ? AND VIGENTE = 1` em remover/substituir (zero linhas hoje) |

## Regras de estado

| Operação | Estados aceitos | Estado depois |
|---|---|---|
| remover documento | todos, **exceto** `SUBMETIDA_SIMULACAO` | o mesmo (nunca muda o estado) |
| substituir documento | todos, **exceto** `SUBMETIDA_SIMULACAO` | o mesmo |
| excluir pedido | **somente** `RASCUNHO` | pedido deixa de existir |

`SUBMETIDA_SIMULACAO` → 409 `CONFLITO_ESTADO` ("Este pedido já foi submetido e só pode ser consultado."). Excluir fora de rascunho e fora de submetido → 409 `CONFLITO_ESTADO` ("Somente rascunhos podem ser excluídos.").

Remover/substituir **não** inicia análise e **não** move o pedido para `PROCESSANDO` (ver `research.md` D6).

## Invariantes (verificadas por teste)

1. Documento disponível ⇔ `REVISAO_REMOCAO IS NULL`; disponíveis por pedido ≤ 3, inclusive sob remover/substituir/enviar simultâneos.
2. Cada remoção ou substituição cria **exatamente uma** revisão, com número seguinte, sem salto nem repetição, gravada na mesma transação da mudança.
3. Substituir mantém a contagem de disponíveis; remover reduz em um.
4. Documento removido ou substituído: não aparece na lista, não abre (404), sem `CHAVE_ENVIO`; seu arquivo é apagado depois do commit.
5. Recusa (arquivo novo inválido, estado, 404, chave reutilizada, falha) não altera revisão, contagem, documento antigo nem deixa arquivo novo guardado.
6. Excluir rascunho deixa **zero** linhas em `RC_DOCUMENTO`, `RC_REVISAO` e `RC_SOLICITACAO` do pedido e zero arquivos dos seus documentos.
7. Nenhum documento disponível sem arquivo (exceto as linhas da carga sintética, que nunca tiveram) e nenhum arquivo de documento disponível ou removido ausente do registro.
8. `(pedido, chave)` vivo é único; chave de documento removido fica livre.

## Entidades (Java)

| Tipo | Mudança |
|---|---|
| `Documento` | sem mudança (record da RC-08) |
| `ResultadoRegistro` (RC-08) | ganha a situação `SUBMETIDO`; continua sendo o desfecho do envio. Na **substituição** é reaproveitado **dentro** de `ResultadoSubstituicao` (`REGISTRADO`, `REPETIDO`, `NAO_ENCONTRADO`, `SUBMETIDO`, `CONFLITO_CHAVE`; `LIMITE` não se aplica) |
| `ResultadoSubstituicao` (novo, record) | embrulha o `ResultadoRegistro` e carrega o documento **antigo** (`substituido`), para o Service apagar o arquivo dele depois do commit |
| `DocumentoService.ResultadoRemover` (novo, record) | versão e contagem devolvidas pelo Service ao controller, que as converte em `RemocaoResponse`; o `ResultadoRemocao` do repositório não sai da camada de serviço |
| `ResultadoRemocao` (novo, record, repositório) | `situacao` (`REMOVIDO`, `NAO_ENCONTRADO`, `SUBMETIDO`), `documento` removido (para apagar o arquivo), `versaoConjuntoDocumental`, `documentosDisponiveis` |
| `ResultadoExclusao` (novo, record) | `situacao` (`EXCLUIDO`, `NAO_ENCONTRADO`, `FORA_DE_RASCUNHO`, `SUBMETIDO`, `COM_DEPENDENCIAS`), `arquivos` (lista de `IdInterno` a apagar depois do commit) |

## DTOs

- **Remover** (`DELETE …/{docId}`, 200): `{ "versaoConjuntoDocumental": 4, "documentosDisponiveis": 2 }`.
- **Substituir** (`PUT …/{docId}`, 201/200): o mesmo `DocumentoEnviadoResponse` da RC-08 (documento novo + `versaoConjuntoDocumental` + `documentosDisponiveis`).
- **Excluir** (`DELETE /solicitacoes/{id}`, 204): sem corpo.
- Nenhum DTO novo expõe caminho, hash, chave ou nome interno.
