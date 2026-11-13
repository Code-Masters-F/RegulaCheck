# Contrato da API: extração das informações dos arquivos (RC-10)

Rotas privadas, **somente perfil `PACIENTE`** (401 sem sessão, 403 para administrador e simulador). A paciente é sempre a da sessão. Dados sintéticos. **A leitura é simulada**: toda resposta traz `simulada: true` e um aviso fixo; o sistema não lê arquivos de verdade e não afirma leitura real.

## Ordem de verificação

401 → 403 → 404 (pedido inexistente ou de outra paciente: resposta idêntica) → 409 (pedido submetido, **mesmo que já exista leitura**) → 422 (sem documentos) → 200 (já existe leitura para esta versão) → leitura → 409 (revisão mudou durante a leitura) → 503. O `POST` não tem corpo.

## `POST /solicitacoes/{id}/extracoes` — ler os documentos

Lê os documentos **disponíveis** da versão atual e guarda o resultado.

| Código | Quando | Corpo |
|---|---|---|
| 201 | leitura nova gravada | `ExtracaoResponse` |
| 200 | já existe leitura para esta versão (nada novo é gravado, o leitor não é chamado de novo) | `ExtracaoResponse` |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido inexistente ou alheio | idêntico |
| 409 `CONFLITO_ESTADO` | pedido em `SUBMETIDA_SIMULACAO` ("Este pedido já foi submetido e só pode ser consultado."); ou os documentos mudaram durante a leitura ("Os documentos mudaram; tente de novo.") | nada é gravado |
| 422 `SEM_DOCUMENTOS` | o pedido não tem documento disponível | nada é criado |
| 503 `LEITURA_INDISPONIVEL` | falha do leitor ou saída fora do contrato; pode tentar de novo | sem detalhe técnico; nada é gravado |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco ou área de arquivos indisponível | sem detalhe técnico |

## `GET /solicitacoes/{id}/extracoes` — consultar a leitura

| Código | Quando | Corpo |
|---|---|---|
| 200 | existe leitura (da versão atual ou de uma anterior) | `ExtracaoResponse` com `situacao` `ATUAL` ou `DESATUALIZADA` |
| 401 / 403 | sem sessão / perfil indevido | erro padrão |
| 404 `NAO_ENCONTRADO` | pedido inexistente ou alheio; ou nunca houve leitura | idêntico |

## `ExtracaoResponse`

`id` é o identificador opaco da leitura (a conferência da transcrição, RC-11, usará `/solicitacoes/{id}/extracoes/{extracaoId}/conferencia`, como já previsto no `CONTRATO_API.md`). `GET` devolve a leitura **mais recente**; o histórico por revisão fica para a RC-13.

```json
{
  "id": "12",
  "situacao": "ATUAL",
  "versaoConjuntoDocumental": 4,
  "simulada": true,
  "aviso": "Leitura simulada com dados sintéticos; não é leitura real dos documentos.",
  "criadaEm": "2026-11-13T12:00:00Z",
  "documentos": [
    {
      "documentoId": 31,
      "tipoDocumental": "ENCAMINHAMENTO",
      "situacao": "LIDO",
      "campos": [
        { "codigo": "MOTIVO_ENCAMINHAMENTO", "situacao": "IDENTIFICADO", "valor": "…",
          "origem": { "pagina": 1, "trecho": "…" } },
        { "codigo": "DATA_EMISSAO", "situacao": "NAO_LOCALIZADO" },
        { "codigo": "NOME", "situacao": "ILEGIVEL" }
      ]
    },
    {
      "documentoId": 32,
      "tipoDocumental": "OUTRO",
      "situacao": "LEITURA_VAZIA",
      "motivo": "Não foi possível extrair informações deste arquivo.",
      "reenviar": true,
      "orientacao": "Envie um arquivo mais nítido.",
      "campos": []
    }
  ]
}
```

- `campos` de documento `LIDO`: os 6 códigos (`TIPO_DOCUMENTO`, `NOME`, `DATA_NASCIMENTO`, `MOTIVO_ENCAMINHAMENTO`, `DATA_EMISSAO`, `AUTORIZACAO_ASSINADA`), cada um em **uma** de `IDENTIFICADO` (com `valor` e, quando disponível, `origem`), `NAO_LOCALIZADO` ou `ILEGIVEL` (os dois sem `valor` e sem `origem`).
- `situacao` do documento: `LIDO`, `PROTEGIDO` ou `LEITURA_VAZIA`; os dois últimos trazem `motivo`, `reenviar: true`, `orientacao` e `campos` vazio.
- `origem.pagina` só em PDF; `origem.trecho` curto; o arquivo de origem é o próprio documento (`documentoId`). Valores e trechos são **dado**, nunca instrução.
- Campo repetido com valores conflitantes no documento sai `ILEGIVEL`. `tipoDocumental` (informado no envio) e o campo `TIPO_DOCUMENTO` (lido) são mostrados sem reconciliação.
- Nenhuma resposta traz caminho, hash, nome interno, resultado de completude, pendência ou estado novo do pedido.

## Garantias

- Uma leitura por versão do conjunto documental; repetir não duplica (inclusive sob concorrência).
- A leitura é gravada de uma vez ou nada; falha do leitor não deixa resultado parcial.
- Depois de remover/substituir/enviar documento (nova versão), a leitura anterior passa a `DESATUALIZADA` na consulta.
- A leitura não altera o estado do pedido, não cria revisão e não inicia análise.

## Métodos

`/solicitacoes/{id}/extracoes`: `GET, POST` (`Allow`); demais → 405.
