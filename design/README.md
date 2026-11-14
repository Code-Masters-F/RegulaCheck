# RegulaCheck — continuidade do design

**Atualizado em 13/11/2026. Leia este arquivo primeiro ao retomar com outro agente.**

## Situação atual

O responsável aprovou **seguir com a identidade dos três exemplos inspirados no Headspace** e levá-la ao pen.dev. A identidade está aprovada como direção. **Em 13/11 os fundamentos e o catálogo foram refeitos como design system v1.0** a partir das três telas (T044–T047); os 73 estados do épico e a usabilidade Android continuam pendentes. O trabalho atual é somente desenho: não há aplicativo Android implementado por este trabalho.

- Workspace: `RegulaCheck`; branch local: `codex/007-design-system-exploracao`. Há alterações locais sem commit; preservar o trabalho existente.
- Fonte nativa: **`design/RegulaCheck.pen`**. Não ler, buscar ou editar `.pen` como texto; usar exclusivamente o MCP `pencil`.
- URI do MCP nesta máquina: `file:///C:/Users/Eliseu%20Santos/Desktop/fiap-faculdade/RegulaCheck/design/RegulaCheck.pen`.
- Rastreabilidade: `specs/007-epico-telas-pendev/`, recorte **T040–T043**. Reutilizar este épico, sem criar outra feature para pequenas alterações.

## Direção aprovada

- **Minimalista e simpática**, com respiro e linguagem que ajude a resolver burocracia. Referência: Headspace / Explore / iOS, captura Refcat de 08/09/2025. Inspirar-se no tom, sem copiar a marca.
- **Bricolage Grotesque 600** em títulos; **Nunito Sans 400** em corpo e **600/700** em ações/ênfase. Títulos de tela 32, corpo 16, apoio 13–14. No pen.dev são famílias explícitas; no Android, usar os arquivos reais e validar fonte ampliada.
- Paleta: texto `#112B3C`; ação azul `#205375`; acento laranja `#F6680E`; neutro `#EFEFEF`; fundo branco. Apoio `#526774`; sucesso `#176448`; simulado `#8D3C0C` sobre `#FFF1E6`.
- Superfícies funcionais com raio 24; botões atuais de 52 de altura; alvos de toque ≥48. Poucas caixas, divisores leves, uma ação principal por contexto.
- **Poucas ilustrações, apenas quando úteis**; nenhuma nas três telas atuais. Sem gradientes ou enfeites decorativos.
- **Pesquisa fica em texto. Não recolocar painéis, sites ou capturas de referências no canvas.**

## O que existe no pen.dev (design system v1.0, 13/11/2026)

**Leia [mobile/design/design-system.md](../mobile/design/design-system.md) antes de desenhar qualquer tela.** Ele é a referência canônica de tokens, tipografia, componentes e regras.

Linha superior (y=0), da esquerda para a direita:

| Quadro | ID | Prévia |
|---|---|---|
| F1 · Fundamentos · v1.0 | `IkCIw` | [PNG](previas/fundamentos/IkCIw.png) |
| F2 · Componentes · v1.0 | `t9lvSH` | [PNG](previas/fundamentos/t9lvSH.png) |
| F3 · Confiança e simulação · v1.0 | `EVIof` | [PNG](previas/fundamentos/EVIof.png) |
| F4 · Estados do pedido · v1.0 | `HBByt` | [PNG](previas/fundamentos/HBByt.png) |
| E1.2 Meus pedidos — Sucesso · v1.0 | `H0BJi` | [PNG](previas/H0BJi.png) |
| E2.2 Documentos — Com arquivos · v1.0 | `CvQRm` | [PNG](previas/CvQRm.png) |
| E3.4 Pendência — Unidade emissora · v1.0 | `XCNVm` | [PNG](previas/XCNVm.png) |

As três telas agora são montadas só com instâncias dos componentes v1.0 (71 componentes desde a Fase 0 da feature 014; ver F2–F4). Componentes mais usados: `Botão/Primário` `YNfQA`, `Botão/Contorno` `DpTaD`, `Botão/Texto` `j97Pe`, `Cabeçalho/Voltar` `PbS1q`, `Tela/Título` `YtzRv`, `Pedido/Destaque` `bwbsT`, `Pedido/Linha` `D4QLJd`, `Documento/Linha` `N6LHC`, `Pendência/Orientação` `tAmsl`, `Aviso/Rodapé` `zX8C6`, `Selo/Simulado` `ONwnK`, `Estado/*` em F4.

**Arquivo** `Wo9WQ` (y=7000): F1–F4 anteriores, biblioteca antiga e prévia `O84XR`, com componentes renomeados `Legado/*`. Não usar; pode ser apagado quando o grupo decidir. `rc-font-family`/Plus Jakarta Sans e `rc-review` (roxo) são legado.

Para montar uma tela nova: copiar uma das três telas (390 × 844, padding 24/24/16, conteúdo vertical com gap 24, `Respiro flexível` antes do rodapé) e trocar as instâncias. Altura de controle deve ser **numérica (52)**: o pen.dev ignora variável em `height`. Telas de lista que passam da altura são desenhadas inteiras, com a linha "dobra · 844" marcando o que aparece sem rolar (ex.: E1.2 `H0BJi`).

### Montagem das telas (feature 014, desde 13/11/2026)

O fluxo, o conteúdo por tela, as ações permitidas pelo backend e o inventário de 94 quadros estão em [specs/014-fluxo-telas-app/](../specs/014-fluxo-telas-app/) (`spec.md`, `data-model.md`, `contracts/`, `tasks.md`). No canvas, cada história tem uma faixa com capa em x=5200: RC-36 (y=0), RC-37 (y=1200), RC-38 (y=2400), RC-39 (y=3600); os quadros seguem à direita, passo 470, com uma nota de operação abaixo. Textos dos quadros: [mobile/design/textos.md](../mobile/design/textos.md); dados exemplares: [mobile/design/dados-ficticios.md](../mobile/design/dados-ficticios.md) — estrutura do backend, textos realistas, nunca os literais de teste.

Fase 0 concluída em 13/11: 13 componentes novos ou ajustados (`Folha/Escolha`, `Campo/Senha`, `Botão/Decisão`, `Conferência/*`, `Pedido/Próximo passo`, `Linha/Navegação`, `Dado/Linha`, `Critério/Linha`, `Comparação/*`, `Aviso/Resultado não vigente`, "Sair" no cabeçalho, data no pedido, nome do arquivo e "Mais opções" no documento) e os três exemplos alinhados ao backend.

## Ferramentas e conexões

| Recurso | Estado e uso |
|---|---|
| **pen.dev / pencil MCP** | Conectado nesta sessão. Usar `get_app_state` → `read_skill` (schema/execute) → `execute` → screenshots/checagem de limites → `save`. `Export` gera prévias; IDs acima evitam recriar quadros. |
| **Refcat MCP remoto** | Configurado como `refcat`, endpoint `https://mcp.refcat.app/mcp`, autenticação `bearer_token_env_var=REFCAT_API_KEY`. A variável do usuário Windows está configurada; **não copiar seu valor para chat, arquivos ou logs**. |
| **Acesso ao Refcat** | Consultas autenticadas de catálogo/tela funcionaram. As ferramentas não apareceram nativamente nesta sessão; a pesquisa usou o protocolo MCP por HTTPS no PowerShell, com validação TLS nativa do Windows. O Python apresentou erro de certificado; não desativar TLS. Reiniciar o Codex pode carregar o MCP e a variável no processo. Diagnóstico sem segredo: `codex mcp get refcat --json`. |
| **Chave local** | Auxiliar `scripts/configurar-refcat.ps1`: o próprio responsável insere a chave em campo oculto. Outro computador/agente remoto precisa de sua própria configuração; este repositório não transporta a chave. |
| **Spec Kit** | Nove skills oficiais `speckit-*` em `.agents/skills/`. CLI isolada em `%LOCALAPPDATA%/RegulaCheck/tools/spec-kit/Scripts/specify.exe`. Configuração Claude anterior preservada. |
| **Android** | Skill comunitária MIT `android-design-guidelines` em `%USERPROFILE%/.codex/skills/`. É consulta; não impõe modo escuro ou cor dinâmica. Nenhum SDK/emulador foi instalado por este trabalho. |
| **Image Gen** | Ferramenta integrada usada para conceitos; imagens em `design/exploracoes/headspace-2026-11-13/`. São referência visual, não substituem o `.pen` editável. |

Não usar geração/exportação do Refcat que consuma cota sem pedido. Os permalinks sugeridos pelo catálogo não funcionaram; pesquisar o aplicativo/fluxo em [Refcat](https://refcat.app/dashboard/). Evidências e limites: [referencias.md](referencias.md).

## Retomar com segurança

1. Ler `AGENTS.md`, `CLAUDE.md`, `.specify/memory/constitution.md` e este resumo; preservar alterações locais.
2. Abrir o `.pen`, consultar os IDs e editar o conjunto `Atual/*`, mantendo a direção aprovada. Conferir os exemplos e textos antes de desenhar outra tela.
3. Usar o épico 007 explicitamente no processo, sem mudar o ponteiro global da feature 008:
   ```powershell
   $env:SPECIFY_FEATURE_DIRECTORY = 'specs/007-epico-telas-pendev'
   & ./.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
   ```
   O script preservado do projeto não aceita `-RequireSpec`; não regenerar scripts/backlog por causa disso.
4. Continuar apenas o próximo recorte solicitado e registrar o progresso real via Spec Kit. Não criar commits, push, issues ou publicação automaticamente.

**Limites do produto:** só dados sintéticos; extração/IA simuladas e identificadas. A paciente confere transcrição; a unidade emissora corrige conteúdo clínico. Sem diagnóstico, urgência, autorização, promessa de vaga ou envio real ao SUS.

**Verificado:** quadros F1–F4 v1.0 e três telas revisados visualmente; consulta de limites sem recorte/colapso (exceto a saudação oculta por padrão em `Tela/Título`, esperado); contraste calculado de todos os pares do catálogo; prévias exportadas e documento salvo pelo MCP. **Pendente:** estados completos, protótipo navegável, revisão com pessoas, texto ampliado/TalkBack/teclado/insets no Android e integração com API. Detalhes: [verificação](verificacao-2026-11-13.md).

Material de apoio: [prompts/conceitos](exploracoes/headspace-direcao-2026-11-13.md), `mobile/design/textos.md`, `dados-ficticios.md`, `mapa-de-percursos.md`, `design-system.md`. Os documentos antigos devem ser lidos com o status de entrega parcial acima.

