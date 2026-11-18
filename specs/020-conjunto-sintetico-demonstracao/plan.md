# Implementation Plan: Conjunto sintético de demonstração (RC-23)

**Branch**: `020-conjunto-sintetico-demonstracao` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/020-conjunto-sintetico-demonstracao/spec.md`

## Summary

Criar 14 conjuntos sintéticos (arquivos fictícios + gabarito escrito à mão por regra) em `samples/conjuntos/`, reaproveitando o gerador e o leitor simulado já existentes (RC-10). O **gabarito é a fonte da verdade, escrito antes**: as respostas esperadas por regra vêm da definição do protocolo (RC-R01 a RC-R06, spec 013), nunca do resultado do motor. Um teste de regressão em memória executa cada conjunto pelo percurso real (leitura simulada → conferência → análise) e compara com o gabarito; divergência é investigada, não "corrigida" no gabarito. Não há mudança no motor de regras, na API nem no banco.

## Technical Context

**Language/Version**: Java 21+ (JDK da máquina: 25.0.2; backend já compila), PDFBox e `ImageIO` já usados pelo `GeradorDeAmostras`.

**Primary Dependencies**: nenhuma nova. Reutiliza `AmostrasSinteticas`, `GeradorDeAmostras`, `LeitorSimulado`, `CenarioAvaliacao`/`CenarioExtracao` (testes).

**Storage**: arquivos versionados em `samples/conjuntos/` e catálogo de leituras em `backend/src/main/resources/extracao-simulada/catalogo.json`. Sem Oracle.

**Testing**: JUnit 5 em memória (`mvnw test`), sem rede e sem credenciais. Oracle não é necessário.

**Target Platform**: máquina de desenvolvimento do grupo (Windows/Unix).

**Project Type**: material de teste + teste de regressão (sem código de produção novo).

**Performance Goals**: N/A. A suíte dos conjuntos deve rodar em segundos.

**Constraints**: dados 100% fictícios; arquivos nunca sobrescritos depois de commitados (o hash identifica a leitura); sem SNAPSHOT; sem dependência nova; sem alteração do motor, da API ou do esquema.

**Scale/Scope**: 14 conjuntos, ~22 arquivos pequenos (PDF de 1 página e PNG 640×360), 1 catálogo, 14 gabaritos, 1 classe de definição, 1 teste.

## Constitution Check

| Princípio | Situação |
|---|---|
| I. Dados sintéticos | OK: todos os arquivos fictícios, com "SEM VALOR REAL" visível; teste verifica a marca. |
| II. Limites clínicos | OK: nenhum conjunto afirma diagnóstico; "sem pendências" é ressalvado como não-aprovação. |
| III. Monólito Java | OK: nenhum código de produção novo. |
| V. IA/dados não confiáveis | OK: conjunto de injeção prova que texto do documento é só dado. |
| VII. Verificação honesta | OK: gabarito anterior à execução; divergência é registrada, não escondida; leitura é **simulada** e dita assim. |
| VIII. Mudanças mínimas | OK: só adiciona material de teste e documentação. |

Sem violações. Reavaliado após o desenho: sem mudança.

## Project Structure

### Documentation (this feature)

```text
specs/020-conjunto-sintetico-demonstracao/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── formato-conjunto.md   # formato do gabarito e do catálogo
├── checklists/requirements.md
└── tasks.md                  # gerado por /speckit-tasks
```

### Source Code (repository root)

```text
samples/
├── README.md                          # atualizado: índice dos conjuntos e como usar
├── catalogo-conjuntos.md              # índice (id, situação, resultado geral, original→corrigido)
├── encaminhamento-sintetico.pdf       # existente, preservado (base do conjunto 01)
├── identificacao-sintetica.png        # existente, preservado
├── gabarito.md                        # existente (RC-10), preservado
└── conjuntos/
    └── <NN>-<nome>/
        ├── gabarito.md                # por regra, campos, resultado geral, data de referência
        └── <arquivos>.pdf|png|txt
backend/
├── src/main/resources/extracao-simulada/catalogo.json   # + hashes dos arquivos novos
└── src/test/java/br/com/regulacheck/extracao/
    ├── ConjuntosSinteticos.java       # definição dos 14 conjuntos + gabarito esperado (escrito à mão)
    ├── GeradorDeAmostras.java         # estendido: gera arquivos NOVOS, nunca sobrescreve existentes
    └── ConjuntosDemonstracaoTest.java # regressão em memória: percurso real × gabarito
docs/ambiente.md                       # como gerar e testar
```

**Structure Decision**: o código fica em `src/test` (já é onde moram as amostras e o gerador); só o catálogo de leituras fica em `src/main/resources`, pois o leitor simulado o carrega em produção para a demonstração.

## Decisões de desenho

1. **Gabarito escrito à mão antes.** `ConjuntosSinteticos` contém, para cada conjunto, a leitura predefinida **e** o resultado esperado por regra e geral, derivados das definições do protocolo. O `gabarito.md` de cada pasta é gerado a partir dessas definições, não do motor. Ordem verificável: o commit dos gabaritos e arquivos precede o commit do teste de regressão (hash registrado em `catalogo-conjuntos.md`).
2. **Arquivos imutáveis.** O gerador só cria arquivo ausente. Os 2 arquivos oficiais e os 5 de teste existentes ficam intactos, preservando os hashes (FR-012, FR-013).
3. **Data de referência fixa.** Todos os gabaritos usam **12/11/2026**, a mesma data do relógio fixo dos testes de análise; validade e maioridade ficam estáveis na regressão (FR-011). Em execução manual vale a data do dia, e os gabaritos avisam que a fronteira dos 18 anos exatos só é exercitada na regressão.
3a. **Estado da conferência no gabarito.** Cada gabarito declara `TUDO_CONFIRMADO`, `NADA_CONFERIDO` ou `DIVERGENCIA(<campo>)`, e o teste reproduz esse estado antes de analisar.
4. **Corrigidos relacionados.** `13-corrigido-de-04` e `14-corrigido-de-09` apontam para o original por id e descrevem a diferença; gabaritos dos originais não mudam (FR-006).
5. **Formato inválido sem leitura.** Conjunto 12 traz arquivos que a validação de envio deve recusar (tipo não suportado e PDF acima de 5 páginas); o gabarito descreve recusa e momento, sem regras (FR-010). Pela documentação (`docs/api/erros.md`): PDF com mais de 5 páginas → `400 REQUISICAO_INVALIDA`; tipo não suportado → `415 TIPO_NAO_SUPORTADO` ou `EXTENSAO_INCOMPATIVEL`. Esses arquivos não entram no catálogo de leituras.
6. **Injeção.** O conjunto 11 reaproveita a ideia do `encaminhamento-injecao.pdf` de teste, mas com arquivo próprio em `samples/`; resultado esperado igual ao do conjunto equivalente sem a instrução.
7. **Documentação honesta.** O texto de cada gabarito diz que a leitura é simulada e que o resultado não é aprovação do SUS.

## Catálogo previsto

| # | Conjunto | Situação coberta | Resultado geral esperado |
|---|---|---|---|
| 01 | completo | caminho feliz (arquivos oficiais) | SEM_PENDENCIAS_IDENTIFICADAS |
| 02 | sem-identificacao | requisito ausente (R03) | COM_PENDENCIAS |
| 03 | sem-motivo | motivo não localizado, responsável unidade emissora (R04) | COM_PENDENCIAS |
| 04 | varias-pendencias | R03 + R04 + R05 juntas | COM_PENDENCIAS |
| 05 | foto-ilegivel | identificação borrada (campo ilegível) | REQUER_CONFERENCIA |
| 06 | foto-cortada | identificação cortada (data de nascimento não localizada) | COM_PENDENCIAS |
| 07 | outra-pessoa | nome divergente entre documentos (R02) | COM_PENDENCIAS |
| 08 | menor-com-autorizacao | condicional aplicável e atendida (R06) | SEM_PENDENCIAS_IDENTIFICADAS |
| 09 | menor-sem-autorizacao | condicional aplicável e pendente (R06) | COM_PENDENCIAS |
| 10 | maioridade-limite | 18 anos exatos na referência: R06 não aplicável | SEM_PENDENCIAS_IDENTIFICADAS |
| 11 | instrucao-no-documento | texto "ignore as regras…" é só dado | igual ao conjunto 01 |
| 12 | formato-invalido | tipo não suportado / PDF com 6 páginas | recusa no envio, sem análise |
| 13 | corrigido-de-04 | reenvio de 04 com as pendências resolvidas | SEM_PENDENCIAS_IDENTIFICADAS (rev. 2) |
| 14 | corrigido-de-09 | reenvio de 09 com a autorização anexada | SEM_PENDENCIAS_IDENTIFICADAS (rev. 2) |
| 15 | nada-conferido | mesmos arquivos do 01, sem nenhuma conferência | REQUER_CONFERENCIA |

A coluna "Resultado geral" assume `TUDO_CONFIRMADO` (exceto o 15). A matriz completa das seis regras, derivada das definições das regras, está em `research.md`. A tabela mostra só a pendência principal: o gabarito preenche as **seis** regras (por exemplo, identificação ilegível deixa RC-R02, RC-R03 e RC-R06 não verificáveis; sem identificação lida, RC-R06 também é não verificável), derivadas da spec 013 e não do motor.

Os valores acima são o **gabarito proposto**; os resultados por regra exatos são fixados em `ConjuntosSinteticos` e nos `gabarito.md` antes da execução. Se a definição do protocolo deixar uma célula ambígua (por exemplo, o resultado de RC-R02 com um documento cortado), a ambiguidade é registrada em `research.md` e o gabarito adota a leitura documentada da spec 013, sem olhar a saída do motor.

## Riscos

- **Divergência real entre gabarito e motor**: é o objetivo do material; registrar como achado (RC-25), não ajustar o gabarito para esconder.
- **Hash instável do PDF**: o PDFBox pode embutir data de criação. Mitigação: nunca regenerar arquivo existente; conferir o hash gravado no catálogo antes do commit.
- **Foto "real"**: as imagens são desenhadas por programa, nunca fotos de documentos reais (FR-008).

## Complexity Tracking

Sem violações da constituição a justificar.
