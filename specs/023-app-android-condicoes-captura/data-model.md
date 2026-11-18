# Data Model: App Android — condições da captura (RC-22, parte do app)

Tudo existe só em memória no app. O modelo persistido (sessão e leituras) é o da [spec 016](../016-iot-simulada/data-model.md).

## Modelos do contrato (`api/Modelos.kt`)

| Modelo | Campos usados | Observação |
|---|---|---|
| `SessaoCaptura` | `id`, `criada?`, `aberta?`, `iniciadaEm?` | Resposta 201/200 de `POST …/sessoes-captura`. 201 e 200 são tratados igualmente |
| `Telemetria` | `consultadaEm`, `prazoAtualidadeSegundos?`, `sessao?`, `sensores` | `sessao` vem omitido sem sessão |
| `SensorTelemetria` | `tipo`, `unidade`, `situacao`, `ultimaLeitura?`, `orientacao?`, `faixa?` | Valores desconhecidos de `situacao` são tratados como `SEM_LEITURA` |
| `UltimaLeitura` | `valor`, `medidaEm` | Vem omitida sem leitura |
| `OrientacaoSensor` | `codigo`, `mensagem` | A mensagem é exibida como texto puro |

## Apresentação (`ui/captura/ApresentacaoCaptura.kt`)

**`SituacaoSensorUi`**: `ADEQUADA`, `ADEQUADA_DE_NOVO`, `BAIXA`, `ALTA`, `FORA_DO_IDEAL`, `DESATUALIZADA`, `SEM_LEITURA`, `INDISPONIVEL`.

| Situação da API | Tipo | Situação na tela | Rótulo | Ícone | Orientação |
|---|---|---|---|---|---|
| `DENTRO_DA_FAIXA` | qualquer | `ADEQUADA`; `ADEQUADA_DE_NOVO` se esteve fora nesta visita | "Adequada" / "Adequada de novo" | Atende (verde) | — / "Condições adequadas de novo." |
| `ABAIXO_DA_FAIXA` | luminosidade | `BAIXA` | "Baixa" | Alerta (laranja) | API |
| `ACIMA_DA_FAIXA` | luminosidade | `ALTA` | "Alta" | Alerta | API |
| `ABAIXO_DA_FAIXA`/`ACIMA_DA_FAIXA` | distância | `FORA_DO_IDEAL` | "Fora do ideal" | Alerta | API |
| `DESATUALIZADA` | qualquer | `DESATUALIZADA` | "Desatualizada" | Relógio | API |
| `SEM_LEITURA` / desconhecida | qualquer | `SEM_LEITURA` | "Sem leitura" | Círculo com traço | API |
| (tela indisponível) | qualquer | `INDISPONIVEL` | "Sensores indisponíveis agora" | Círculo com traço | só no 1º: "Você pode anexar e salvar seus documentos normalmente." |

**`ApresentacaoSensor`**: `tipo`, `nome`, `valor` ("410 lux", "31,5 cm" ou "—"), `situacao`, `rotulo`, `orientacao?` e `linhaLeitura`, que pode ser:

- "Leitura há {t} · sensor simulado";
- "Leitura há {t} · antes: {v} · sensor simulado";
- "Última leitura há {t} · sensor simulado";
- "Sem leitura disponível · sensor simulado".

**`ApresentacaoCaptura`**: `sensores` (luminosidade, distância), `indisponivel` e `mesmoAssim` (indisponível, ou algum sensor desatualizado ou sem leitura).

**`MemoriaCaptura`** (por visita): `foraDaFaixa: Map<tipo, valor>` e `recuperados: Map<tipo, valorAnterior>`.

Transições por consulta bem-sucedida:

- **DENTRO**: se o tipo está em `foraDaFaixa`, ele sai de lá e vai para `recuperados` com o valor anterior; se já estava em `recuperados`, continua lá.
- **ABAIXO/ACIMA**: `foraDaFaixa[tipo] = valor` e o tipo sai de `recuperados`.
- **DESATUALIZADA/SEM_LEITURA**: o tipo sai de `recuperados`; `foraDaFaixa` é mantido.

Uma falha de consulta não altera a memória.

## Estado da tela (`CondicoesCapturaViewModel`)

```text
Carregando ──POST ok / rede ou 5xx────▶ Exibindo(apresentação) ◀──GET a cada 4 s──┐
    │                                        │                                 │
    │ 401/403/404/409 (POST)                 │ 401/403/404 (GET)               │
    ▼                                        ▼                                 │
Encerrada(erro) ◀────────────────────────────┘           (rede/5xx → Exibindo(indisponível))
```

`novaVisita()` volta a `Carregando`, zera a memória e libera um novo pedido de sessão. Em todos os estados `Exibindo`, a ação de seguir está disponível.
