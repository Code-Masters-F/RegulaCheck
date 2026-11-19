# Resultados da verificação (feature 025)

Execução em 19/11/2026, Windows, JDK Temurin 25.0.2 (o `CLAUDE.md` propõe 21; o build do projeto roda com o JDK instalado). Scripts auxiliares ficaram no scratchpad da sessão, fora do repositório.

## Build e testes (`backend/`, `.\mvnw.cmd clean verify`)

| Momento | Execuções (s) | Mediana | Resultado |
|---|---|---|---|
| Antes | 52, 51, 62 | 52 s | BUILD SUCCESS; 775 testes, 0 falhas, 36 ignorados (opt-in Oracle) |
| Depois | 49, 51, 48 | 49 s | BUILD SUCCESS; 775 testes, 0 falhas, 36 ignorados |

Ruído observado no baseline: 62 s contra 51 s (cerca de 20%). A mediana depois não é maior que a de antes. **Não há alteração em `backend/`**, então a igualdade era esperada; a medição não prova melhoria.

## Contratos

| Item | Antes e depois |
|---|---|
| `docs/api/openapi.yaml` | `a45b14d4…593b`, idêntico |
| `docs/api/RegulaCheck.postman_collection.json` | `9c995349…cf4`, idêntico |
| `git diff --stat` em `backend/`, `database/`, `.specify/`, `.agents/`, `.claude/`, `docs/evidencias/`, `specs/` anteriores | vazio |

## Links relativos e caminhos citados (escopo: documentos fora de `specs/`)

Baseline: 20 apontamentos em 415 verificados. Depois: 20 (mais 1 por `verificacao-resultados.md`, que passa a existir com este arquivo). **Nenhum foi corrigido porque nenhum é link quebrado de verdade:**

- `design/previas/`, `design/referencias.md`, `scripts/configurar-refcat.ps1`, `mobile/design/checklist-revisao.md`: arquivos removidos em 14/11/2026, citados como histórico (`docs/decisoes.md` explica).
- `build/outputs/apk/...`: artefato gerado, não versionado.
- `docs/evidencias/T06-02/`: pasta que a execução do plano criará.
- `ui/*.kt`, `mobile/app/.../ui/Textos.kt`, `extracao-simulada/catalogo.json`: caminhos abreviados dentro do pacote ou do classpath.

Em `specs/`: 606 apontamentos pela mesma heurística (caminhos de arquivos planejados nos artefatos históricos). Não tratados: fora do escopo (D6).

## Convenções (verificação automática de H1, caminho absoluto, salto de nível, segredo)

Desvios encontrados e corrigidos: `CONTRATO_TESTES.md` sem H1 e sem propósito; caminho de máquina com nome de pessoa em `design/README.md`; salto de nível `##` → `####` em `docs/testes/plano-de-testes.md` (P01). Restam: datas ISO em prosa (a heurística também pega exemplos JSON, não revisado caso a caso) e `C:/Users/Public/rcgt` em `docs/ambiente.md` (contorno documentado, mantido). Convenções C5, C7 a C9 e C12 **não foram verificadas por script nem por leitura completa**.

## Redundância (8 palavras iguais entre arquivos, `docs/` e raiz sem `specs/`, `samples/conjuntos`, evidências)

Antes: 377 de 4289 linhas não vazias. Depois: 369 de 4296. **Redução de 2%; a meta de 20% (SC-004) não foi atingida.** A maior parte da sobreposição restante é legítima: `resultado-da-rodada.md` repete os títulos dos casos do plano (tabela de resultados), e o plano repete resultados esperados que descrevem o contrato. Consolidar isso removeria informação. A redundância real eliminada foi o parágrafo de situação por RC e as linhas longas de `CONTRATO_API.md`, que repetiam `docs/api/README.md` e o `openapi.yaml`.

## Casos do plano

Diff em `docs/testes/plano-de-testes.md`: 1 linha (nível do título P01). Nenhum caso, ID ou esperado alterado.

## Pendências

- SC-004 não atingido (acima). Decidir com o grupo se a meta deve mudar ou se há outro alvo de consolidação.
- Leitura integral de `docs/arquitetura.md`, `ambiente.md`, `decisoes.md`, `modelo-de-dados.md`, `protocolo-academico.md`, `mobile/`, `database/` e `samples/` **não foi feita**; foram avaliados apenas por medição de sobreposição e por scripts. Divergências de conteúdo com o código podem existir nesses arquivos.
- `AGENTS.md` e `CLAUDE.md` não foram editados (nenhuma contradição comprovada), só o ponteiro do plano em `CLAUDE.md`.
- Nenhuma alteração de código, JavaDoc ou SQL foi feita (T021 a T023).
