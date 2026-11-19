# Feature Specification: Exibir resultado e orientar a correção (RC-15)

**Feature Branch**: `019-exibir-resultado-correcao`

**Created**: 2026-11-16

**Status**: Implementada e verificada: backend no PR #70 (17/11/2026; 695 testes, 0 falhas; ver `quickstart.md`) e apresentação no app pela RC-19 ([spec 022](../022-app-android-documentos-resultado/spec.md), PR #74), verificada com o backend real em 17/11/2026. O SC-005 (avaliação com pessoas) acompanha a RC-26

**Input**: Issue [RC-15 #37](https://github.com/Code-Masters-F/RegulaCheck/issues/37): como paciente, entender cada pendência e distinguir o que posso resolver do que exige retorno à UBS. Exibir resultado, resumo e pendências sem depender só de cor; mostrar critério, versão do protocolo e trecho de origem quando disponível; orientar reenvio para foto cortada ou arquivo ausente e procura da unidade emissora para informação clínica ausente ou documento médico incorreto; esclarecer que ausência de pendências não garante aceitação e que o protocolo é demonstrativo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entender o resultado (Priority: P1)

A paciente abre a verificação do próprio pedido e vê o resultado geral, o resumo identificado como simulado e as pendências. Os estados são nomeados por texto, com aviso claro quando a leitura precisa de conferência ou quando nenhuma pendência foi identificada.

**Why this priority**: Sem o resultado compreensível, a paciente não sabe se precisa corrigir algo.

**Independent Test**: Com pedidos sintéticos analisados nos três resultados possíveis, abrir cada resultado e conferir título textual, resumo disponível, lista coerente com a análise e avisos.

**Acceptance Scenarios**:

1. **Given** uma análise vigente com pendências, **When** a paciente abre o resultado, **Then** vê “Com pendências”, o resumo com selo “SIMULADO” e cada pendência com critério, situação e responsável registrado pela regra, sem depender da cor para entendê-los.
2. **Given** uma análise vigente que requer conferência, **When** abre o resultado, **Then** vê “Confira a leitura” e entende que dado não verificável não significa dado ausente no documento.
3. **Given** uma análise vigente sem pendências identificadas, **When** abre o resultado, **Then** vê esse resultado e o aviso de que não há aprovação garantida, vaga prometida nem critérios oficiais do SUS.
4. **Given** uma análise vigente cujo resumo está indisponível, **When** abre o resultado, **Then** vê a explicação padrão identificada, as pendências originais e uma opção de tentar obter o resumo novamente.

---

### User Story 2 - Identificar a correção adequada (Priority: P1)

A paciente abre uma pendência e encontra a regra avaliada, a versão do protocolo, a evidência disponível e uma orientação compatível com quem pode resolver o problema.

**Why this priority**: Uma instrução errada pode levar a paciente a alterar dados clínicos ou reenviar um arquivo sem resolver a pendência.

**Independent Test**: Abrir detalhes sintéticos de arquivo incompleto, arquivo ausente, informação não verificável e informação clínica ausente; conferir a origem e a orientação atribuída a cada caso.

**Acceptance Scenarios**:

1. **Given** uma pendência ligada a foto cortada ou arquivo ausente, **When** a paciente abre o detalhe, **Then** recebe orientação para reenviar ou substituir o arquivo somente se a operação estiver disponível; se o arquivo ausente não puder ser anexado nessa etapa, vê a limitação e a necessidade de obter o arquivo antes de seguir o fluxo de reenvio, sem botão inoperante.
2. **Given** uma pendência de informação clínica ausente ou documento médico incorreto, **When** abre o detalhe, **Then** é orientada a procurar a unidade emissora e não recebe opção de preencher ou alterar o documento clínico original no aplicativo.
3. **Given** uma pendência com trecho de origem disponível, **When** abre o detalhe, **Then** vê o critério, a versão do protocolo, o documento e o trecho correspondente, apresentados como informação do documento.
4. **Given** uma pendência sem trecho de origem, **When** abre o detalhe, **Then** vê o critério, a versão e a orientação cabível sem trecho inventado nem afirmação de que um campo não localizado inexiste no original.
5. **Given** RC-R02 com `responsavel=PACIENTE` e divergência entre documentos, **When** a paciente abre o detalhe, **Then** vê a responsabilidade registrada pela regra, orientação para conferir os documentos e a condição “se o erro estiver no documento médico original, solicite correção à unidade emissora”; o app não afirma qual documento está errado nem oferece edição clínica.

---

### User Story 3 - Reconhecer validade e limites (Priority: P2)

Depois de uma alteração nos documentos ou na conferência, a paciente distingue o resultado anterior de uma análise vigente e mantém claros os limites acadêmicos da simulação.

**Why this priority**: Um resultado antigo ou demonstrativo não pode parecer uma decisão atual de regulação.

**Independent Test**: Abrir o resultado de uma revisão que deixou de ser vigente e verificar a identificação de resultado anterior e os avisos acadêmicos.

**Acceptance Scenarios**:

1. **Given** uma análise que perdeu vigência após mudança no pedido, **When** a paciente a consulta, **Then** vê “Resultado anterior” e a revisão avaliada; as pendências antigas não são apresentadas como conclusão atual.
2. **Given** qualquer resultado exibido, **When** a paciente lê a orientação, **Then** encontra identificação de simulação e do protocolo acadêmico fictício, sem diagnóstico, prescrição, urgência, promessa de vaga ou garantia de aprovação.

### Edge Cases

- Sem análise concluída ou análise ainda em andamento: mostrar estado próprio; não inferir resultado nem pendências.
- Falha ao consultar análise ou perda de conexão: explicar a indisponibilidade e oferecer nova consulta sem declarar conclusão.
- Resumo ausente ou padrão: preservar o resultado e a lista determinística; permitir nova tentativa quando cabível.
- Pendência sem documento, página ou trecho associado: omitir a origem inexistente e manter regra, versão e orientação.
- Texto de origem com símbolos como `<`, `>` e `&`: exibi-los como conteúdo do documento, sem interpretar instruções ou marcação.
- Resultado de revisão antiga: marcar a falta de vigência e não combinar resumo ou pendências de revisões diferentes.
- Responsável indicado e ação disponível divergentes: mostrar o responsável e uma orientação segura; não oferecer ação que altere informação clínica original.
- RC-R02 registra `PACIENTE` mesmo quando o erro está no documento original; distinguir a providência da paciente (solicitar revisão) de quem corrige o documento (unidade emissora), sem reclassificar a regra na interface.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Somente a paciente autorizada pode consultar o resultado e os detalhes das análises do próprio pedido.
- **FR-002**: A apresentação MUST usar o resultado geral, o resumo e as pendências associados à mesma análise e revisão; MUST distinguir análise vigente de resultado anterior.
- **FR-003**: A apresentação MUST identificar em texto os resultados “Com pendências”, “Confira a leitura” e “Sem pendências identificadas”; a compreensão de estado, situação e ação MUST NOT depender somente de cor.
- **FR-004**: Quando disponível, o resumo MUST aparecer identificado como “SIMULADO”. Resumo ausente ou padrão MUST NOT ocultar ou substituir o resultado e as pendências determinísticas; quando cabível, a paciente MUST poder solicitar nova tentativa.
- **FR-005**: Cada pendência exibida MUST permitir consultar código e descrição do critério, versão do protocolo, tipo de pendência, explicação e responsável registrado pela regra; quando a correção do documento original depender da unidade emissora, a próxima ação MUST distinguir essa atribuição do valor `responsavel` da API.
- **FR-006**: O detalhe MUST apresentar documento, página e trecho de origem somente quando esses dados estiverem disponíveis na evidência da análise; MUST NOT fabricar trecho nem tratar “não localizado” como prova de ausência no original.
- **FR-007**: Para arquivo ausente, ilegível ou comprovadamente incompleto, a orientação MUST indicar reenvio ou substituição compatível com as operações disponíveis no estado do pedido; se inclusão de arquivo estiver indisponível após a primeira análise, MUST explicar essa limitação sem oferecer botão inoperante e orientar a obter o arquivo para o fluxo de reenvio quando disponível. MUST distinguir isso de correção do conteúdo clínico.
- **FR-008**: Para informação clínica ausente ou documento médico incorreto, a orientação MUST indicar que a paciente solicite revisão à unidade emissora, responsável por corrigir o original; MUST NOT oferecer edição do documento clínico original pela paciente. Quando a API só indicar `NAO_LOCALIZADO` ou divergência entre documentos, a orientação MUST ser condicional (“se o erro estiver no original”) e MUST NOT afirmar que o original está errado.
- **FR-009**: A apresentação de resultado sem pendências MUST explicar que isso não garante aceitação pela regulação e que os critérios são demonstrativos, não requisitos oficiais do SUS.
- **FR-010**: Em qualquer resultado, a apresentação MUST identificar o caráter acadêmico e simulado e MUST NOT diagnosticar, prescrever, definir urgência, prometer vaga, prazo ou aprovação.
- **FR-011**: Sem análise concluída, durante processamento ou em falha de consulta (rede/503), MUST haver estado textual próprio e opção de consultar novamente quando possível, sem apresentar conclusão não confirmada; 401/403/404 MUST ter tratamento textual de sessão expirada, acesso negado ou pedido não encontrado, sem expor pedido alheio.
- **FR-012**: Trechos e outros conteúdos provenientes de documentos MUST ser tratados como dados não confiáveis e apresentados como texto, sem executar marcação ou instruções neles contidas.

### Key Entities *(include if feature involves data)*

- **Análise**: resultado geral, revisão avaliada, vigência, momento da avaliação e versão do protocolo.
- **Resumo simulado**: explicação associada à análise, com indicação de simulação e origem gerada ou padrão.
- **Pendência**: critério, tipo, explicação, responsável e evidências da análise.
- **Evidência**: referência ao documento e, quando disponível, campo, situação, página e trecho de origem.
- **Pedido**: contexto da paciente e revisão atual, usado para distinguir resultado vigente de anterior.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% dos cenários sintéticos dos três resultados gerais, a paciente encontra o estado em texto e vê pendências iguais às da análise consultada, sem mistura de revisões.
- **SC-002**: Em 100% das pendências dos cenários de aceitação, o detalhe mostra critério, versão, situação e responsável; trecho aparece apenas quando existe evidência de origem.
- **SC-003**: Nos cenários de arquivo ausente/incompleto e informação clínica ausente/documento médico incorreto, 100% das orientações preservam o responsável da regra e apontam a próxima ação segura, condicional quando a origem do erro não foi comprovada; 0 oferecem edição clínica pela paciente ou ação de arquivo indisponível.
- **SC-004**: Em 100% dos resultados sem pendências, o aviso nega garantia de aceitação e identifica os critérios como demonstrativos; em 100% dos resumos exibidos, a simulação está identificada.
- **SC-005**: Em uma avaliação de usabilidade com pelo menos 3 pessoas que não desenharam o fluxo, ao menos 2 conseguem dizer, em até 2 minutos por cenário, qual pendência podem resolver e qual exige a unidade emissora; registrar resultados reais, sem presumir aprovação.

## Aceite do recorte backend (17/11/2026)

- `POST /solicitacoes/{id}/analises`, `GET /solicitacoes/{id}/analises` e `POST /solicitacoes/{id}/resumo` devolvem `rotuloResultado` coerente com um dos três valores de `resultadoGeral` e `pendencias[].orientacao` textual, sem alterar campos existentes.
- RC-R02 preserva `responsavel=PACIENTE` e condiciona a procura da unidade emissora ao erro estar no documento original. `NAO_VERIFICAVEL` orienta conferência e não afirma ausência. RC-R04/RC-R05 orientam a unidade emissora.
- O backend não afirma que uma ação de reenvio está disponível, não diagnostica e não modifica resultado, evidência, revisão, resumo ou autorização por recurso. Testes de endpoint e regressão do backend verificam esses pontos.
- Estes critérios são apenas para a entrega da API. As histórias de exibição e a SC-005 permanecem pendentes até existir interface e avaliação real.

## Assumptions

- Recorte de implementação autorizado em 17/11/2026: entregar no backend rótulo textual do resultado e orientação segura por pendência na resposta existente de análise. A exibição em telas, ações de navegação, acessibilidade Android e SC-005 permanecem fora desta entrega e não serão declaradas concluídas.
- Esta história cobre a apresentação e a orientação do resultado da RC-15. Geração do resumo é da RC-14; reenvio, reanálise e submissão pertencem às histórias seguintes. As ações mostradas aqui respeitam as operações já disponíveis ao pedido.
- Os quadros E3.3 e E3.4 de `specs/015-fluxo-telas-app/` e os textos de `mobile/design/` são referência de linguagem e fluxo, não prova de implementação ou validação de usabilidade.
- A análise da RC-13 fornece regra, versão, pendência, responsável e evidência; o trecho de origem pode faltar. A RC-14 fornece resumo simulado ou explicação padrão. Ausências devem ser tratadas explicitamente.
- Somente dados sintéticos e protocolo acadêmico fictício são usados; “não localizado”, “ilegível” e “ausente” têm significados distintos.
