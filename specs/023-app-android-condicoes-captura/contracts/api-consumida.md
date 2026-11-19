# Contrato consumido pelo app — RC-22 (parte do app)

O contrato completo está em [api-iot.md](../../016-iot-simulada/contracts/api-iot.md) (spec 016), em `docs/api/openapi.yaml` (`SessaoCaptura` e `Telemetria`) e em `docs/api/erros.md`. Esta tabela registra só o que o app consome e como reage. O envio do arquivo depois da E4.1 usa a mesma rota da RC-19 ([api-consumida 022](../../022-app-android-documentos-resultado/contracts/api-consumida.md)).

| # | Rota | Quando | Resposta → reação do app |
|---|---|---|---|
| 1 | `POST /solicitacoes/{id}/sessoes-captura` (sem corpo, token da paciente) | uma vez ao abrir a E4.1, ou ao voltar do segundo plano sem sessão confirmada | 201 ou 200 → segue para a consulta · 401 → login (sessão apagada) · 403 → "sem permissão" · 404 → "Pedido não encontrado." · 409 → "Este pedido já foi submetido e só pode ser consultado." + "Voltar" · rede, tempo ou 5xx → "indisponível" e segue só com a consulta (2) |
| 2 | `GET /solicitacoes/{id}/leituras` (token da paciente) | ao abrir e depois a cada 4 s, só enquanto a E4.1 está visível; nunca duas ao mesmo tempo | 200 → apresentação por sensor (data-model). Os dois sensores `SEM_LEITURA` → "indisponível" · 401/403/404 → como em (1), e a consulta para · rede, tempo, 5xx ou corpo fora do contrato → "indisponível", e a consulta continua no ritmo normal |

Garantias: o app não envia identificador da paciente nem a chave do simulador; sem retentativa automática do OkHttp; sem log de cabeçalho ou corpo; `orientacao.mensagem` é exibida como texto puro.
