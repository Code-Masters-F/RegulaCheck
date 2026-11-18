# Implementation Plan: Exibir resultado e orientar a correção (RC-15)

**Branch**: `019-exibir-resultado-correcao` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Especificação da RC-15 em `specs/019-exibir-resultado-correcao/spec.md` (issue #37).

## Summary

Recorte backend autorizado em 17/11/2026: acrescentar ao DTO da análise existente um rótulo textual do resultado e orientação segura por pendência, preservando resultado, responsável, revisão, resumo e evidências da RC-13/RC-14. Nenhuma rota, tabela, regra ou adaptador novo. Apresentação Android e validação de usabilidade ficam pendentes.

## Recorte backend da implementação atual

- `AnaliseResponse`: novo `rotuloResultado` derivado de `resultadoGeral`, e `orientacao` derivada de código/tipo/motivo da pendência. O texto não altera o responsável da regra nem infere erro no original.
- RC-R02: conferir a divergência; se o erro estiver no documento médico original, solicitar correção à unidade emissora.
- Pendência `NAO_VERIFICAVEL`: conferir a transcrição ou reenviar arquivo legível quando possível; não afirmar ausência.
- RC-R01/RC-R03/RC-R06: orientar obtenção/envio do arquivo necessário, sem prometer que a inclusão está disponível na etapa atual.
- RC-R04/RC-R05: solicitar revisão ou novo documento à unidade emissora.
- Testes de DTO e endpoint com dados sintéticos, incluindo vigência, resumo e autorização já existentes. Atualizar contrato e OpenAPI. Nenhum campo novo persistido.

## Technical Context

**Language/Version**: Java 21 no backend existente. O app Android em Kotlin continua planejado, sem implementação neste recorte.

**Primary Dependencies**: DTO e API de análise RC-13 e resumo RC-14 existentes; sem biblioteca, provedor ou serviço novo.

**Storage**: Nenhuma persistência nova. Os campos textuais são derivados da análise já gravada.

**Testing**: Testes de endpoint/DTO com respostas sintéticas, regressão de histórico, resumo e autorização, e build do backend. Testes Android e avaliação de usabilidade SC-005 permanecem pendentes.

**Target Platform**: API Java existente; cliente Android futuro.

**Project Type**: Backend Java em monólito modular; extensão aditiva do DTO da análise. Motor de regras e schema permanecem iguais.

**Performance Goals**: Campos textuais gerados no mapeamento da resposta, sem consulta ou chamada externa adicional. A SC-005 depende de cliente e participantes futuros.

**Constraints**: Somente dados sintéticos; protocolo fictício e resumo sempre identificados; texto de documentos tratado como dado; estados nunca somente por cor. Preservar o responsável da regra e apresentar condicionalmente a correção do documento original pela unidade emissora, sem inferir erro clínico de mera divergência. Não oferecer acréscimo de arquivo após a primeira análise enquanto a API permitir apenas substituição/remoção nessa situação; explicar a limitação na tela.

**Scale/Scope**: Respostas de análise vigente e histórica já existentes, até seis critérios do protocolo atual por análise e histórico de até 50 análises. Reenvio, reanálise, submissão e telas estão fora do recorte backend.

## Constitution Check

*GATE: verificado antes da pesquisa e novamente após o desenho dos artefatos abaixo.*

| Princípio | Decisão de conformidade |
|---|---|
| I. Escopo acadêmico | Aviso do protocolo fictício e do resumo simulado; “sem pendências” sem promessa de aceitação. |
| II. Limites clínicos | Se houver erro no original, a paciente solicita revisão à unidade emissora; RC-R02 preserva `PACIENTE` como responsável registrado pela regra sem inferir erro clínico de divergência isolada. Nenhuma edição do original ou diagnóstico. |
| III. Backend modular | Reutiliza a API Java existente; nenhuma mudança de arquitetura ou dependência no backend. |
| IV. Autorização | Consome rotas privadas com a sessão da paciente e trata 401/403/404 sem expor pedido alheio. |
| V. IA subordinada | Resultado e pendências determinísticos têm precedência; resumo padrão ou ausente não os altera. |
| VI. Revisões | Seleciona análise e resumo da mesma revisão; marca resultado anterior sem torná-lo vigente. |
| VII. Verificação honesta | Testes e build do backend têm evidência no quickstart; app, usabilidade e ações de tela seguem pendentes. |
| VIII. Mudanças mínimas | Reutiliza design v1.0 e contratos existentes; alteração de API só se uma lacuna real for demonstrada. |

**Gate inicial**: aprovado para planejar; nenhuma violação justificada. **Gate após Phase 1**: aprovado para desenho; execução depende da base Android e da verificação dos dados reais da API. Nenhuma exceção à constituição foi introduzida.

## Project Structure

### Documentation (this feature)

```text
specs/019-exibir-resultado-correcao/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── data-model.md
├── contracts/apresentacao-resultado.md
└── quickstart.md
```

`tasks.md` é produto da etapa `$speckit-tasks` e não é criado por este plano.

### Source Code (repository root)

No recorte backend atual, os arquivos alterados são `backend/src/main/java/br/com/regulacheck/analise/dto/AnaliseResponse.java`, `OrientacaoDaPendencia.java` no mesmo pacote e `backend/src/test/java/br/com/regulacheck/analise/OrientacaoResultadoEndpointTest.java`. A estrutura móvel abaixo permanece referência para trabalho futuro, sem arquivos Android criados nesta entrega.

```text
mobile/
├── README.md                 # estado atual: projeto Android ainda não criado
├── design/
│   ├── design-system.md
│   ├── acessibilidade.md
│   ├── textos.md
│   └── dados-ficticios.md
└── app/                      # base prevista pela feature 017; ausente na main
    └── app/src/
        ├── main/java/br/com/regulacheck/app/
        │   ├── api/           # cliente e modelos de resposta
        │   ├── resultado/     # tela, estado e detalhe da pendência da RC-15
        │   └── MainActivity.kt
        └── test/java/br/com/regulacheck/app/resultado/
backend/src/main/java/br/com/regulacheck/
├── analise/                 # resultado, pendências e evidências da RC-13
└── resumo/                  # resumo simulado da RC-14
design/RegulaCheck.pen       # fonte nativa, editada apenas pelas ferramentas pen.dev
```

**Structure Decision**: Os caminhos Android acima são o alvo previsto pela feature 017, ainda não integrada à `main`. A primeira tarefa deve confirmar a base e ajustar plano e tarefas se a estrutura entregue diferir, antes de alterar código. A RC-15 acrescenta a apresentação nesse app; os dados continuam nos contratos atuais do backend.
