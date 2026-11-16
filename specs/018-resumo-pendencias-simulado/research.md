# Research: RC-14

- **Decisão**: reutilizar `RC_ANALISE.RESUMO_TEXTO/RESUMO_ORIGEM`. **Razão**: esquema já prevê a RC-14. **Alternativa**: tabela separada, desnecessária para um resumo por análise.
- **Decisão**: itens estruturados derivados das pendências gravadas. **Razão**: referência a regra e ação verificável. **Alternativa**: texto livre do gerador, que poderia inventar regras.
- **Decisão**: origem `PADRAO` para fallback recuperável. **Razão**: permite repetir a geração. **Alternativa**: falha HTTP sem orientação.
