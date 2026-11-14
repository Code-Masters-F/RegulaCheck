# Design system RegulaCheck — v1.0 (13/11/2026)

Fundamentos refeitos a partir das três telas de exemplo aprovadas (Meus pedidos, Documentos e Pendência). Direção: minimalismo simpático inspirado no Headspace, sem copiar a marca. Fonte nativa: `design/RegulaCheck.pen` (somente pelo MCP `pencil`). Retomada e IDs: `design/README.md`. Rastreabilidade: épico 007, recorte T044–T047.

**Situação:** fundamentos e catálogo v1.0 desenhados e revisados visualmente no pen.dev; três telas refeitas com os componentes. **Não** comprova acessibilidade em Android (fonte ampliada, TalkBack, teclado/insets), nem cobre os 73 estados do épico.

## Quadros no pen.dev

| Quadro | ID | Conteúdo |
|---|---|---|
| F1 · Fundamentos | `IkCIw` | Princípios, cor, tipografia, espaço/forma/toque, ícones, faça e evite |
| F2 · Componentes | `t9lvSH` | Ações, campos, estrutura de tela, objetos do percurso, sensores, retorno |
| F3 · Confiança e simulação | `EVIof` | Aviso acadêmico, selo SIMULADO, resumo e comprovante simulados, linguagem |
| F4 · Estados | `HBByt` | 7 estados do pedido, 4 do arquivo, 4 resultados por critério |
| Arquivo · biblioteca anterior | `Wo9WQ` | F1–F4 e prévia antigos; componentes com prefixo `Legado/`. Não usar. Em 14/11/2026 não estava mais no documento aberto; provavelmente saiu no commit `0e4cdb6` (recuperável de `efc76d1`); a remoção fica para o grupo confirmar |

Prévias PNG: `design/previas/fundamentos/` (quadros) e `design/previas/` (telas).

## Princípios

1. **Uma ação por contexto.** O azul conduz o passo principal; apoio em contorno ou texto.
2. **Hierarquia antes de caixas.** Espaço e tipografia agrupam; superfície neutra só para a tarefa atual ou uma lista.
3. **Estado é ícone + texto.** A cor reforça, nunca carrega sozinha o significado.
4. **Transparência visível.** Aviso acadêmico em toda tela; SIMULADO em leitura, resumo, comprovante e sensores.

## Cor

Duas camadas de variáveis: primitivos (`rc-blue-50…900`, `rc-orange-50…900`, `rc-neutral-ref`) e **tokens semânticos**, os únicos usados em telas.

| Token | Valor | Uso | Contraste |
|---|---|---|---|
| `rc-text` | `#112B3C` | Títulos, corpo, rótulos de estado | 14,64:1 branco · 12,73:1 neutro |
| `rc-text-muted` | `#526774` | Apoio, metadados, rodapé | 5,91:1 branco · 5,14:1 neutro |
| `rc-action` / `rc-on-action` | `#205375` / `#FFFFFF` | Ação principal, links, foco | 8,22:1 |
| `rc-action-pressed` | `#112B3C` | Botão pressionado | 14,64:1 com branco |
| `rc-accent` | `#F6680E` | Barra e marcador decorativos | 3,04:1 branco · 2,64:1 neutro — **nunca texto ou ícone isolado** |
| `rc-pending` | `#B44809` | Ícone de pendência e conferência | 5,42:1 branco · 4,72:1 neutro |
| `rc-success` | `#176448` | Ícone de atendido / enviado | 7,11:1 · 6,18:1 |
| `rc-error` / `rc-error-bg` | `#B42335` / `#FDEDF0` | Erro, falha, ação destrutiva | 6,50:1 branco · 5,74:1 no fundo de erro |
| `rc-simulated-fg` / `-bg` | `#8D3C0C` / `#FFF1E6` | Selo SIMULADO e comprovante | 6,82:1 |
| `rc-surface` | `#EFEFEF` | Superfície da tarefa (substitui `#F7F9FB`) | — |
| `rc-info-bg` | `#EAF2F8` | Aviso de entrada, sem conexão | texto 12,93:1 · apoio 5,23:1 |
| `rc-border` | `#82929B` | Borda de campo | 3,21:1 (não textual, ≥ 3:1) |
| `rc-divider` / `rc-skeleton` | `#DAE2E7` | Divisores e esqueleto | decorativo |
| `rc-focus` | `#205375` | Anel de foco 2 | 8,22:1 |
| `rc-scrim` | `#112B3C` a 40% | Véu de diálogo | — |

Método: luminância relativa sRGB, `(L1+0,05)/(L2+0,05)`, sobre os valores sólidos. Fora da v1.0: roxo de conferência (`rc-review`), fundos coloridos por estado, laranja em botões e branco sobre laranja.

## Tipografia

Bricolage Grotesque 600 só em títulos; Nunito Sans no resto. Duas famílias no máximo. Tokens de tamanho `rc-size-*`.

| Papel | Especificação | Token | Exemplo |
|---|---|---|---|
| Título de tela | Bricolage 600 · 32/1,15 | `rc-size-display` | Meus pedidos |
| Título de seção / item em destaque | Bricolage 600 · 22/1,2 | `rc-size-heading` | Motivo do encaminhamento |
| Título de item (listas, superfícies) | Bricolage 600 · 18/1,25 | `rc-size-title` | Cardiologia |
| Abertura | Nunito 400 · 18/1,4 · apoio | `rc-size-lead` | Retome de onde parou. |
| Corpo | Nunito 400 · 16/1,5 | `rc-size-body` | Texto de explicação |
| Ação e ênfase | Nunito 700 · 16/1,4 | `rc-size-body` | Ver pendências |
| Metadado / estado | Nunito 400–700 · 14/1,4 | `rc-size-meta` | PED-0004 · Atualizado em… |
| Legenda | Nunito 400 · 13/1,4 | `rc-size-caption` | Protótipo acadêmico · dados fictícios |
| Selo | Nunito 800 · 12 · caixa-alta · +0,6 | `rc-size-badge` | SIMULADO |

Consolidação feita nas telas: títulos 18/20/21/22 → 18 ou 22; corpo 17/18 → 16 (abertura 18 só no início); metadados 13 → 14.

## Espaço, forma e toque

- Escala: 4, 8, 12, 16, 20, 24, 32, 40, 48 (`rc-space-*`). Tela 390 × 844: margem lateral 24 (`rc-screen-margin`), topo 24, rodapé 16; largura útil 342.
- Ritmo: 24 entre blocos, 8–12 dentro de um grupo, 32 antes de nova seção.
- Superfície da tarefa: `rc-surface`, raio 24 (`rc-radius-task`), padding 20 (`rc-surface-padding`), sem borda, sombra ou aninhamento.
- Raios: 8 marcadores, 16 campos, 24 superfícies/diálogos, cápsula (999) botões e selo.
- Controles: altura 52, alvo mínimo 48 × 48, borda de campo 1,5 (`rc-stroke-control`), foco 2 (`rc-stroke-focus`) com afastamento 3. **A altura 52 é numérica:** o pen.dev descarta variável em `height`; `rc-control-height` documenta o valor.
- Elevação: nenhuma. Diálogo sobre `rc-scrim`.
- Ícones: Lucide em traço; 16 em estados, 20 em ações, 24 em navegação; sempre com rótulo visível.

## Componentes (F2, F3, F4)

| Grupo | Componentes |
|---|---|
| Ações | `Botão/Primário`, `/Pressionado`, `/Carregando`, `/Desabilitado`, `/Contorno`, `/Texto`, `/Adicionar`, `/Destrutivo` (contorno vermelho), `/Voltar` (48 × 48); anel de foco como composição |
| Campos | `Campo/Padrão`, `/Foco`, `/Erro`, `/Desabilitado`, `Campo/Senha`, `Seletor/Padrão`, `Opção/Selecionada`, `Opção/Não selecionada`, `Folha/Escolha` (folha inferior sobre `rc-scrim`) |
| Estrutura | `Cabeçalho/Marca` (com "Sair"), `Cabeçalho/Voltar` (selo SIMULADO oculto por padrão), `Tela/Título` (saudação oculta por padrão), `Pedido/Próximo passo`, `Linha/Navegação`, `Dado/Linha` |
| Percurso | `Pedido/Destaque`, `Pedido/Linha` (número e data numa linha; seta, a linha toda abre), `Documento/Linha` (tipo, nome do arquivo, formato, "Abrir" e "Mais opções"), `Pendência/Linha`, `Pendência/Orientação`, `Critério/Linha` |
| Conferência | `Botão/Decisão`; `Conferência/Identificado` (sem decisão · confirmado · com diferença), `Conferência/Não localizado` (· indicado), `Conferência/Ilegível`, `Conferência/Documento não lido`. Pergunta "Confere com o seu documento?" com "Está correto" / "Está diferente"; nunca campo de digitação |
| Sensores e indicadores | `Sensor/Na faixa`, `/Fora da faixa`, `/Indisponível`, `Indicador/Total` |
| Retorno | `Retorno/Erro`, `Retorno/Sem conexão`, `Aviso/Resultado não vigente`, `Carregando/Esqueleto`, `Vazio/Pedidos`, `Diálogo/Confirmação` |
| Confiança (F3) | `Aviso/Rodapé`, `Aviso/Entrada`, `Selo/Simulado`, `Resumo/Simulado`, `Comprovante/Simulado` |
| Estados (F4) | `Estado/*` (7 do pedido; `PROCESSANDO` = "Em verificação", `FALHA_ANALISE` = "Falha na verificação"), `Arquivo/*` (enviando, enviado, não enviado; "aguardando" não é usado), `Critério/*` (atende, pendente, não verificável, não se aplica), `Comparação/*` (resolvida, continua, nova) |

"Conferência" nomeia só a etapa em que a paciente confere a leitura; a análise dos critérios é a "verificação". Os componentes `Extração/*` da primeira versão foram removidos em 13/11, substituídos por `Conferência/*`.

Estados: ícone 16 na cor do significado + rótulo 14/700 em `rc-text`. Rótulos na linguagem da paciente; o código da API (`COM_PENDENCIAS` etc.) aparece só na documentação. Mensagens e próxima ação por estado estão em F4 e em `textos.md`.

## Comportamentos

- Uma ação principal por contexto; o laranja não concorre com ela.
- Durante envio, `Botão/Carregando` bloqueia novo toque; “Enviado” só após confirmação do servidor; sem percentual inventado.
- Erro e sem conexão dizem o que aconteceu, o que foi preservado e o próximo passo; fundo colorido só nesses dois casos.
- Exclusão sempre por `Diálogo/Confirmação`, com “Cancelar” em destaque e consequência nomeada.
- Documento clínico não é editável; ausência de informação clínica é resolvida pela unidade emissora. “Não localizado” não prova ausência.
- Sensores: valores demonstrativos (200–1000 lux; 15–40 cm), idade da leitura visível, indisponibilidade nunca bloqueia envio.
- Sem barra de navegação inferior no percurso da paciente (linear, volta pelo cabeçalho), decisão registrada na feature 014 (FR-011).
- Telas de lista mais altas que 844 são desenhadas inteiras, com a linha "dobra · 844" marcando o que aparece sem rolar.
- Fonte ampliada: o texto acompanha a escala do sistema (até 200%, escala não linear do Android 14). Quando o texto não cabe, `Documento/Linha` e `Critério/Linha` passam as ações e o resultado para baixo do nome, e `Resumo/Simulado` põe o selo abaixo do título. Nunca cortar palavra ou esconder ação. Exemplos: quadros E4.4.
- Exemplos nos quadros: estrutura do backend, textos realistas em português correto (`mobile/design/dados-ficticios.md`); nunca "Exemplo", "sintético" ou texto sem acento.

## Faça e evite

- Faça: estados com o mesmo padrão em toda lista; uma superfície neutra por tela; texto concreto; rodapé acadêmico em toda tela.
- Evite: pílulas coloridas repetidas, cartões aninhados, sombras, gradientes, ilustração decorativa, tamanhos fora da escala, prometer aprovação, vaga, prazo ou envio real ao SUS.
