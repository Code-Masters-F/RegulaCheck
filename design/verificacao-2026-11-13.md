# Verificação da aplicação inicial — 13/11/2026

Escopo: identidade aprovada aplicada a três telas em `design/RegulaCheck.pen`, tarefas T040–T043 do épico 007. Este registro não aprova os 73 estados ou a usabilidade do aplicativo.

## Evidências

- MCP conectado; consulta ao estado e às instruções de schema/execute realizada.
- Quadros nativos `H0BJi`, `CvQRm`, `XCNVm`: textos/formas/instâncias editáveis, sem capturas rasterizadas como conteúdo das telas.
- Papéis tipográficos explícitos Bricolage Grotesque/Nunito Sans, seis componentes `Atual/*`, F1 atualizado. Biblioteca anterior preservada e identificada como em revisão.
- Screenshots analisadas e exportação PNG 2× em `design/previas/` (780 × 1688 por tela).
- Consulta `Get` com instâncias resolvidas: zero problemas de conteúdo parcialmente/totalmente cortado nas três telas e no conjunto de componentes atual. Um aviso inicial de dependência circular na altura do acento foi corrigido com altura explícita; nova consulta sem problema.
- Corpo base 16, apoio 13–14, selo SIMULADO 12; títulos de tela 32. Botões principais/contorno/link 52; Abrir e Voltar têm contêineres de pelo menos 48 de altura; Novo pedido tem 48.
- Aviso acadêmico nas três telas; SIMULADO no detalhe de pendência; responsáveis e limites clínicos explícitos.
- Abertura, ações e navegação são desenhos. Não foi executada API nem há interatividade comprovada.

## Contraste calculado

Método: luminância relativa sRGB, linearização por canal e razão `(Lmais clara+0,05)/(Lmais escura+0,05)`, arredondada a duas casas. Valores calculados sobre os tokens sólidos, não sobre pixels antisserrilhados das letras.

| Papel | Frente | Fundo | Razão |
|---|---|---|---|
| Texto principal / branco | `#112B3C` | `#FFFFFF` | 14.64:1 |
| Texto principal / superfície neutra | `#112B3C` | `#EFEFEF` | 12.73:1 |
| Texto secundário / branco | `#526774` | `#FFFFFF` | 5.91:1 |
| Texto secundário / superfície neutra | `#526774` | `#EFEFEF` | 5.14:1 |
| Branco / ação principal | `#FFFFFF` | `#205375` | 8.22:1 |
| Link / branco | `#205375` | `#FFFFFF` | 8.22:1 |
| Sucesso / neutro | `#176448` | `#EFEFEF` | 6.18:1 |
| Selo simulado / laranja claro | `#8D3C0C` | `#FFF1E6` | 6.82:1 |
| Marcador decorativo / neutro | `#F6680E` | `#EFEFEF` | 2.64:1 |

Os pares de texto usados são ≥4,5:1. O laranja vivo sobre neutro tem 2,64:1 e serve somente como acento decorativo acompanhado do estado por escrito; não usar esse par para texto ou ícone funcional isolado. Sucesso usa texto/ícone verde; botões e links usam azul.

## Rastreabilidade e limites

- Spec/plan/tasks do épico 007 reconciliados para o recorte autorizado; a pasta `design/` e a branch corrente prevalecem sobre a organização histórica em cinco arquivos.
- Skills Spec Kit consultadas; scripts oficiais `resolve-template.ps1 spec-template -Json` e `check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks` executados com override da feature 007 no processo. O script local preservado não aceita `-RequireSpec`; o comando foi ajustado após verificar os parâmetros, sem trocar o ponteiro global de 008.
- O checklist de qualidade de requisitos existente foi lido sem alterar marcações. A consistência foi avaliada para este recorte: requisitos de três quadros editáveis, tokens, limites clínicos, salvamento, exportação e continuidade correspondem a T040–T043; a matriz completa do épico não foi dada como coberta.
- Nenhum teste Java/Android executado neste recorte: somente design/documentação foram alterados. Não afirmar que o backend foi retestado.
- Não medidos: fonte ampliada, TalkBack, acessibilidade de interação, comportamento de Voltar, teclado/insets, restauração de rascunho, tempo de tarefa ou compreensão com pacientes.
- Aprovação do responsável: direção visual e sua transposição ao pen.dev. A revisão final dos quadros nativos e os critérios completos das histórias permanecem sujeitos à avaliação; não confundir aprovação da direção com validação de uso.


## Design system v1.0 — fundamentos refeitos (T044–T047)

Escopo: avaliação das três telas novas, refação de F1–F4 com base nelas e reconstrução das telas com os componentes. Somente design e documentação.

**Avaliação que motivou a refação**

- Telas: escala tipográfica dispersa (títulos 18/20/21/22; corpo 16/17/18; metadados 13/14); padding de superfície 14 em Documentos e 20 nas demais; três padrões diferentes de estado (ponto laranja, check verde em texto verde, texto cinza sem ícone); status "Enviado" empilhado sobre "Abrir"; "Novo pedido" em 18.
- Fundamentos anteriores: superfície `#F7F9FB` diferente do neutro `#EFEFEF` das telas; controles 56 em vez de 52; roxo de conferência, fundos coloridos e pílulas por estado (evitados na nova direção); biblioteca F2 e F4 em Plus Jakarta Sans; selo SIMULADO com outra cor/ícone.

**Executado**

- 25 tokens semânticos novos (cor, tamanho tipográfico, espaço, controle), com aliases para os primitivos; `rc-surface` passou a `#EFEFEF`. Total de 81 variáveis.
- Quadros novos F1 `IkCIw`, F2 `t9lvSH`, F3 `EVIof`, F4 `HBByt`; 56 componentes v1.0 (seis `Atual/*` reaproveitados e renomeados, mantendo IDs). Material anterior movido para `Wo9WQ`, componentes renomeados `Legado/*`; nada apagado além do grupo vazio "Identidade atual · componentes 13/11" (`EzfMo`), cujos componentes foram movidos.
- Telas `H0BJi`, `CvQRm`, `XCNVm` refeitas só com instâncias v1.0, corrigindo os pontos acima.
- Consulta `Get` com instâncias resolvidas: 0 problemas de recorte nas três telas; 1 aviso nos quadros, da saudação oculta por padrão em `Tela/Título` (esperado). Ícone `clock` inexistente no Lucide trocado por `clock-3`; altura por variável descartada pelo pen.dev, corrigida para 52 numérico.
- Prévias: `design/previas/*.png` (2×) e `design/previas/fundamentos/*.png` (1×). Documento salvo pelo MCP.

**Não verificado:** fonte ampliada, TalkBack, foco por teclado, insets, interação, compreensão com pacientes, demais estados do épico. Nenhum teste Java/Android executado (sem mudança de código).
