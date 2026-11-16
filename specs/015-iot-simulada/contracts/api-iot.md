# Contrato: rotas de IoT simulada (RC-21 + RC-22)

Formato de erro: o padrão do projeto (`codigo`, `mensagem`, `detalhes[]`) em [docs/api/erros.md](../../../docs/api/erros.md). Instantes em ISO-8601 UTC. Todas as respostas de telemetria são **simuladas** e os limites são **demonstrativos**.

## 1. Abrir sessão de captura — `POST /solicitacoes/{id}/sessoes-captura`

- **Quem**: `PACIENTE` (token de sessão); dona do pedido.
- **Corpo**: nenhum.
- **201 Created** (sessão nova) / **200 OK** (já havia sessão aberta), `Location: /solicitacoes/{id}/leituras`:

```json
{
  "id": 42,
  "solicitacaoId": 7,
  "iniciadaEm": "2026-11-16T14:00:00Z",
  "aberta": true,
  "criada": true,
  "simulada": true
}
```

- **Erros**: 401 sem sessão; 403 administrador ou chave do simulador; 404 `NAO_ENCONTRADO` pedido inexistente ou de outra paciente (idênticos); 409 `CONFLITO_ESTADO`; 503 `DEPENDENCIA_INDISPONIVEL` banco fora.

## 2. Enviar leitura — `POST /sessoes-captura/{id}/leituras`

- **Quem**: simulador, cabeçalho `X-Chave-Simulador: <IOT_API_KEY>`.
- **Corpo** (`application/json`, até 4 KB):

```json
{
  "sessaoId": 42,
  "tipo": "LUMINOSIDADE",
  "valor": 350.0,
  "unidade": "LUX",
  "medidaEm": "2026-11-16T14:00:05Z"
}
```

- **201 Created**:

```json
{
  "id": 901,
  "sessaoId": 42,
  "tipo": "LUMINOSIDADE",
  "valor": 350.0,
  "unidade": "LUX",
  "medidaEm": "2026-11-16T14:00:05Z",
  "recebidaEm": "2026-11-16T14:00:05.120Z"
}
```

- **Erros** (nada é gravado):

| Status | Código | Quando |
|---|---|---|
| 401 | `NAO_AUTENTICADO` | sem chave, chave errada, ou chave não configurada no servidor |
| 403 | `ACESSO_NEGADO` | conta (paciente/admin) autenticada sem a chave |
| 404 | `NAO_ENCONTRADO` | sessão inexistente, encerrada, ou de pedido inexistente (idênticos) |
| 400 | `REQUISICAO_INVALIDA` | corpo inválido; `detalhes[].campo` ∈ `sessaoId`, `tipo`, `unidade`, `valor`, `medidaEm` |
| 415 | `TIPO_NAO_SUPORTADO` | corpo não é `application/json` |
| 503 | `DEPENDENCIA_INDISPONIVEL` | banco fora |

Ordem das conferências: 401/403 → 404 (sessão, antes de ler o corpo) → 415/400 de formato → 400 de conteúdo.

Regras do 400: `sessaoId` ≠ `{id}`; tipo fora de `LUMINOSIDADE`/`DISTANCIA`; unidade incompatível (`LUX` só com luminosidade, `CM` só com distância); valor ausente, não numérico, luminosidade fora de 0–200000 ou distância fora de (0, 500]; `medidaEm` ausente, sem fuso ou mais de 30 s no futuro.

## 3. Consultar telemetria — `GET /solicitacoes/{id}/leituras`

- **Quem**: `PACIENTE`; dona do pedido.
- **200 OK** (sempre os dois sensores, na ordem luminosidade, distância):

```json
{
  "solicitacaoId": 7,
  "sessao": { "id": 42, "iniciadaEm": "2026-11-16T14:00:00Z", "aberta": true },
  "consultadaEm": "2026-11-16T14:00:30Z",
  "prazoAtualidadeSegundos": 60,
  "simulada": true,
  "aviso": "Leituras simuladas, com limites demonstrativos e sem calibração. Elas não comprovam a legibilidade do arquivo e não impedem o envio de documentos.",
  "sensores": [
    {
      "tipo": "LUMINOSIDADE",
      "unidade": "LUX",
      "faixa": { "minimo": 300, "maximo": 1000 },
      "ultimaLeitura": { "valor": 120.0, "medidaEm": "2026-11-16T14:00:20Z" },
      "situacao": "ABAIXO_DA_FAIXA",
      "orientacao": { "codigo": "MELHORAR_ILUMINACAO", "mensagem": "Ilumine melhor o documento antes de capturar." }
    },
    {
      "tipo": "DISTANCIA",
      "unidade": "CM",
      "faixa": { "minimo": 20, "maximo": 40 },
      "ultimaLeitura": null,
      "situacao": "SEM_LEITURA",
      "orientacao": { "codigo": "CAPTURA_SEGUE_SEM_SENSOR", "mensagem": "Sem leitura do sensor. Você pode capturar e enviar o documento normalmente." }
    }
  ]
}
```

- `sessao` é **omitido** quando o pedido não tem sessão (os dois sensores ficam `SEM_LEITURA`); `ultimaLeitura` é **omitido** quando o sensor não tem leitura. A API omite campos nulos (mesmo padrão da RC-13); o `null` do exemplo acima é só ilustrativo.
- `situacao` ∈ `DENTRO_DA_FAIXA`, `ABAIXO_DA_FAIXA`, `ACIMA_DA_FAIXA`, `DESATUALIZADA`, `SEM_LEITURA`.
- `orientacao.codigo` ∈ `CONDICAO_ADEQUADA`, `MELHORAR_ILUMINACAO`, `REDUZIR_ILUMINACAO_OU_REFLEXO`, `AFASTAR_DOCUMENTO`, `APROXIMAR_DOCUMENTO`, `VERIFICAR_ESTACAO`, `CAPTURA_SEGUE_SEM_SENSOR`. Os textos são fixos e podem ser ajustados na implementação; o **código** é o contrato para a tela do Épico 6.
- **Erros**: 401; 403 administrador ou chave do simulador; 404 pedido inexistente ou de outra paciente (idênticos); 503 banco fora.

## Simulador (linha de comando)

```text
java simulador-iot/SimuladorSensores.java --sessao <id> [--sequencia normal|fora-da-faixa|recuperacao]
     [--intervalo-ms 1000] [--api http://localhost:8080] [--seco]
Variáveis: IOT_API_KEY (obrigatória, exceto em --seco), API_URL (opcional)
--seco: imprime `tipo valor unidade` por leitura, sem instante e sem conexão (saída reproduzível).
Saída 0: todas as leituras com 201. Saída 1: primeira falha (sem repetir). Saída 2: argumentos inválidos.
```
