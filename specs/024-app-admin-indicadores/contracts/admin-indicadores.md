# Contrato: `GET /admin/indicadores` (RC-20)

Fonte publicada: `docs/api/openapi.yaml` (este arquivo é o desenho da feature 024).

- **Autorização**: sessão de `ADMINISTRADOR` (`Authorization: Bearer <token>`). Sem sessão válida → 401 `NAO_AUTENTICADO`; `PACIENTE` → 403 `ACESSO_NEGADO` (antes de qualquer consulta). Só `GET`; outro método do administrador → 405 `METODO_NAO_PERMITIDO`.
- **Falha do banco**: 503 `DEPENDENCIA_INDISPONIVEL`. Erros no formato de `docs/api/erros.md`.
- **Conteúdo**: só agregados e dados do protocolo. Nunca nome, e-mail, identificador de paciente ou pedido, arquivo, trecho lido, resumo ou mensagem de pendência.

## 200

```json
{
  "dadosSinteticos": true,
  "aviso": "Indicadores agregados de demonstracao, calculados sobre dados sinteticos. Nao representam pacientes, filas ou servicos reais.",
  "consultadoEm": "2026-11-18T20:00:00Z",
  "protocolosAtivos": [
    {
      "protocoloId": "PROTOCOLO-ACADEMICO-FICTICIO-v0.1",
      "identificador": "PROTOCOLO-ACADEMICO-FICTICIO",
      "versao": "v0.1",
      "quantidadeRegras": 6,
      "regras": [
        { "codigo": "RC-R01", "descricao": "Encaminhamento presente" }
      ]
    }
  ],
  "totalPedidos": 12,
  "analisesPorResultado": {
    "semPendenciasIdentificadas": 3,
    "comPendencias": 5,
    "requerConferencia": 2
  },
  "pedidosEmFalhaAnalise": 1,
  "regrasComMaisPendencias": [
    { "codigo": "RC-R02", "descricao": "Identificação coerente entre documentos", "total": 4, "pendentes": 3, "naoVerificaveis": 1 }
  ]
}
```

| Campo | Regra |
|---|---|
| `protocolosAtivos` | protocolos com `ATIVO = 1`, ordem por identificador e versão; regras na ordem do protocolo; lista vazia se não houver |
| `totalPedidos` | todos os pedidos, qualquer estado |
| `analisesPorResultado` | análises com `VIGENTE = 1` por resultado geral; as três chaves sempre presentes (zero quando não houver) |
| `pedidosEmFalhaAnalise` | pedidos com estado atual `FALHA_ANALISE` (a análise que falha não é gravada); pode sobrepor um grupo acima |
| `regrasComMaisPendencias` | pendências (`PENDENTE` + `NAO_VERIFICAVEL`) das análises vigentes por regra; só total > 0; ordem total desc, código asc; no máximo 5 |
| `consultadoEm` | instante UTC da consulta |

## Uso no app (E4.2)

| Elemento da tela | Origem |
|---|---|
| "Protocolo acadêmico fictício · versão {versão} · {n} regras" | `versao` (sem o "v"), `quantidadeRegras` |
| Lista das regras | `regras[].descricao` (o `codigo` não é exibido desde 19/11/2026; spec, Clarifications) |
| "Total de pedidos" | `totalPedidos` |
| "Análises por resultado" | `analisesPorResultado.*` e `pedidosEmFalhaAnalise` ("Falha", explicada em texto) |
| Nota de demonstração | texto fixo do app (`mobile/design/textos.md`); o `aviso` da API não é exibido (o backend escreve sem acento, como suas demais mensagens) |
| "Regras com mais pendências" | `regrasComMaisPendencias[]`: `descricao` e `total`; abaixo, as parcelas `pendentes` e `naoVerificaveis` maiores que zero (desde 19/11/2026; o `codigo` não é exibido) |
