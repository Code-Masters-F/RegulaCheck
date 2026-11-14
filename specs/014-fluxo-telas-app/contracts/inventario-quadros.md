# Contrato de design: inventário de quadros e ordem de desenho

Inventário fechado: **94 quadros** em `design/RegulaCheck.pen`, 390 × 844, nomeados `<ID> <Tela> — <Estado>`. Substitui o inventário de `specs/007-epico-telas-pendev/contracts/telas.md` (73 quadros, anterior às specs 008–013). **†** = depende de operação contratada e ainda não implementada: o quadro leva uma nota "contrato provisório" ao lado. **Existe** = quadro já desenhado que precisa dos ajustes da Fase 0.

## Fase 0 — ajustes no design system e nos exemplos (pré-requisito)

### Componentes

| # | Ajuste | Motivo (FR / research) |
|---|---|---|
| A1 | F4: `Estado/Em conferência` → "Em verificação"; `Estado/Falha na conferência` → "Falha na verificação" | "conferência" é a etapa da paciente (data-model §2) |
| A2 | `Cabeçalho/Marca` com ação "Sair" à direita | FR-011, FR-024 |
| A3 | `Pedido/Linha` com "Atualizado em"; padrões "Pedido 12" em `Pedido/Destaque` e `Pedido/Linha` | FR-007, FR-022 |
| A4 | `Documento/Linha` com nome do arquivo e "Mais opções" (alvo 48) | FR-030 |
| A5 | `Folha/Escolha` (folha inferior: título, opções de escolha única, Cancelar) | FR-034, opções do documento |
| A6 | `Campo/Senha` (mostrar/ocultar, alvo 48) | FR-020 |
| A7 | `Botão/Decisão` (contorno compacto, altura 48) e `Conferência/Identificado` (sem decisão · confirmado · com diferença), `Conferência/Não localizado` (sem indicação · indicado), `Conferência/Ilegível`, `Conferência/Documento não lido` | FR-038, FR-039, D8 |
| A8 | `Pedido/Próximo passo` (superfície com estado, mensagem e ação principal) | FR-026 |
| A9 | `Linha/Navegação` (rótulo, situação, seta; linha inteira é o alvo) | FR-026 |
| A10 | `Critério/Linha` (nome do critério + marcador `Critério/*`) | FR-042 |
| A11 | F4: `Comparação/Resolvida`, `/Continua`, `/Nova` | FR-047 |
| A12 | `Dado/Linha` (rótulo + valor) | FR-025, FR-044, FR-049 |
| A13 | `Aviso/Resultado não vigente` (fundo `rc-info-bg`, ícone, texto, ação) | FR-046 |

### Exemplos e documentos

| # | Ajuste |
|---|---|
| X1 | E1.2 Sucesso (`H0BJi`): saudação "Olá, Ana"; destaque Pedido 4 (Rascunho, "Ler documentos"); linhas Pedido 2 (Com pendências) e Pedido 1 (Simulação concluída) com data; "Sair" no cabeçalho (data-model §5) |
| X2 | E2.2 Com arquivos (`CvQRm`): "Pedido 5"; linhas `encaminhamento-cardiologia.pdf` (PDF · 184 KB · 2 páginas) e `documento-identidade.jpg` (JPG · 1,2 MB) com tipo, nome do arquivo e "Mais opções"; "2 de 3"; ação principal "Ler documentos" + nota de leitura simulada |
| X3 | E3.4 Unidade emissora (`XCNVm`): "Pedido 2"; critério RC-R04 com a mensagem do protocolo; origem `encaminhamento-ubs-santa-aurora.pdf`; "Conferir documento" → "Abrir documento"; responsável visível |
| X4 | `mobile/design/dados-ficticios.md`: pessoas, pedidos e leituras conforme data-model §5 — estrutura da carga `02_carga_sintetica.sql` e dos catálogos da leitura simulada, com textos exemplares realistas (sem "Exemplo", "sintético" ou texto sem acento); Pedidos 5 a 8 "só no design" |
| X5 | `mobile/design/textos.md`: rótulos de data-model §2; textos de E2.7, E3.7, E3.8 e dos botões de decisão |
| X6 | `specs/007-epico-telas-pendev/contracts/telas.md`: nota de que o inventário foi substituído por esta feature |
| X7 | `design/README.md` e `mobile/design/INDEX.md`: apontar para esta feature e para o novo inventário |

## Fase 1 — RC-36 Entrada e pedidos (P1) · 25 quadros

| ID | Tela | Estados | Qtd |
|---|---|---|---|
| E1.1 | Login | Padrão · Enviando · Credenciais inválidas · Serviço indisponível · Sem conexão | 5 |
| E1.2 | Meus pedidos | Carregando · Vazia · Sucesso (**existe**) · Erro · Sem conexão | 5 |
| E1.3 | Pedido | Carregando · Rascunho sem documentos · Rascunho sem leitura · Rascunho a conferir · Rascunho pronto para verificar · Em verificação † · Confira a leitura † · Com pendências † · Sem pendências identificadas † · Falha na verificação † · Resultado não vigente † · Simulação concluída † (P2) · Não encontrado | 13 |
| E1.4 | Sessão expirada | Padrão | 1 |
| E1.5 | Sem permissão | Padrão | 1 |

## Fase 2 — RC-37 Envio (P1) · 28 quadros

| ID | Tela | Estados | Qtd |
|---|---|---|---|
| E2.1 | Novo pedido | Padrão · Criando · Erro · Sem serviços · Trocar serviço | 5 |
| E2.2 | Documentos | Vazia · Com arquivos (**existe**) · Enviando · Não enviado · Opções do documento · Limite atingido · Pedido verificado · Pedido concluído | 8 |
| E2.7 | Anexar (folha) | Tipo do documento · Origem | 2 |
| E2.3 | Recusa de arquivo | Formato não suportado · Extensão incompatível · Acima de 5 MB · PDF com mais de 5 páginas · PDF protegido · Arquivo ilegível · Limite de 3 · Pedido não aceita documentos | 8 |
| E2.4 | Substituir documento | Rascunho · Pedido verificado | 2 |
| E2.5 | Remover documento | Rascunho · Pedido verificado | 2 |
| E2.6 | Excluir rascunho | Confirmação | 1 |

## Fase 3 — RC-38 Leitura, conferência e verificação (P1) · 22 quadros

| ID | Tela | Estados | Qtd |
|---|---|---|---|
| E3.1 | Leitura simulada | Lendo · Indisponível · Sem documentos · Documentos mudaram | 4 |
| E3.2 | Conferência | Documento lido · Decisões registradas · Não localizado indicado · Campo ilegível · Documento não lido · Texto do documento como dado · Desatualizada · Erro ao salvar | 8 |
| E3.8 | Verificar critérios (diálogo) | Confirmação (campos sem decisão e fim do anexo) | 1 |
| E3.3 | Resultado † | Com pendências · Confira a leitura · Sem pendências identificadas · Falha · Resumo indisponível (verificação em andamento fica em E1.3, research D19) | 5 |
| E3.4 | Detalhe da pendência † | Unidade emissora (**existe**) · Substituir arquivo · Conferir leitura · Documento ausente | 4 |

## Fase 4 — RC-38 Correção e conclusão (P2) · 4 quadros

| ID | Tela | Estados | Qtd |
|---|---|---|---|
| E3.5 | Correção | Resultado não vigente · Comparação † | 2 |
| E3.7 | Concluir simulação (diálogo) † | Confirmação | 1 |
| E3.6 | Comprovante simulado † | Padrão (repetição mostra o mesmo; nota no quadro) | 1 |

## Fase 5 — RC-39 Sensores, administrador e transversais (P3) · 15 quadros

| ID | Tela | Estados | Qtd |
|---|---|---|---|
| E4.1 | Condições da captura † | Normal · Luminosidade fora · Distância fora · Recuperada · Desatualizada · Indisponível | 6 |
| E4.2 | Protocolo e indicadores † | Padrão · Carregando · Erro | 3 |
| E4.3 | Conexão ruim | Tempo esgotado · Sem conexão | 2 |
| E4.4 | Acessibilidade | Anotações · Fonte ampliada E1.2 · Fonte ampliada E2.2 · Fonte ampliada E3.3 | 4 |

## Totais

| História | Quadros | Prioridade |
|---|---|---|
| RC-36 | 25 | P1 (1 quadro P2) |
| RC-37 | 28 | P1 |
| RC-38 | 26 | 22 P1 · 4 P2 |
| RC-39 | 15 | P3 |
| **Total** | **94** | 3 já existem e serão ajustados |

## Fase 6 — revisão e entrega

Para cada fase: screenshots revisadas, consulta de limites sem recorte, contraste dentro dos pares de F1, nota de operação ao lado de cada quadro, prévia PNG em `design/previas/`, arquivo salvo pelo MCP. No fim: SC-001 a SC-007 conferidos e registrados.
