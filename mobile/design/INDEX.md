# Índice das telas

> Inventário dos 73 quadros do épico (fonte: `specs/007-epico-telas-pendev/contracts/telas.md`). **Atualização de 13/11/2026:** três exemplos da identidade aprovada estão editáveis em `design/RegulaCheck.pen`, com consulta pelo MCP e revisão visual. Isso não conclui conjuntos inteiros de estados. Fonte de continuidade e IDs: `design/README.md`.
> Nenhuma tela chama a API: a coluna "API" é só referência para o app futuro. Rotas *planejada* ainda não existem no backend; a tela segue o contrato proposto em `CONTRATO_API.md`.
> Textos de cada quadro: [textos.md](textos.md). Dados: [dados-ficticios.md](dados-ficticios.md). Percursos: [mapa-de-percursos.md](mapa-de-percursos.md).

Legenda de cobertura de estado: **P** quadro próprio · **C** coberto por quadro/componente compartilhado (ver matriz no contrato) · **—** não se aplica. Status: *a desenhar* → *desenhado* → *revisado* → *aprovado*.

## Arquivo corrente: `design/RegulaCheck.pen`

| Exemplo atual | ID nativo | Situação |
|---|---|---|
| E1.2 Meus pedidos — Sucesso | `H0BJi` | Aplicação da identidade; demais estados de E1.2 pendentes |
| E2.2 Documentos — Com arquivos | `CvQRm` | Aplicação da identidade; demais estados de E2.2 pendentes |
| E3.4 Pendência — Unidade emissora | `XCNVm` | Exemplo parcial; revisão completa de critérios/estados ainda pendente |

Prévias e verificações em `design/previas/` e `design/verificacao-2026-11-13.md`. Ordem de leitura: cabeçalho/contexto → conteúdo/próxima ação → ações → aviso acadêmico. Abrir arquivo e Voltar têm alvos próprios de 48 dp; não há leitor de tela ou navegação implementados no desenho.

As seções abaixo preservam o inventário planejado em cinco arquivos. Para este recorte, usar o arquivo corrente acima.

## 00-fundamentos.pen (4 quadros)
| Quadro | Conteúdo | Status |
|---|---|---|
| F1 | Variáveis: cor, tipografia, espaço, forma, toque | v1.0 `IkCIw` no arquivo corrente · desenhado (13/11) |
| F2 | Catálogo de componentes | v1.0 `t9lvSH` · desenhado; sem navegação inferior (decisão pendente) |
| F3 | Aviso acadêmico e selo SIMULADO | v1.0 `EVIof` · desenhado |
| F4 | Selos dos sete estados do pedido (texto + ícone) | v1.0 `HBByt` · desenhado, com estados de arquivo e de critério |

Versões anteriores (`EjfrK`, `H7Mq2`, `EgUbK`, `kBUUQ`) foram movidas para `Wo9WQ` (arquivo, não usar). Detalhes: [design-system.md](design-system.md).

## 01-entrada.pen — história 1 (21 quadros)
| Quadros | Tela | Estados | API (referência) | Rótulo / ordem de leitura | Status |
|---|---|---|---|---|---|
| E1.1 (5) | Login | Padrão · Enviando · Credenciais inválidas · Serviço indisponível · Sem conexão | `POST /auth/login` | a preencher | a desenhar |
| E1.2 (5) | Lista de pedidos | Carregando · Vazia · Sucesso · Erro · Sem conexão | `GET /solicitacoes` | a preencher | a desenhar |
| E1.3 (9) | Detalhe do pedido | Carregando · 7 estados do pedido · Não encontrado | `GET /solicitacoes/{id}` | a preencher | a desenhar |
| E1.4 (1) | Sessão expirada | Padrão | 401 `NAO_AUTENTICADO` | a preencher | a desenhar |
| E1.5 (1) | Sem permissão | Padrão | 403 `ACESSO_NEGADO` | a preencher | a desenhar |

## 02-envio.pen — história 2 (18 quadros)
| Quadros | Tela | Estados | API (referência) | Rótulo / ordem de leitura | Status |
|---|---|---|---|---|---|
| E2.1 (4) | Rascunho e serviço | Padrão · Salvando · Erro · Campos preservados | `GET /protocolos` (*planejada*), `POST/PUT /solicitacoes` | a preencher | a desenhar |
| E2.2 (4) | Documentos | Vazia · Com arquivos · Enviando · Interrompido | `POST/GET /solicitacoes/{id}/documentos` (*planejada*) | a preencher | a desenhar |
| E2.3 (7) | Recusa de arquivo | Tipo · Extensão · Tamanho · Páginas · Protegido · Ilegível · Quantidade | 415, 413, 400 (`docs/api/protecoes.md`) | a preencher | a desenhar |
| E2.4 (1) | Confirmar substituição | Confirmação | documentos (*planejada*) | a preencher | a desenhar |
| E2.5 (1) | Confirmar remoção | Confirmação | `DELETE .../documentos/{docId}` (*planejada*) | a preencher | a desenhar |
| E2.6 (1) | Confirmar exclusão do rascunho | Confirmação | `DELETE /solicitacoes/{id}` (*planejada*) | a preencher | a desenhar |

## 03-resultado.pen — história 3 (15 quadros)
| Quadros | Tela | Estados | API (referência) | Rótulo / ordem de leitura | Status |
|---|---|---|---|---|---|
| E3.1 (1) | Leitura simulada | Processando | `POST .../extracoes` (*planejada*) | a preencher | a desenhar |
| E3.2 (3) | Conferência da transcrição | Padrão · Divergência marcada · Campo ausente | `PUT .../extracoes/{id}/conferencia` (*planejada*) | a preencher | a desenhar |
| E3.3 (5) | Resultado da análise | Processando · Com pendências · Requer conferência · Sem pendências identificadas · Falha | `POST/GET .../analises` (*planejada*) | a preencher | a desenhar |
| E3.4 (2) | Detalhe da pendência | Reenviar · Unidade emissora | `GET .../analises` (*planejada*) | a preencher | a desenhar |
| E3.5 (2) | Reenvio e comparação | Antes do novo resultado · Comparação | documentos e análises (*planejada*) | a preencher | a desenhar |
| E3.6 (2) | Comprovante simulado | Padrão · Repetido | `POST .../submissoes-simuladas` (*planejada*) | a preencher | a desenhar |

## 04-sensores-admin-acessibilidade.pen — história 4 (15 quadros)
| Quadros | Tela | Estados | API (referência) | Rótulo / ordem de leitura | Status |
|---|---|---|---|---|---|
| E4.1 (6) | Estação de captura | Normal · Luminosidade · Distância · Recuperada · Desatualizada · Indisponível | `GET /solicitacoes/{id}/leituras` (*planejada*) | a preencher | a desenhar |
| E4.2 (3) | Administrador | Padrão · Carregando · Sem permissão | `GET /admin/indicadores` (*planejada*) | a preencher | a desenhar |
| E4.3 (2) | Conexão ruim | Tempo esgotado · Sem conexão | sem chamada | a preencher | a desenhar |
| E4.4 (4) | Guia de acessibilidade | Anotações · Fonte ampliada em E1.2, E3.3 e E2.2 | — | a preencher | a desenhar |

**Total: 4 + 21 + 18 + 15 + 15 = 73 quadros.**
