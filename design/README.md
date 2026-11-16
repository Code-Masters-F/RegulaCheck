# RegulaCheck — continuidade do design

**Atualizado em 14/11/2026. Leia este arquivo primeiro ao retomar com outro agente.**

## Situação atual

O responsável aprovou **seguir com a identidade dos três exemplos inspirados no Headspace** e levá-la ao pen.dev. A identidade está aprovada como direção. **Em 13/11 os fundamentos e o catálogo foram refeitos como design system v1.0.** Em 14/11 a feature 015 desenhou **os 94 quadros do fluxo de telas**, alinhados ao backend, em quatro faixas (RC-36 a RC-39). Eles estão listados em [mobile/design/INDEX.md](../mobile/design/INDEX.md). Os quadros foram revisados por quem desenhou, mas ainda não foram aprovados pelo grupo nem percorridos por outra pessoa (T060). O trabalho é somente desenho: não há aplicativo Android implementado por ele.

- Workspace: `RegulaCheck`; branch: `codex/007-design-system-exploracao`, enviada ao GitHub. Preservar o trabalho existente.
- Fonte nativa: **`design/RegulaCheck.pen`**. Não ler, buscar ou editar `.pen` como texto; usar exclusivamente o MCP `pencil`.
- URI do MCP nesta máquina: `file:///C:/Users/Eliseu%20Santos/Desktop/fiap-faculdade/RegulaCheck/design/RegulaCheck.pen`.
- Rastreabilidade: `specs/007-epico-telas-pendev/`, recorte **T040–T043**. Reutilizar este épico, sem criar outra feature para pequenas alterações.

## Direção aprovada

- **Minimalista e simpática**, com respiro e linguagem que ajude a resolver burocracia. Referência de tom: Headspace (tela Explore, iOS). Inspirar-se no tom, sem copiar a marca.
- **Bricolage Grotesque 600** em títulos; **Nunito Sans 400** em corpo e **600/700** em ações/ênfase. Títulos de tela 32, corpo 16, apoio 13–14. No pen.dev são famílias explícitas; no Android, usar os arquivos reais e validar fonte ampliada.
- Paleta: texto `#112B3C`; ação azul `#205375`; acento laranja `#F6680E`; neutro `#EFEFEF`; fundo branco. Apoio `#526774`; sucesso `#176448`; simulado `#8D3C0C` sobre `#FFF1E6`.
- Superfícies funcionais com raio 24; botões atuais de 52 de altura; alvos de toque ≥48. Poucas caixas, divisores leves, uma ação principal por contexto.
- **Poucas ilustrações, apenas quando úteis**; nenhuma nas três telas atuais. Sem gradientes ou enfeites decorativos.
- **Não colocar painéis, sites ou capturas de referências no canvas.**

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

As três telas agora são montadas só com instâncias dos componentes v1.0 (71 componentes desde a Fase 0 da feature 015; ver F2–F4). Componentes mais usados: `Botão/Primário` `YNfQA`, `Botão/Contorno` `DpTaD`, `Botão/Texto` `j97Pe`, `Cabeçalho/Voltar` `PbS1q`, `Tela/Título` `YtzRv`, `Pedido/Destaque` `bwbsT`, `Pedido/Linha` `D4QLJd`, `Documento/Linha` `N6LHC`, `Pendência/Orientação` `tAmsl`, `Aviso/Rodapé` `zX8C6`, `Selo/Simulado` `ONwnK`, `Estado/*` em F4.

**Arquivo** `Wo9WQ` (antes em y=7000): F1–F4 anteriores, biblioteca antiga e prévia `O84XR`, com componentes renomeados `Legado/*`. Não usar. Em 14/11/2026 o quadro não estava mais no documento aberto, e a feature 015 não o excluiu. A remoção provavelmente aconteceu no commit `0e4cdb6`; a versão de `efc76d1` pode recuperar o quadro. Fica para o grupo confirmar. `rc-font-family`/Plus Jakarta Sans e `rc-review` (roxo) são legado.

Para montar uma tela nova: copiar uma das três telas (390 × 844, padding 24/24/16, conteúdo vertical com gap 24, `Respiro flexível` antes do rodapé) e trocar as instâncias. Altura de controle deve ser **numérica (52)**: o pen.dev ignora variável em `height`. Telas de lista ou de conferência que passam da altura são desenhadas inteiras, com a linha "dobra · 844" marcando o que aparece sem rolar (ex.: E1.2 `H0BJi`).

### Montagem das telas (feature 015, desde 13/11/2026)

O fluxo, o conteúdo por tela, as ações permitidas pelo backend e o inventário de 94 quadros estão em [specs/015-fluxo-telas-app/](../specs/015-fluxo-telas-app/) (`spec.md`, `data-model.md`, `contracts/`, `tasks.md`). No canvas (research D20, 14/11/2026), à direita de F1–F4 (x=5200), o cartão **"Como avaliar o fluxo"** (`io4cw`) explica a leitura e numera o caminho principal. Abaixo dele vêm **8 etapas**, com **uma linha por tela (01 a 21)** na ordem de uso. Em cada linha, um cartão à esquerda diz o propósito da tela, de onde se chega, para onde se vai e a operação; os estados seguem da esquerda para a direita (passo 470, a partir de x=5760), cada um com a sua nota logo abaixo. As linhas estão listadas em [mobile/design/INDEX.md](../mobile/design/INDEX.md). Textos dos quadros: [mobile/design/textos.md](../mobile/design/textos.md); dados exemplares: [mobile/design/dados-ficticios.md](../mobile/design/dados-ficticios.md) — estrutura do backend, textos realistas, nunca os literais de teste.

Fase 0 concluída em 13/11: 13 componentes novos ou ajustados (`Folha/Escolha`, `Campo/Senha`, `Botão/Decisão`, `Conferência/*`, `Pedido/Próximo passo`, `Linha/Navegação`, `Dado/Linha`, `Critério/Linha`, `Comparação/*`, `Aviso/Resultado não vigente`, "Sair" no cabeçalho, data no pedido, nome do arquivo e "Mais opções" no documento) e os três exemplos alinhados ao backend.

## Ferramentas e conexões

| Recurso | Estado e uso |
|---|---|
| **pen.dev / pencil MCP** | Conectado nesta sessão. Usar `get_app_state` → `read_skill` (schema/execute) → `execute` → screenshots/checagem de limites → `save`. `Export` gera prévias; IDs acima evitam recriar quadros. |
| **Spec Kit** | Nove skills oficiais `speckit-*` em `.agents/skills/`. CLI isolada em `%LOCALAPPDATA%/RegulaCheck/tools/spec-kit/Scripts/specify.exe`. Configuração Claude anterior preservada. |
| **Android** | Skill comunitária MIT `android-design-guidelines` em `%USERPROFILE%/.codex/skills/`. É consulta; não impõe modo escuro ou cor dinâmica. Nenhum SDK/emulador foi instalado por este trabalho. |
A pesquisa de referências (Refcat), as capturas de outros aplicativos e as imagens de conceito saíram do repositório em 14/11/2026, por decisão do responsável. A direção que elas ajudaram a escolher está em [design-system.md](../mobile/design/design-system.md). Os arquivos podem ser recuperados pelo histórico do Git (commit `b696b1f`).

## Retomar com segurança

1. Ler `AGENTS.md`, `CLAUDE.md`, `.specify/memory/constitution.md` e este resumo; preservar alterações locais.
2. Abrir o `.pen` e consultar os IDs em [mobile/design/INDEX.md](../mobile/design/INDEX.md). Usar só os componentes v1.0 de F2–F4 e manter a direção aprovada. Conferir textos e dados antes de mudar uma tela.
3. A feature ativa do Spec Kit é a 015 (`.specify/feature.json`). Rodar os scripts a partir da raiz do repositório:
   ```powershell
   & ./.specify/scripts/powershell/check-prerequisites.ps1 -Json -RequireTasks -IncludeTasks
   ```
   O script preservado do projeto não aceita `-RequireSpec`; não regenerar scripts/backlog por causa disso.
4. Continuar apenas o próximo recorte solicitado e registrar o progresso real via Spec Kit. Não criar commits, push, issues ou publicação automaticamente.

**Limites do produto:** só dados sintéticos; extração/IA simuladas e identificadas. A paciente confere transcrição; a unidade emissora corrige conteúdo clínico. Sem diagnóstico, urgência, autorização, promessa de vaga ou envio real ao SUS.

**Verificado em 14/11/2026:**
- F1–F4 v1.0 e os 94 quadros foram revisados visualmente. A consulta de limites não encontrou recorte nem colapso, fora elementos ocultos por padrão.
- Contraste calculado para todos os pares do catálogo.
- Auditorias T057/T058: toda ação tem operação ou navegação registrada; dados conferidos com a carga; 7 estados, 3 situações de campo e 4 decisões desenhados; linguagem proibida só aparece em negações; aviso acadêmico em 100% dos quadros; SIMULADO onde é exigido; 0 `Legado/*`; no máximo uma ação principal por quadro.
- Prévias exportadas e documento salvo pelo MCP.

**Pendente:**
- Percurso com uma pessoa que não desenhou (T060, SC-003 e SC-004).
- Aprovação do grupo e protótipo navegável.
- Fonte ampliada, TalkBack, teclado e insets em aparelho Android.
- Integração com a API, inclusive as rotas † ainda não implementadas (RC-14 a RC-17, RC-20, RC-21 e RC-22). As da RC-13 já estão na `main`; em 15/11/2026 o contrato das telas e as notas do canvas foram conferidos com elas e perderam o † (T069, T073).
- Confirmação do grupo sobre a saída do quadro de arquivo `Wo9WQ`.

Detalhes: [verificação](verificacao-2026-11-13.md).

Material de apoio em `mobile/design/`: `design-system.md`, `textos.md`, `dados-ficticios.md`, `mapa-de-percursos.md`, `acessibilidade.md` e `INDEX.md`.

