# Entrega

Pacote da Global Solution 2026/2 (grupo CodeMasters).

| Item | Arquivo |
|---|---|
| Dados do grupo e link do pitch | [`entrega.txt`](entrega.txt) |
| PDF com os entregáveis de cada parte | [`RegulaCheck-entregaveis.pdf`](RegulaCheck-entregaveis.pdf) |
| Gerador do PDF | [`gerar_pdf.py`](gerar_pdf.py): `uv run python -X utf8 entrega/gerar_pdf.py`, da raiz do repositório |
| Montagem do ZIP | [`montar_zip.py`](montar_zip.py): `uv run python -X utf8 entrega/montar_zip.py`; gera `RegulaCheck-CodeMasters.zip` uma pasta acima do repositório |

O ZIP não leva `specs/`, `.specify/`, `entrega/prints/` (as capturas só alimentam o PDF), `.env` nem builds. Execução do projeto: README da raiz.
