# Contrato: análises do pedido (RC-13)

Campos nulos ficam **fora** do JSON (convenção da API): "omitido" abaixo significa que a chave não aparece. Rotas privadas, só perfil `PACIENTE` (`RotasPrivadas`: 401 sem sessão válida, 403 para outro perfil). A paciente é sempre a da sessão. Identificadores saem como texto decimal opaco. Instantes em ISO-8601 UTC com milissegundos. **A leitura dos documentos é simulada e o protocolo é acadêmico e fictício**; toda análise traz `simulada: true` e o `aviso`.

## `POST /solicitacoes/{id}/analises`

Sem corpo (o corpo, se vier, é ignorado). Pede a análise da versão atual dos documentos com a conferência atual e espera a conclusão.

| Status | Quando | Corpo |
|---|---|---|
| **201** | análise feita e gravada agora; virou a vigente e o pedido foi para o resultado geral | `Analise` |
| **200** | já existe análise vigente da versão atual dos documentos e da conferência atual; nada foi gravado | `Analise` (a existente) |
| 404 `NAO_ENCONTRADO` | pedido inexistente, de outra paciente ou id mal formado (mesma resposta) | erro padrão |
| 409 `CONFLITO_ESTADO` | pedido submetido ("Este pedido já foi submetido e só pode ser consultado."); sem leitura da versão atual ("Leia os documentos antes de pedir a análise."); documentos ou conferência mudaram durante a análise ("Os documentos ou a conferência mudaram durante a análise. Confira e peça a análise de novo."); análise passou do limite e foi assumida por outra ("A análise passou do tempo limite e não foi publicada. Consulte as análises ou peça de novo.") | erro padrão |
| 409 `ANALISE_EM_ANDAMENTO` | outra análise do pedido está em andamento dentro do limite de 1 minuto; cabeçalho `Retry-After` com os segundos restantes | erro padrão |
| 503 `ANALISE_INDISPONIVEL` | a avaliação falhou (versão do protocolo não aplicável ou erro inesperado); nada gravado; pedido em `FALHA_ANALISE`; pode tentar de novo | erro padrão |
| 503 `DEPENDENCIA_INDISPONIVEL` | banco indisponível (ou sem o script `10_`); nada gravado; se o início ficou gravado, o limite de 1 minuto libera o pedido | erro padrão |

**Ordem de verificação**: 401 → 403 → 404 (dona) → 409 submetido → 409 `ANALISE_EM_ANDAMENTO` → 200 (vigente) → 409 sem leitura atual → avaliação → 409 (entrada mudou / assumida) | 503 → 201.

**Efeitos do 201**, numa transação: grava a análise, um resultado por regra, as pendências com motivo e evidências; a anterior vigente (se houver) deixa de ser vigente; `ESTADO` = resultado geral. Nenhum resumo é gerado (RC-14).

## `GET /solicitacoes/{id}/analises`

Histórico do pedido, da análise mais recente para a mais antiga (até 50). Pedido nunca analisado → 200 com lista vazia.

```json
{
  "solicitacaoId": "12",
  "estado": "COM_PENDENCIAS",
  "versaoConjuntoDocumental": 6,
  "vigenteId": "31",
  "limite": 50,
  "analises": [ { "...": "Analise (vigente: true)" }, { "...": "Analise (vigente: false)" } ]
}
```

- `execucao`: omitida fora de `PROCESSANDO`; senão `{ "situacao": "EM_ANDAMENTO" | "INTERROMPIDA", "iniciadaEm": "…", "limiteEm": "…" }`. `INTERROMPIDA` = passou do limite sem concluir; a paciente pode pedir de novo.
- `vigenteId`: id da análise vigente; omitido quando nenhuma vale para a versão atual (no exemplo acima, a vigente é a `"31"`).
- 404 e 503 como no `POST`.

## `Analise`

```json
{
  "id": "31",
  "revisao": 6,
  "vigente": true,
  "executadaEm": "2026-11-14T13:05:10.123Z",
  "protocolo": { "identificador": "PROTOCOLO-ACADEMICO-FICTICIO", "versao": "v0.1" },
  "conferenciaConsiderada": "2026-11-14T13:04:58.001Z",
  "dataReferencia": "2026-11-14",
  "resultadoGeral": "COM_PENDENCIAS",
  "simulada": true,
  "aviso": "Análise simulada: leitura simulada dos documentos e protocolo acadêmico fictício. \"Sem pendências identificadas\" não é aprovação do SUS.",
  "documentos": [
    { "documentoId": "41", "nomeArquivo": "encaminhamento-sintetico.pdf", "tipoDeclarado": "ENCAMINHAMENTO", "disponivel": true }
  ],
  "resultados": [
    { "codigoRegra": "RC-R01", "descricao": "…", "resultado": "ATENDE" },
    { "codigoRegra": "RC-R04", "descricao": "…", "resultado": "PENDENTE" },
    { "codigoRegra": "RC-R06", "descricao": "…", "resultado": "NAO_APLICAVEL" }
  ],
  "pendencias": [
    {
      "codigoRegra": "RC-R04",
      "descricaoRegra": "…",
      "tipo": "PENDENTE",
      "explicacao": "O encaminhamento não traz o motivo…",
      "responsavel": "UNIDADE_EMISSORA",
      "evidencias": [
        { "documentoId": "41", "campo": "MOTIVO_ENCAMINHAMENTO", "situacao": "NAO_LOCALIZADO" }
      ]
    }
  ]
}
```

| Campo | Regra |
|---|---|
| `revisao` | versão do conjunto documental avaliada (= `versaoConjuntoDocumental` da época) |
| `executadaEm` | data e hora de conclusão da execução |
| `conferenciaConsiderada`, `dataReferencia` | omitidos só nas análises da carga sintética |
| `resultados[]` | um por regra da versão, na ordem da versão; `motivo` só em `NAO_VERIFICAVEL` (omitido nas análises da carga, que não o têm) |
| `pendencias[]` | uma por regra `PENDENTE` ou `NAO_VERIFICAVEL`; `motivo` só em `NAO_VERIFICAVEL`; `responsavel` ∈ {`PACIENTE`, `UNIDADE_EMISSORA`} |
| `evidencias[]` | documento, campo e situação (`CONFIRMADO`, `NAO_LOCALIZADO`, `ILEGIVEL`, `NAO_CONFERIDO`, `DIVERGENCIA_APONTADA`, `DOCUMENTO_NAO_LIDO`); `origem: {pagina, trecho}` só quando a leitura daquela revisão tem origem para o campo. **Nenhum valor lido aparece aqui.** |
| `documentos[]` | documentos da revisão avaliada; `disponivel: false` se depois foram substituídos ou removidos |
| resumo | não existe nesta história: a RC-14 acrescenta o campo |

Mensagens fixas, sem diagnóstico, urgência ou promessa de vaga. `SEM_PENDENCIAS_IDENTIFICADAS` não é aprovação do SUS.

## Efeitos em outras rotas

- **Pedido** (`GET/POST/PUT /solicitacoes…`): `analise.situacao` ∈ `NAO_REALIZADA` (nenhuma análise vale para esta versão), `EM_ANDAMENTO` (estado `PROCESSANDO`), `CONCLUIDA` (com `analise.resultadoGeral`). Campo `resultadoGeral` aditivo, só em `CONCLUIDA`.
- **Conferência** (`PUT …/conferencia`): quando grava alguma decisão nova ou diferente, a análise vigente do pedido deixa de ser vigente (histórico preservado). Resposta e estado do pedido inalterados.
- **Documentos** (RC-09): já tiravam a vigência; agora isso aparece na consulta das análises.
