# Quickstart: desenhar e revisar os quadros desta feature

Guia para quem vai montar as telas no pen.dev (pessoa ou agente). Só design: nenhum código muda.

## Antes de começar

1. Abrir `design/RegulaCheck.pen` no pen.dev, conectado ao pencil MCP. Nunca ler ou editar `.pen` como texto.
2. Ler, nesta ordem: `mobile/design/design-system.md` (tokens e componentes v1.0), [spec.md](spec.md) (fluxo e FRs), [data-model.md](data-model.md) (o que cada tela mostra e com que rótulo), [contracts/telas-operacoes.md](contracts/telas-operacoes.md) (ações e respostas), [contracts/inventario-quadros.md](contracts/inventario-quadros.md) (lista e ordem).
3. Conferir que a Fase 0 do inventário (ajustes A1–A13 e X1–X7) está feita antes de desenhar a Fase 1.

## Montar um quadro

1. Copiar a tela-exemplo mais próxima (`H0BJi`, `CvQRm` ou `XCNVm`): 390 × 844, padding 24/24/16, conteúdo vertical com gap 24, `Respiro flexível` antes do `Aviso/Rodapé`.
2. Nomear `<ID> <Tela> — <Estado>` (ex.: `E3.2 Conferência — Campo ilegível`) e posicionar na faixa da história (RC-36 a RC-39), da esquerda para a direita na ordem do inventário.
3. Trocar só instâncias e textos. Textos vêm de `mobile/design/textos.md` e dos rótulos de data-model §2; dados de data-model §5 (carga do backend, catálogo da leitura simulada e Pedidos 5 a 8 "só no design") e de `mobile/design/dados-ficticios.md` para datas ilustrativas.
4. Conferir que toda ação do quadro está em `telas-operacoes.md` para aquele estado; se não estiver, a ação não entra.
5. Adicionar ao lado do quadro um nó `note` com: operação(ões) usada(s), FR atendidos e, se for o caso, "contrato provisório (RC-xx)".

## Verificar

| Verificação | Como | Critério |
|---|---|---|
| Layout | `Get` com `resolveInstances` e `ctx.problems` | 0 recorte (a saudação oculta de `Tela/Título` é esperada) |
| Visual | `TakeScreenshot` do quadro | hierarquia, no máximo uma ação principal (FR-005), aviso no rodapé |
| Dados | valores do quadro ↔ data-model §5 | estrutura da carga; valores exemplares em português correto, sem "Exemplo", "sintético", texto sem acento ou nome de arquivo de teste; "só no design" anotado |
| Componentes | visitor procurando `ref` para `Legado/*` | 0 (SC-007) |
| Rastreabilidade | cada ação ↔ linha de `telas-operacoes.md`; cada dado ↔ data-model §3 | 100% (SC-001, SC-002) |
| Linguagem | busca em `textos.md` por SUS, fila, vaga, aprov, garant, prazo, urgência, autoriza, diagnóst | só negações e avisos (SC-006) |
| Simulação | quadros de leitura, conferência, resumo, comprovante e sensores | selo SIMULADO presente |

Ao fim de cada fase: exportar PNG 2× para `design/previas/`, salvar pelo MCP e registrar em `design/verificacao-2026-11-13.md` (ou arquivo do dia) o que foi e o que não foi verificado.

## Percurso de validação (SC-003, SC-004)

Com o protótipo navegável ou os quadros em sequência, uma pessoa que não desenhou:
1. percorre login → Pedido 1 na revisão 2 (sem pendências identificadas, caso 1) → Concluir simulação → comprovante `SIM-2026-0001`, cronometrando (meta ≤ 5 min);
2. abre os quadros de E1.3 e diz a ação principal de cada situação (meta ≥ 10 de 11).

Registrar resultado real; sem execução, marcar como não verificado.
