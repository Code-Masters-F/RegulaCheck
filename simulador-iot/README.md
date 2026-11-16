# Simulador IoT — estação de captura (RC-21 + RC-22)

Simula os **dois sensores** da estação de captura do RegulaCheck e envia as leituras à API em **JSON por HTTP**. Não há hardware: os valores são tabelas fixas, **demonstrativas e sem calibração real**.

> **Limite importante:** as leituras só orientam a paciente a melhorar a iluminação ou ajustar a distância antes de fotografar o documento. Elas **não comprovam que o arquivo ficou legível** e **nunca bloqueiam** o envio de documentos, o salvamento ou a exclusão do rascunho. Todos os dados são sintéticos.

## O que cada sensor mede

| Sensor | Mede | Unidade | Faixa demonstrativa (inclusiva) | Limite físico aceito pela API |
|---|---|---|---|---|
| `LUMINOSIDADE` | Iluminação sobre o documento | lux (`LUX`) | 300 a 1000 lux | 0 a 200000 lux |
| `DISTANCIA` | Distância entre a câmera e o documento | centímetros (`CM`) | 20 a 40 cm | maior que 0 e até 500 cm |

- **Fora da faixa** a leitura é aceita e gravada normalmente; só muda a orientação: abaixo de 300 lux → *melhorar a iluminação*; acima de 1000 lux → *reduzir a iluminação ou o reflexo*; abaixo de 20 cm → *afastar a câmera*; acima de 40 cm → *aproximar a câmera*.
- **Fora do limite físico** (valor implausível para o sensor) a API recusa com 400.
- **Leitura desatualizada:** medida há mais de **60 s** aparece como `DESATUALIZADA` e não gera orientação de correção (orienta a verificar se a estação está ligada).
- O instante da medição pode estar no máximo **30 s** à frente do relógio do servidor (diferença de relógios).
- Os valores ficam num único ponto do backend: `backend/src/main/java/br/com/regulacheck/iot/service/FaixasDemonstrativas.java`. Mudou lá, mude aqui.

## Pré-requisitos

- **JDK 21** (o mesmo do backend). Não precisa de Maven nem de dependências: o simulador é um único arquivo executado pelo *source launcher* do Java.
- API no ar com a variável **`IOT_API_KEY`** definida (ver [docs/ambiente.md](../docs/ambiente.md)). Sem ela a API recusa todo envio com 401.
- Uma **sessão de captura aberta**, criada pela **paciente** dona do pedido:

  ```http
  POST /solicitacoes/{id}/sessoes-captura
  Authorization: Bearer <token da paciente>
  ```

  A resposta (201, ou 200 se a sessão já estava aberta) traz o `id` da sessão, que vai em `--sessao`. Cada pedido tem no máximo uma sessão aberta.

## Como executar

A partir da raiz do repositório. A chave vem **só** da variável `IOT_API_KEY` (nunca por argumento, para não ficar no histórico do terminal) e nunca é impressa.

Bash:

```bash
export IOT_API_KEY='<a mesma chave configurada na API>'
java simulador-iot/SimuladorSensores.java --sessao 1 --sequencia fora-da-faixa
```

PowerShell:

```powershell
$env:IOT_API_KEY = '<a mesma chave configurada na API>'
java simulador-iot/SimuladorSensores.java --sessao 1 --sequencia recuperacao
```

| Argumento | Padrão | Para quê |
|---|---|---|
| `--sessao <id>` | obrigatório | Sessão de captura aberta (id devolvido pela API) |
| `--sequencia normal\|fora-da-faixa\|recuperacao` | `normal` | Qual tabela de valores enviar |
| `--intervalo-ms <n>` | `1000` | Espera entre um envio e o próximo |
| `--api <url>` | variável `API_URL` ou `http://localhost:8080` | Endereço da API (`http://` ou `https://`) |
| `--seco` | — | Só imprime as leituras (`tipo valor unidade`), sem conectar e sem chave |

## Sequências (reproduzíveis)

Cada par envia primeiro a luminosidade e depois a distância. A mesma sequência sempre envia os mesmos valores, na mesma ordem; só o instante da medição muda (é o instante real do envio). Confira sem enviar nada com `--seco`.

| Sequência | Pares (lux, cm) | O que a paciente vê |
|---|---|---|
| `normal` | (450, 30) (520, 28) (610, 32) (480, 26) (550, 35) | Condição adequada nos dois sensores |
| `fora-da-faixa` | (120, 55) (90, 12) (150, 60) (80, 15) (110, 58) | Melhorar a iluminação; aproximar ou afastar a câmera |
| `recuperacao` | (100, 60) (180, 50) (250, 45) (350, 38) (500, 30) (650, 28) | Começa com orientação de correção e termina em condição adequada |

Depois de cada sequência, a paciente consulta `GET /solicitacoes/{id}/leituras` para ver a situação e a orientação de cada sensor. Para ver o estado *desatualizada*, espere mais de 60 s sem enviar e consulte de novo.

## Saída e códigos de saída

- Uma linha por leitura: `LUMINOSIDADE 120 LUX -> 201`.
- **Nunca repete** um envio: a primeira resposta diferente de 201 (por exemplo `-> 401 NAO_AUTENTICADO`, `-> 404 NAO_ENCONTRADO`) ou falha de rede interrompe a sequência.

| Código | Significado |
|---|---|
| 0 | Todas as leituras aceitas (201) |
| 1 | Envio interrompido na primeira recusa ou falha de rede |
| 2 | Argumentos inválidos ou `IOT_API_KEY` ausente |

## Contrato e testes

- Contrato das rotas: [specs/015-iot-simulada/contracts/api-iot.md](../specs/015-iot-simulada/contracts/api-iot.md); formatos em [docs/api/contratos-dados.md](../docs/api/contratos-dados.md).
- O backend executa este arquivo como processo nos testes (`SimuladorSensoresProcessoTest`): `--seco` reproduzível, envio real contra a API de teste e parada na primeira recusa.
- Decisões: MQTT foi descartado (12/11/2026); o simulador fica fora do módulo Maven do backend (uma aplicação, um módulo). Detalhes em [specs/015-iot-simulada/research.md](../specs/015-iot-simulada/research.md).

## O que ainda não existe

A **tela** do app que mostra os alertas da estação de captura depende do Épico 6 (app Android). Ela deve consumir `GET /solicitacoes/{id}/leituras` e usar `orientacao.codigo` (contrato estável) para ícone e texto.
