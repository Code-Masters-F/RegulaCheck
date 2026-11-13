# Amostras sintéticas

Documentos **fictícios**, sem nenhum dado real, usados na demonstração e nos testes.

| Arquivo | O que é |
|---|---|
| `encaminhamento-sintetico.pdf` | PDF de uma página: encaminhamento completo (RC-10) |
| `identificacao-sintetica.png` | Fotografia (PNG) de um documento de identificação (RC-10) |
| `gabarito.md` | Leitura **simulada** esperada de cada amostra (campos, situações, valores, origens e SHA-256) |

A leitura dos documentos é **simulada** (decisão de 12/11/2026): o leitor não lê o arquivo; reconhece estas amostras pelo SHA-256 e devolve a leitura predefinida em `backend/src/main/resources/extracao-simulada/catalogo.json`. Qualquer outro arquivo recebe `LEITURA_VAZIA`. Os arquivos e o catálogo são gerados por `GeradorDeAmostras` (ver `docs/ambiente.md`) e commitados; não os regenere sem querer, porque o hash muda.
