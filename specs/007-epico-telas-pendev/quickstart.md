# Quickstart: Épico de telas no pen.dev

## Pré-requisitos

**Recorte corrente (13/11/2026):** começar por `design/README.md`, na branch `codex/007-design-system-exploracao`, e abrir `design/RegulaCheck.pen`. Os três exemplos estão editáveis; as instruções de cinco arquivos/73 estados abaixo são o plano histórico ainda incompleto. Para scripts Spec Kit, usar `SPECIFY_FEATURE_DIRECTORY=specs/007-epico-telas-pendev` no processo, pois o ponteiro global ainda é da feature 008. Não trocar o ponteiro global automaticamente.
- Aplicativo pen.dev aberto e conectado ao servidor de ferramentas (pencil MCP). Se o assistente responder "transport not connected", abra o aplicativo e a pasta do projeto e tente de novo.
- Branch `007-epico-telas-pendev` e, na pasta `mobile/design/`, os arquivos `.pen` da história em andamento. Arquivos `.pen` só se leem e editam **pelas ferramentas do pen.dev**.

## Ordem de trabalho (uma história por vez)
1. **Fundamentos** (`00-fundamentos.pen`): variáveis e componentes. Nada das outras histórias começa sem isso.
2. **História 1** — entrada (`01-entrada.pen`): login, lista, detalhe, sessão expirada, sem permissão.
3. **História 2** — envio (`02-envio.pen`).
4. **História 3** — resultado (`03-resultado.pen`).
5. **História 4** — sensores, administrador e acessibilidade (`04-...pen`).

## Para cada tela
1. Conferir a linha da tela em `specs/007-epico-telas-pendev/contracts/telas.md` (estados, API, origem).
2. Escrever os textos em `mobile/design/textos.md` **antes** de desenhar; usar os dados de `dados-ficticios.md`.
3. Desenhar um quadro por estado, nomeado `<ID> <Tela> — <Estado>`, usando só componentes e variáveis do catálogo.
4. Conferir: aviso acadêmico, selo SIMULADO onde couber, estado por texto + ícone, alvos ≥ 48 dp, contraste ≥ 4,5:1, texto ≥ 16 sp.
5. Registrar a tela em `INDEX.md` (quadro, rota da API, contrato, rótulos para leitor de tela, ordem de leitura).
6. Atualizar `mapa-de-percursos.md` se a tela mudar uma ligação.

## Revisão e entrega
1. Aplicar `mobile/design/checklist-revisao.md` à história e registrar o resultado (o que foi e o que **não** foi revisado).
2. Pedir a uma pessoa que não desenhou para percorrer o percurso principal pelo mapa (SC-005).
3. O responsável aprova a história (SC-007); correções pedidas ficam registradas.
4. Só depois de aprovado, abrir PR da história. Nenhum commit sem pedido.

## O que nunca fazer
- Colocar dado de pessoa real, credencial, token ou chave nos desenhos ou nos textos.
- Escrever texto que diagnostique, defina urgência, autorize atendimento ou prometa vaga/aprovação.
- Marcar uma pendência como resolvida só porque houve novo envio.
- Ler ou buscar texto dentro de um `.pen` com ferramentas comuns de arquivo.
