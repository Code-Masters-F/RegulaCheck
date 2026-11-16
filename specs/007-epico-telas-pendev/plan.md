# Implementation Plan: Épico de telas do aplicativo no pen.dev (somente design)

**Branch**: `007-epico-telas-pendev` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-epico-telas-pendev/spec.md`

## Summary

**Recorte autorizado em 13/11/2026:** manter este épico e a branch local `codex/007-design-system-exploracao`; transpor somente os três exemplos aprovados como direção visual para `design/RegulaCheck.pen`. Reutilizar variáveis de cor, definir papéis tipográficos Bricolage Grotesque/Nunito Sans e componentes editáveis para as novas telas. A biblioteca anterior permanece identificada como referência em revisão. Validar por screenshots, limites dos elementos e pares de contraste; exportar prévias e salvar pelo MCP. Criar `design/README.md` como ponto de retomada. Não executar código Android/backend, criar issues ou expandir os 73 estados. A organização em cinco arquivos abaixo permanece o plano histórico do inventário completo, não a localização desta entrega parcial.

Entrega o **desenho das telas** do app Android (celular, retrato) em arquivos do pen.dev, com dados fictícios e sem código. Como os arquivos `.pen` só devem ser acessados pelas ferramentas do pen.dev (o site os descreve como JSON aberto, mas a ferramenta os trata como protegidos e não revisáveis por diff), o plano separa o que é **texto revisável no Git** do que é **desenho**:

1. **Desenho** em 5 arquivos `.pen` em `mobile/design/` (um por grupo de histórias): `00-fundamentos`, `01-entrada`, `02-envio`, `03-resultado`, `04-sensores-admin-acessibilidade`. Cada tela é um quadro nomeado `<ID> <Tela> — <Estado>` (73 quadros no total, ver `contracts/telas.md`, com a matriz tela × estado como critério de revisão).
2. **Texto revisável**: `mobile/design/textos.md` (todos os textos da interface em português, por tela e estado), `mobile/design/INDEX.md` (inventário tela → quadro → rota da API → contrato), `mobile/design/mapa-de-percursos.md` (como as telas se ligam) e `mobile/design/checklist-revisao.md` (conteúdo, estados, acessibilidade, linguagem proibida, com o resultado registrado). A **revisão de linguagem** (SC-003) é feita sobre `textos.md` e conferida contra os quadros.
3. **Fundamentos**: variáveis de design (cores, tipografia, espaçamento, raios) e um catálogo de componentes reutilizados por todas as telas, incluindo o aviso acadêmico, o selo de simulado e o selo de estado por texto + ícone.
4. **Registro no ClickBus**: depois da aprovação do texto da especificação, 1 épico e 4 histórias como issues, com critérios de aceite e a lista de quadros de cada uma.

Nenhum backend, nenhum código Android/Kotlin, nenhuma chamada à API e nenhuma alteração no Oracle.

## Technical Context

**Language/Version**: não se aplica (design). Ferramenta: aplicativo pen.dev e seu servidor de ferramentas (pencil MCP), operados pela pessoa Dev.

**Primary Dependencies**: pen.dev (https://www.pen.dev/, antigo pencil.dev; aplicativo, versão web ou extensão de VS Code/Cursor, aberto e conectado ao pencil MCP). Nenhuma dependência de build nem biblioteca nova no repositório.

**Storage**: arquivos `.pen` versionados em `mobile/design/` (binários: não aparecem como texto no diff). Texto de apoio em Markdown. Sem banco.

**Testing**: revisão, não teste automatizado: (a) checklist de revisão por tela; (b) conferência de que o inventário (`INDEX.md`/`contracts/telas.md`) bate com os quadros existentes; (c) revisão de linguagem sobre `textos.md`; (d) percurso feito por uma pessoa que não desenhou (SC-005); (e) aprovação do responsável (SC-007). Os testes Java existentes não mudam; `mvnw clean verify` deve continuar verde (nada de código é alterado).

**Target Platform**: celular Android em retrato (referência de tela 360 × 800 dp), sem tablet, sem modo escuro.

**Project Type**: design de aplicativo móvel (artefatos de design + documentação). Sem módulo Maven novo.

**Performance Goals**: não se aplica. Metas de uso: percurso principal compreendido em até 5 minutos (SC-005); rota da API de uma tela achada em até 2 minutos (SC-006).

**Constraints**: só dados sintéticos; sem diagnóstico, urgência, autorização, promessa de vaga ou aprovação do SUS; resumo e extração sempre marcados como **simulados**; nunca só cor; contraste ≥ 4,5:1 para texto, alvos de toque ≥ 48 dp, texto base ≥ 16 sp e layout que suporta fonte ampliada; sem credencial, token ou chave desenhados nas telas; nenhum nome ou documento de pessoa real.

**Scale/Scope**: 5 arquivos `.pen`, 73 quadros, ≈ 25 componentes, 4 histórias, 1 pessoa Dev. Prazo: o épico acompanha o Épico 6 do cronograma (mobile e operação administrativa, até 17/11/2026); datas finais são do grupo.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende e é o centro do épico: aviso acadêmico em toda tela, dados fictícios, protocolo fictício `PROTOCOLO-ACADEMICO-FICTICIO v0.1`. |
| II. Limites clínicos e integridade documental | Atende: telas orientam a unidade emissora para informação clínica ausente; nenhuma tela edita o documento original nem diagnostica; extração distingue identificado, não localizado e ilegível. |
| III. Backend Java em monólito modular | N/A: não há backend nem código. Nenhuma tela acessa banco ou provedor de IA (o app só consome a API). |
| IV. Segurança e autorização por recurso | Atende: telas refletem 401/403/404 do contrato; visão do administrador sem dado individual; nenhum segredo nas telas. |
| V. Regras determinísticas, IA subordinada e dados não confiáveis | Atende: resumo/extração marcados como simulados; texto de documento desenhado como texto puro; a tela não altera resultado do motor. |
| VI. Persistência e confiabilidade | Atende por refletir o contrato: `PROCESSANDO` não equivale a concluído; sucesso só após confirmação da API; resolvida só após novo resultado. |
| VII. Verificação honesta | Atende: "implementado" = telas desenhadas **e revisadas**; revisão registrada com o que não foi revisado; SC-005 e SC-007 só valem se medidos com pessoas. |
| VIII. Código documentado, mudanças mínimas | Atende: sem código; mudanças confinadas a `mobile/design/`, `mobile/README.md` e documentação do épico. |
| Restrições tecnológicas | Atende: não escolhe framework novo de mobile (Android/Kotlin já decidido); `.pen` só via ferramentas do pen.dev. |

**Re-avaliação após o desenho (Phase 1)**: sem violações. Sem itens em *Complexity Tracking*.

## Project Structure

### Documentation (this feature)

```text
specs/007-epico-telas-pendev/
├── plan.md              # Este arquivo
├── research.md          # Decisões D1–D9
├── data-model.md        # Convenções: arquivos, quadros, variáveis, componentes, ids
├── quickstart.md        # Como abrir, desenhar, revisar e entregar
├── contracts/
│   └── telas.md         # Inventário de telas × estados × rota da API × RC de origem
├── checklists/
│   └── requirements.md
└── tasks.md             # Gerado por /speckit-tasks
```

### Source (repository root)

```text
mobile/
├── README.md                        # + seção "Design das telas" apontando para design/
└── design/
    ├── 00-fundamentos.pen           # variáveis, estilos e catálogo de componentes
    ├── 01-entrada.pen               # login, lista, detalhe, sessão expirada, sem permissão
    ├── 02-envio.pen                 # rascunho, documentos, recusas, confirmações
    ├── 03-resultado.pen             # conferência, resultado, pendências, reenvio, comprovante
    ├── 04-sensores-admin-acessibilidade.pen
    ├── INDEX.md                     # inventário: quadro → rota/contrato → RC de origem
    ├── mapa-de-percursos.md         # percursos e ligações entre telas (texto + diagrama)
    ├── textos.md                    # todos os textos da interface por tela/estado (revisáveis em Git)
    ├── dados-ficticios.md           # pedidos, pessoas, pendências e arquivos inventados usados nos desenhos
    ├── acessibilidade.md            # lista de acessibilidade do épico
    └── checklist-revisao.md         # checklist + registro da revisão
docs/ (sem mudança de contrato); CONTRATO_API.md (sem mudança)
```

**Structure Decision**: tudo em `mobile/design/`, ao lado do README do app. Um arquivo `.pen` por grupo para que a pessoa Dev entregue e o grupo revise por história. Texto e inventário em Markdown porque `.pen` não é diffável. Sem pasta `utils`, sem código.

## Complexity Tracking

Sem desvios: nenhuma dependência, tabela ou módulo novo.

## Riscos e pré-condições

- **Conexão com o pen.dev**: nesta sessão o servidor de ferramentas respondeu "transport not connected to app" — o aplicativo precisa estar aberto e conectado para qualquer criação ou leitura de desenho. As ferramentas `read_skill`/`get_style` ainda **não foram consultadas**. O site oficial informa exportação de quadros (PDF, PNG, JPEG, WEBP) e extensões para VS Code e Cursor, mas as capacidades reais (variáveis, componentes, exportação) serão confirmadas na primeira tarefa de execução; se algo não existir, a entrega usa a alternativa descrita em `research.md` (D8). O site não menciona Git: os `.pen` entram no repositório como arquivos comuns.
- **Revisão sem diff**: como `.pen` não aparece no diff, a consistência depende de `INDEX.md` e `textos.md`; uma tarefa dedicada confere que inventário, textos e quadros batem.
- **Dependência do contrato**: telas que mostram algo que a API ainda não prevê (ex.: comparação de pendências) ficam anotadas como "a confirmar com a história de API", não inventadas.
