# API: submissão simulada (RC-17)

Rotas privadas de `PACIENTE`; ordem de `docs/api/erros.md`: 401 sem sessão → 403 para administrador e simulador → 404 idêntico (pedido inexistente, de outra paciente ou id ilegível) → 409/503. A submissão é **simulada**: nada é enviado ao SUS, não há fila real nem número oficial de protocolo.

## `POST /solicitacoes/{id}/submissoes-simuladas` — concluir a simulação

Sem corpo (corpo enviado é ignorado).

- **201** — submissão registrada agora; o pedido passa a `SUBMETIDA_SIMULACAO` (só consulta).
- **200** — o pedido já tinha submissão: o mesmo comprovante, nada novo registrado.

```json
{
  "solicitacaoId": "12",
  "referencia": "SIM-2026-000012",
  "submetidaEm": "2026-11-17T14:45:03.120Z",
  "simulada": true,
  "destaque": "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS",
  "servico": {
    "localidade": "Município Exemplo (fictício)",
    "especialidade": "Consulta ambulatorial em cardiologia",
    "avisoFicticio": "Protocolo acadêmico fictício…"
  },
  "versaoAnalisada": {
    "analiseId": "31",
    "revisao": 6,
    "protocolo": { "identificador": "PROTOCOLO-ACADEMICO-FICTICIO", "versao": "v0.1" }
  },
  "nota": "Este comprovante é de demonstração. Não é documento clínico, não é protocolo oficial e não coloca você em fila real."
}
```

### Recusas (nada é registrado, o estado não muda)

| HTTP | Código | Quando | Mensagem |
|---|---|---|---|
| 409 | `CONFLITO_ESTADO` | análise vigente com pendência ou critério não verificável | "A simulação só pode ser concluída quando a análise vigente não aponta pendências nem critérios não verificáveis. Esta é uma restrição do simulador acadêmico e não impede atendimento em serviço de saúde." |
| 409 | `CONFLITO_ESTADO` | sem análise vigente da versão atual (rascunho, nunca analisado, falha, documentos ou conferência mudaram) | "Não há análise válida para a versão atual dos documentos. Verifique o pedido antes de concluir a simulação. Esta é uma restrição do simulador acadêmico e não impede atendimento em serviço de saúde." |
| 409 | `CONFLITO_ESTADO` | análise em andamento (`PROCESSANDO`) | "A verificação deste pedido está em andamento. Aguarde o resultado antes de concluir a simulação." |
| 409 | `CONFLITO_ESTADO` | estado `SUBMETIDA_SIMULACAO` sem submissão registrada | "Este pedido já foi submetido e só pode ser consultado." |
| 503 | `DEPENDENCIA_INDISPONIVEL` | banco indisponível | genérica, sem detalhe técnico |

## `GET /solicitacoes/{id}/submissoes-simuladas` — consultar o comprovante

- **200** — o mesmo comprovante da conclusão.
- **404** `NAO_ENCONTRADO` — pedido inexistente, de outra paciente **ou ainda não submetido** (resposta idêntica).
- **503** — banco indisponível.

## Depois da submissão

Pedido em `SUBMETIDA_SIMULACAO` é só consulta (comportamento das RC-08 a RC-21, já implementado): enviar, substituir e remover documento, excluir, ler, conferir, analisar, verificar a versão corrigida e abrir sessão de captura → 409 "Este pedido já foi submetido e só pode ser consultado." Consultar pedido, documentos, leitura, conferência, análises e comprovante continua funcionando.
