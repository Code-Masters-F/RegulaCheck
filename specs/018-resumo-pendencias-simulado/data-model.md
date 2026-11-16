# Data model: RC-14

- `Analise` (existente): id, pedido, revisão, vigente, resultados e pendências imutáveis. `RESUMO_TEXTO` opcional, até 2000 caracteres; `RESUMO_ORIGEM` opcional (`ADAPTADOR_MOCKADO` ou `PADRAO`).
- `Resumo` (resposta): texto, origem, simulado=true, itens. Cada item contém código de regra, ação e responsável derivados da pendência gravada.
- Estado: ausente → gerado ou padrão; padrão → gerado em nova tentativa; gerado → sem alteração por repetição. Ao perder vigência, o resumo permanece no histórico da análise.
