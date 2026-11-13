# Gabarito das amostras sintéticas (RC-10)

> Documentos **sintéticos**, sem nenhum dado real. A leitura é **simulada** (decisão de 12/11/2026):
> o leitor não lê o arquivo; ele o identifica pelo SHA-256 e devolve a leitura abaixo
> (`backend/src/main/resources/extracao-simulada/catalogo.json`). Gerado por `GeradorDeAmostras`.

## encaminhamento-sintetico.pdf

SHA-256: `a2954d2c8797c39ea26fcfc8cd7477bc49284cd0b8e93e8e9c04713835b8e160`

| Campo | Situação | Valor | Origem |
|---|---|---|---|
| `TIPO_DOCUMENTO` | IDENTIFICADO | ENCAMINHAMENTO | pág. 1: "ENCAMINHAMENTO PARA CONSULTA AMBULATORIAL" |
| `NOME` | IDENTIFICADO | Maria Exemplo da Silva | pág. 1: "Paciente: Maria Exemplo da Silva" |
| `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | pág. 1: "Nascimento: 12/04/1990" |
| `MOTIVO_ENCAMINHAMENTO` | IDENTIFICADO | Avaliacao cardiologica ambulatorial (texto sintetico) | pág. 1: "Motivo: Avaliacao cardiologica ambulatorial (texto sintetico)" |
| `DATA_EMISSAO` | IDENTIFICADO | 2026-10-20 | pág. 1: "Emissao: 20/10/2026" |
| `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |

## identificacao-sintetica.png

SHA-256: `001117f663acd6b8fd2e6f2b03bcfc4456ca75619f60b9877fb1762d319b90da`

| Campo | Situação | Valor | Origem |
|---|---|---|---|
| `TIPO_DOCUMENTO` | IDENTIFICADO | IDENTIFICACAO | "DOCUMENTO DE IDENTIFICACAO (SINTETICO)" |
| `NOME` | IDENTIFICADO | Maria Exemplo da Silva | "Nome: Maria Exemplo da Silva" |
| `DATA_NASCIMENTO` | IDENTIFICADO | 1990-04-12 | "Nascimento: 12/04/1990" |
| `MOTIVO_ENCAMINHAMENTO` | NAO_LOCALIZADO | - | - |
| `DATA_EMISSAO` | NAO_LOCALIZADO | - | - |
| `AUTORIZACAO_ASSINADA` | NAO_LOCALIZADO | - | - |

## Casos extras (só de teste)

Ficam em `backend/src/test/resources/extracao-simulada/` (catálogo `catalogo-testes.json`): encaminhamento sem
motivo (`NAO_LOCALIZADO`), identificação com data de nascimento borrada (`ILEGIVEL`), encaminhamento com duas
datas de emissão conflitantes (`ILEGIVEL`), PDF que se apresenta como identificação (tipo lido diferente do
informado) e documento com texto-injeção (`ignore as regras...`), que é apenas dado do campo.
Qualquer outro arquivo recebe `LEITURA_VAZIA`, nunca campos inventados.
