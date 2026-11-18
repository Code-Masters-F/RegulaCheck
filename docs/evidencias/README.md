# Evidências

Somente evidências reais (saídas de comandos, capturas). Nenhuma evidência fictícia.

## Convenção

- **Por caso de teste** (plano em [../testes/plano-de-testes.md](../testes/plano-de-testes.md)): uma pasta `docs/evidencias/<ID-DO-CASO>/`, por exemplo `docs/evidencias/T06-02/`. A pasta só é criada quando o caso é executado (RC-25); o plano não cria nenhuma.
- **Conteúdo mínimo** da pasta: data e hora; versão ou commit da API e do app; comando ou passos executados (com o `id` do pedido criado); resultado bruto (saída de comando, resposta HTTP ou captura); status final e executor.
- **Nunca** registrar token, senha, chave do simulador, `DB_*` nem conteúdo de documento real.
- **Registros avulsos** de história (anteriores ao plano): `AAAA-MM-DD_<assunto>.<ext>`. Eles **não** substituem a execução de um caso.
