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

## Feature 014 — Fase 0 (T001–T025), 13/11/2026

Escopo: ajustes do design system e dos exemplos antes de desenhar as histórias RC-36 a RC-39 (`specs/014-fluxo-telas-app/tasks.md`).

**Executado**
- Commit `9f8a6c6` enviado para `origin/codex/007-design-system-exploracao` antes da fase; capturas de terceiros em `design/referencias/*.webp` mantidas fora do repositório público.
- Capas das quatro faixas (x=5200; y=0, 1200, 2400, 3600) e exemplos movidos para suas posições do inventário (`H0BJi` x=8960, `CvQRm` x=8490/y=1200, `XCNVm` x=14130/y=2400), com nota de operação abaixo de cada exemplo.
- Componentes (A1–A13): F4 com "Em verificação" e "Falha na verificação" e nova seção `Comparação/*`; "Sair" no `Cabeçalho/Marca`; `Pedido/Linha` e `Pedido/Destaque` com número e data numa linha (linha com seta); `Documento/Linha` com nome do arquivo e "Mais opções", sem ícone decorativo; novos `Campo/Senha`, `Folha/Escolha`, `Pedido/Próximo passo`, `Linha/Navegação`, `Dado/Linha`, `Botão/Decisão`, `Conferência/*` (7), `Critério/Linha`, `Aviso/Resultado não vigente`. `Extração/*` removidos (sem instâncias). Total: 71 componentes v1.0; 0 instâncias `Legado/*` nos quadros v1.0.
- Varredura de valores não imersivos em F1–F4 e capas: 9 textos trocados por exemplares (`ana.moreira@email.test`, "Santa Aurora (município fictício)", "Pedido N"); nenhum resíduo.
- Exemplos (X1–X3): E1.2 com Pedidos 4 (destaque), 2 e 1 e "Olá, Ana", desenhada com rolagem (904 de altura, linha "dobra · 844"); E2.2 do Pedido 5 com arquivos exemplares e ação principal "Ler documentos"; E3.4 do Pedido 2 com RC-R04, origem e "Abrir documento".
- Documentos (X4–X7): `mobile/design/dados-ficticios.md` e `textos.md` reescritos; nota de substituição em `specs/007-epico-telas-pendev/contracts/telas.md`; `design/README.md`, `mobile/design/INDEX.md` e `design-system.md` atualizados.

**Verificado**: consulta de limites com instâncias resolvidas — 0 problemas em F1–F4, capas e nos três exemplos (desconsiderados a saudação oculta de `Tela/Título`, o `Respiro flexível` com altura zero na tela com rolagem e a linha absoluta da dobra); screenshots revisadas; prévias exportadas (`design/previas/*.png` 2×, `design/previas/fundamentos/*.png` 1×); documento salvo pelo MCP.

**Ajustes durante a execução**: nome do arquivo passou a 13 e saiu o ícone decorativo para caber numa linha; botões de decisão usam a pergunta "Confere com o seu documento?" com "Está correto" / "Está diferente" para caberem lado a lado (spec FR-039 atualizada); E1.2 virou tela com rolagem porque os três pedidos da carga não cabem em 844.

**Não verificado**: fonte ampliada, TalkBack, interação real; nenhum teste Java/Android (sem mudança de código).

## Feature 014 — US1 / RC-36 (T026–T031), 14/11/2026

- Commit da Fase 0: `988ac68` (enviado).
- 25 quadros na faixa RC-36: E1.1 Login (5), E1.2 Meus pedidos (5, incluindo `H0BJi`), E1.3 Pedido (13: carregando, 4 situações de rascunho, 7 estados verificados, não encontrado), E1.4 e E1.5.
- Consulta de limites: 0 problemas; 0 componentes `Legado/*`; todos com aviso acadêmico; nenhuma ação fora de `specs/014-fluxo-telas-app/contracts/telas-operacoes.md`; ação principal de cada E1.3 confere com FR-012 e há no máximo uma por quadro.
- Correções feitas na revisão: rascunho com "Mais opções" no cabeçalho (excesso de altura); texto de E1.4 que afirmava algo não garantido pelo backend foi trocado pelo texto aprovado.
- Não verificado: compreensão por pessoa que não desenhou (SC-003), fonte ampliada, TalkBack, interação.

## Feature 014 — US2 / RC-37 (T032–T038), 14/11/2026

- Commit da US1: `f724cd6` (enviado).
- 28 quadros na faixa RC-37: E2.1 Novo pedido (5), E2.2 Documentos (8, incluindo `CvQRm`), E2.7 Anexar (2), E2.3 Recusa de arquivo (8, uma por resposta do backend: 415 ×2, 413, 400 ×3, 422, 409), E2.4/E2.5 (2 + 2) e E2.6 (1).
- Consulta de limites: 0 problemas reais (avisos restantes vêm de elementos ocultos por padrão); 0 componentes `Legado/*`; aviso acadêmico em todos; "Anexar" nunca aparece fora de rascunho; no máximo uma ação principal por quadro.
- Correções na revisão: nome longo truncado com reticências; terceiro documento `exames-anteriores.pdf`; "Não enviado" com "Tentar de novo" como única ação; linha de limites removida onde a mensagem já a cobre (3 quadros).
- Não verificado: fonte ampliada, TalkBack, interação.

## Feature 014 — US3 / RC-38 leitura e conferência (T039–T042), 14/11/2026

- Commit da US2: `0eb5296` (enviado).
- 12 quadros: E3.1 Leitura simulada (lendo, indisponível, sem documentos, documentos mudaram) e E3.2 Conferência (documento lido, decisões registradas, não localizado indicado, campo ilegível, documento não lido, texto do documento como dado, desatualizada, erro ao salvar).
- Telas de conferência desenhadas inteiras com marca da dobra em 844; capa da RC-39 movida para y=5000.
- Consulta de limites: 0 problemas; 0 `Legado/*`; selo SIMULADO em todos; nenhum campo de digitação; decisões oferecidas só as aceitas pelo backend (RC-11).
- Não verificado: fonte ampliada, TalkBack, interação.

## Feature 014 — US4 / RC-38 verificação e pendências (T043–T046), 14/11/2026

- Commit da US3: `74cdfd3` (enviado).
- 10 quadros: E3.8 Verificar critérios, E3.3 Resultado (5) e E3.4 Detalhe da pendência (`XCNVm` + 3).
- Critérios e pendências conferidos com `database/scripts/02_carga_sintetica.sql` (Pedidos 1 e 2) e `docs/protocolo-academico.md`.
- Consulta de limites: 0 problemas; 0 `Legado/*`; selo SIMULADO onde há resumo; negação de aprovação do SUS no resultado sem pendências.
- Marco: percurso P1 (US1–US4, RC-36 a RC-38) desenhado em 14/11/2026, antes do marco de 16/11.
- Não verificado: compreensão por pessoa que não desenhou (SC-003, SC-004), fonte ampliada, TalkBack.
