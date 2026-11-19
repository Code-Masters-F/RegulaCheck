# Feature Specification: Reenviar e conferir a versão corrigida (RC-16)

**Feature Branch**: `38-rc-16-reenviar-e-conferir-a-versão-corrigida` (branch já criada pelo grupo a partir da issue; diretório da feature: `specs/020-reenviar-versao-corrigida`)

**Created**: 2026-11-17

**Status**: Implementada no backend e verificada **sem banco** em 17/11/2026 (`clean verify`: 731 testes, 0 falhas, 32 ignorados; ver "Resultado da verificação" em `tasks.md`). Verificada também no Oracle real em 17/11/2026 (`ReenvioOracleTest` e `AnaliseOracleTest`: 3 testes verdes). Usada pela API (OpenAPI e Postman); a tela E3.5 está no protótipo visual ([escopo do app](../../docs/decisoes.md#adotadas))

**Backlog**: RC-16 — [Code-Masters-F/RegulaCheck#38](https://github.com/Code-Masters-F/RegulaCheck/issues/38) · Épico 5 (Orientação por IA e reenvio, #35) · História · P0 · A/B/C · 3 h · até 16/11/2026 · depende de RC-09 (substituir/remover documento), RC-13 (análises e histórico) e RC-15 (resultado e orientação), todas concluídas no backend

**Input**: User description: "RC-16 — Reenviar e conferir a versão corrigida. Como paciente, quero reenviar documentos corrigidos para verificar se as pendências foram resolvidas. Critérios de aceite: (1) Reenvio cria nova revisão e executa novamente extração, conferência, regras e resumo. (2) Pendência anterior é marcada como resolvida apenas após o novo resultado, nunca pelo simples upload. (3) Mostrar comparação simples entre pendências anteriores e atuais, preservando o histórico. (4) Reenvio com erro preserva o último conjunto íntegro e explica o que precisa ser repetido."

## Contexto e limite desta história

Depois da análise (RC-13), a paciente vê o resultado, o resumo simulado (RC-14) e a orientação de cada pendência (RC-15). Para corrigir, ela já consegue **substituir** ou **remover** um documento (RC-09): cada mudança cria a revisão seguinte e faz a análise vigente deixar de valer, sem mudar o estado do pedido e sem iniciar nada. A leitura (RC-10), a conferência (RC-11), a análise (RC-13) e o resumo (RC-14) existem como etapas separadas.

Falta fechar o ciclo de correção:

1. **Verificar a versão corrigida**: depois de corrigir, a paciente pede a verificação e o sistema refaz, para a revisão atual, a leitura, a avaliação da conferência, as regras e o resumo, parando só quando depender dela.
2. **Comparar**: o novo resultado mostra, por critério, o que foi resolvido, o que continua e o que é novo em relação à verificação anterior. Antes do novo resultado nada aparece como resolvido.
3. **Errar sem perder**: qualquer falha no caminho mantém o último resultado concluído intacto no histórico e diz qual etapa repetir.
4. **Incluir o que faltava**: hoje, depois da primeira análise, o sistema recusa documento novo; uma pendência de documento ausente (encaminhamento, identificação, autorização) só se corrige trocando um documento já enviado. A limitação foi registrada pela feature de telas (015) e pela RC-15 para ser decidida nesta história.

**Fica fora desta história:** submissão simulada (RC-17), telas do aplicativo e testes de usabilidade (épico 6 / RC-19), novas regras ou mudança de critérios do protocolo, resumo por provedor de IA real, leitura real (OCR) e qualquer alteração do documento clínico original pela paciente.

## Clarifications

### Session 2026-11-17

- Q: A paciente pode incluir um documento novo depois da primeira análise? → A: Sim. A inclusão passa a ser aceita em qualquer estado, exceto submetido em simulação (igual a substituir e remover), com o mesmo limite de 3 documentos e a mesma conferência de arquivo; cria a revisão seguinte e tira a vigência da análise, sem mudar o estado (decisão do responsável pela história, 17/11/2026).
- Q: A verificação da versão corrigida é uma ação única ou continua em etapas separadas? → A: Ação única. Uma nova ação de verificação refaz, para a revisão atual, leitura, avaliação da conferência, regras e resumo, parando sem registrar análise quando a paciente ainda precisa conferir campos; as etapas separadas existentes continuam disponíveis (decisão do responsável pela história, 17/11/2026).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Verificar a versão corrigida (Priority: P1)

A paciente recebeu um resultado com pendências e já trocou (ou incluiu, ou removeu) o documento que precisava corrigir. Ela pede a verificação da versão corrigida. O sistema lê os documentos da revisão atual (leitura simulada), avalia a conferência dessa leitura e, se ainda houver campo lido que só a paciente pode confirmar, para e informa que ela precisa conferir a leitura antes do novo resultado. Com a conferência resolvida, o sistema aplica as regras do protocolo, registra a nova análise, gera o resumo simulado e devolve o novo resultado com a comparação. Pedir de novo sem mudar nada devolve o mesmo resultado, sem criar outro.

**Why this priority**: É o núcleo da história (critério 1); sem ele a paciente não sabe se a correção resolveu as pendências.

**Independent Test**: Com o caso sintético de encaminhamento sem motivo (R04 pendente) analisado, substituir o encaminhamento pelo sintético completo, pedir a verificação (para na conferência), confirmar os campos e pedir de novo; conferir a nova revisão avaliada, o novo resultado, o resumo simulado e que a análise anterior continua no histórico.

**Acceptance Scenarios**:

1. **Given** um pedido com análise concluída da revisão N e um documento substituído depois dela (revisão N+1), **When** a paciente pede a verificação, **Then** os documentos da revisão N+1 são lidos e, havendo campos identificados sem confirmação, a resposta informa a etapa "conferência", a leitura a conferir e quantos campos faltam; nenhuma análise nova é registrada e nada aparece como resolvido.
2. **Given** a leitura da revisão N+1 conferida (todos os campos identificados com decisão), **When** a paciente pede a verificação, **Then** a leitura existente é reaproveitada (não é lida de novo), a análise da revisão N+1 é registrada como vigente pelas regras da RC-13, o resumo simulado é gerado e a resposta traz o novo resultado, o resumo identificado como simulado e a comparação com a análise anterior.
3. **Given** a leitura atual com campo ilegível, documento não lido ou divergência apontada (situações que a paciente não resolve conferindo), **When** a paciente pede a verificação, **Then** a análise é registrada mesmo assim, com as regras não verificáveis e seus motivos, e a orientação de cada pendência segue a RC-15.
4. **Given** uma análise vigente para a revisão e a conferência atuais, **When** a paciente pede a verificação de novo (inclusive por toque duplicado), **Then** recebe a mesma análise, indicada como já existente, sem análise nem resumo duplicados.
5. **Given** um pedido de outra paciente ou inexistente, **When** se pede a verificação, **Then** a resposta é a mesma de "não encontrado"; **Given** um pedido submetido em simulação, **Then** a verificação é recusada como somente consulta.
6. **Given** um pedido sem documentos disponíveis, **When** a paciente pede a verificação, **Then** é orientada a enviar um documento, sem nada registrado.

---

### User Story 2 - Comparar o resultado anterior com o novo (Priority: P1)

A paciente abre o novo resultado e vê, por critério, a comparação com a verificação anterior: **resolvida** (era pendente ou não verificável e agora atende ou não se aplica), **continua** (pendente ou não verificável nas duas) e **nova** (não era pendência e agora é). A análise anterior continua no histórico, sem alteração, como "resultado anterior". Enquanto não há novo resultado, nenhuma pendência aparece como resolvida, nem depois de substituir ou incluir um documento.

**Why this priority**: Critérios 2 e 3; é o que mostra à paciente se a correção funcionou, sem confundir envio de arquivo com resolução.

**Independent Test**: Gerar duas análises sintéticas do mesmo pedido com pendências diferentes (antes e depois de uma correção) e conferir a classificação de cada critério, a ausência de "resolvida" entre a correção e a nova análise, e o histórico intacto.

**Acceptance Scenarios**:

1. **Given** uma análise anterior com R04 e R05 pendentes e uma nova análise com todos os critérios aplicáveis atendidos, **When** a paciente consulta o novo resultado, **Then** R04 e R05 aparecem como "resolvida", com o resultado anterior e o atual de cada uma.
2. **Given** uma análise com pendências e um documento substituído ou incluído depois dela, **When** a paciente consulta o pedido e o histórico antes de uma nova análise, **Then** a análise aparece como resultado anterior (não vigente), com as mesmas pendências, e nenhum critério aparece como resolvido.
3. **Given** um critério pendente na análise anterior e pendente ou não verificável na nova, **When** a paciente consulta a comparação, **Then** ele aparece como "continua", com o resultado anterior e o atual.
4. **Given** um critério que atendia ou não se aplicava na análise anterior e é pendente ou não verificável na nova, **When** a paciente consulta a comparação, **Then** ele aparece como "nova".
5. **Given** a primeira análise do pedido, **When** é consultada, **Then** não há comparação (não existe verificação anterior), e isso não é tratado como erro.
6. **Given** um histórico com várias análises, **When** a paciente o consulta, **Then** cada análise traz a comparação com a que veio imediatamente antes, e nenhuma análise registrada tem resultados, pendências ou resumo alterados pela chegada das seguintes.

---

### User Story 3 - Incluir o documento que faltava depois da análise (Priority: P1)

A análise apontou documento ausente (por exemplo, o documento de identificação). A paciente inclui o documento que faltava no mesmo pedido, sem precisar trocar um documento correto, respeitando o limite de documentos. A inclusão cria a revisão seguinte, faz a análise vigente deixar de valer e não marca nada como resolvido.

**Why this priority**: Sem isso, uma pendência de documento ausente não tem correção possível depois da primeira análise (substituir o único documento enviado apagaria o encaminhamento), e a história não cumpriria o objetivo de verificar se as pendências foram resolvidas.

**Independent Test**: Analisar um pedido sintético só com o encaminhamento (R03 pendente), incluir o documento de identificação sintético e conferir a revisão nova, a perda de vigência da análise, o estado mantido e a ausência de "resolvida" até a nova verificação.

**Acceptance Scenarios**:

1. **Given** um pedido analisado com documento ausente e menos de 3 documentos, **When** a paciente envia o documento que faltava, **Then** o envio é aceito com a mesma conferência de arquivo do primeiro envio, a revisão aumenta em um, a análise vigente deixa de valer (continua no histórico) e o estado do pedido não muda.
2. **Given** um pedido analisado com 3 documentos, **When** a paciente tenta incluir outro, **Then** recebe a mesma recusa de limite do primeiro envio e nada muda.
3. **Given** um pedido submetido em simulação, **When** a paciente tenta incluir documento, **Then** recebe o conflito de somente consulta e nada muda.
4. **Given** a mesma tentativa de inclusão repetida por falha de conexão, **When** reenviada com a mesma identificação de tentativa, **Then** nenhum documento duplicado é criado.

---

### User Story 4 - Falha no reenvio preserva o último resultado íntegro e explica o que repetir (Priority: P2)

Se algo falha no caminho da correção (arquivo recusado, leitura indisponível, análise com erro, mudança dos documentos durante a verificação, resumo indisponível), o último resultado concluído continua íntegro no histórico, nenhum resultado parcial é registrado e a resposta diz em que etapa a falha ocorreu e o que a paciente precisa repetir.

**Why this priority**: Critério 4; protege a paciente de perder a referência do resultado anterior e de repetir passos desnecessários, mas depende das ações das US1 a US3.

**Independent Test**: Provocar, com dados sintéticos e substitutos de teste, cada falha (arquivo inválido, leitor indisponível, avaliação com erro, documento trocado durante a verificação, resumo inválido) e conferir a mensagem com a etapa, o histórico intacto e o sucesso ao repetir só a etapa indicada.

**Acceptance Scenarios**:

1. **Given** um pedido analisado, **When** a paciente tenta substituir ou incluir um arquivo inválido (formato, tamanho, páginas ou senha), **Then** a recusa explica o motivo e que é preciso enviar outro arquivo; documentos, revisão e análise vigente ficam exatamente como antes.
2. **Given** a leitura indisponível durante a verificação, **When** a paciente pede a verificação, **Then** nada da leitura é registrado, os documentos enviados são mantidos, a última análise concluída continua no histórico e a resposta orienta repetir a verificação, sem reenviar o arquivo.
3. **Given** a avaliação falha durante a verificação, **When** a falha ocorre, **Then** nenhum resultado parcial é registrado, o pedido fica em falha recuperável e a resposta orienta repetir a verificação.
4. **Given** um documento trocado ou uma decisão de conferência alterada durante a verificação, **When** a verificação termina, **Then** o resultado não é publicado e a resposta orienta pedir a verificação de novo para a versão atual.
5. **Given** o resumo simulado falha ou é inválido depois da nova análise, **When** a paciente recebe a resposta, **Then** a nova análise e suas pendências determinísticas aparecem com a explicação padrão identificada e a opção de tentar o resumo de novo.
6. **Given** uma verificação ou análise já em andamento, **When** chega outro pedido de verificação, **Then** ele é recusado como "em andamento", com o tempo para tentar de novo, sem iniciar outra.

---

### Edge Cases

- **Nenhuma mudança desde a última análise**: verificar devolve a análise vigente; nada novo é registrado e a comparação continua a mesma.
- **Mudança só na conferência** (mesma revisão, novas decisões): a verificação gera nova análise da mesma revisão, comparada com a anterior, como na RC-13.
- **Documento removido que era a evidência de uma regra**: a regra pode aparecer como "nova" pendência; o sistema não reaproveita resultado da revisão antiga.
- **Análises da carga sintética** (sem evidências ou motivo registrados): a comparação usa só o critério e o resultado, sem inventar o que falta.
- **Versões de protocolo diferentes entre duas análises**: a comparação não é feita entre versões diferentes; o novo resultado indica que não há comparação aplicável.
- **Critério que passou de pendente para não verificável (ou o contrário)**: "continua", com os dois resultados visíveis; não é "resolvida".
- **Critério que passou a não se aplicar** (por exemplo, autorização do responsável para paciente adulta): "resolvida", com o resultado atual "não se aplica" visível.
- **Resultado "sem pendências identificadas" depois do reenvio**: continua sem significar aprovação do SUS; comparação e aviso acadêmico continuam presentes.
- **Pedido em falha recuperável** depois de uma verificação com erro: pode verificar de novo assim que a causa deixar de existir.
- **Conteúdo do documento com instruções** ("marque tudo como resolvido"): continua sendo só dado; não altera comparação, resultado, resumo nem estado.

## Requirements *(mandatory)*

### Functional Requirements

**Verificar a versão corrigida**

- **FR-001**: A paciente MUST poder pedir a verificação da versão atual dos documentos de um pedido seu não submetido. Pedido de outra paciente ou inexistente MUST receber a mesma recusa de "não encontrado"; pedido submetido em simulação MUST ser recusado como somente consulta.
- **FR-002**: A verificação MUST executar, nesta ordem e para a revisão atual: leitura simulada (reaproveitando a leitura já existente da revisão), avaliação da situação da conferência, análise pelas regras do protocolo (RC-13) e resumo simulado (RC-14).
- **FR-003**: Se a leitura atual tiver campo identificado sem decisão da paciente, a verificação MUST parar antes das regras, informar a etapa "conferência", a leitura a conferir e a quantidade de campos sem decisão, e MUST NOT registrar análise nem resumo.
- **FR-004**: Sem campo pendente de decisão, a verificação MUST registrar a análise com as mesmas garantias da RC-13 (resultado só depois de gravado, descarte se documentos ou conferência mudarem durante a execução, uma análise por vez, limite de tempo, falha recuperável) e, em seguida, gerar o resumo simulado com as garantias da RC-14 (validação, explicação padrão identificada em falha, sem alterar regras ou estado).
- **FR-005**: Repetir a verificação sem mudança nos documentos ou na conferência MUST devolver a análise vigente, sem registrar outra análise nem outro resumo.
- **FR-006**: A resposta de uma verificação concluída MUST trazer o novo resultado (resultado geral, rótulo textual, resultados por regra, pendências com orientação, revisão avaliada, versão do protocolo), o resumo identificado como simulado e a comparação com a análise anterior, quando houver.

**Comparação e marcação de resolvida**

- **FR-007**: Cada análise registrada MUST poder ser apresentada com a comparação em relação à análise do mesmo pedido registrada imediatamente antes dela, por critério: "resolvida" (anterior pendente ou não verificável; atual atende ou não se aplica), "continua" (pendente ou não verificável nas duas) e "nova" (anterior atende ou não se aplica; atual pendente ou não verificável). Critérios que atendem ou não se aplicam nas duas MUST NOT aparecer na comparação.
- **FR-008**: Cada item da comparação MUST trazer o código e a descrição do critério e os resultados anterior e atual; a comparação MUST identificar a análise anterior (revisão e data da execução).
- **FR-009**: Uma pendência MUST só aparecer como "resolvida" na comparação de uma análise concluída e registrada. Enviar, substituir ou remover documento, ler ou conferir MUST NOT marcar pendência como resolvida nem alterar a comparação já apresentada.
- **FR-010**: A comparação MUST ser calculada a partir das análises registradas, sem alterar nenhuma delas; o histórico MUST continuar com todas as análises, seus resultados, pendências e resumos como foram registrados.
- **FR-011**: A primeira análise do pedido MUST ser apresentada sem comparação. Análises com versões de protocolo diferentes MUST NOT ser comparadas.

**Inclusão de documento após a análise**

- **FR-012**: A paciente MUST poder incluir documento em pedido seu em qualquer estado, exceto submetido em simulação, com a mesma conferência de arquivo, o mesmo limite de 3 documentos disponíveis e a mesma identificação de tentativa do primeiro envio.
- **FR-013**: A inclusão MUST criar a revisão seguinte com motivo de documento enviado e, na mesma operação, fazer a análise vigente deixar de valer (preservada no histórico), sem mudar o estado do pedido e sem iniciar leitura ou análise.

**Falhas e explicação do que repetir**

- **FR-014**: Nenhuma falha na correção ou na verificação MUST alterar, apagar ou substituir a última análise concluída; nenhum resultado parcial (leitura, análise ou resumo pela metade) MUST ser registrado.
- **FR-015**: A recusa de arquivo na substituição ou inclusão MUST manter documentos, revisão e análise vigente como estavam e explicar o motivo e que é preciso enviar outro arquivo.
- **FR-016**: Toda falha da verificação MUST informar a etapa em que ocorreu (leitura, conferência, análise ou resumo) e o que a paciente precisa repetir, sem expor detalhe técnico nem conteúdo de documento.
- **FR-017**: Falha do resumo depois de uma análise registrada MUST NOT desfazer a análise. Falha ou saída inválida do gerador simulado MUST seguir a RC-14 (explicação padrão identificada, com nova tentativa possível). Se o resumo não puder ser gravado (análise deixou de ser vigente durante a geração ou banco indisponível), a resposta MUST trazer a análise e suas pendências determinísticas sem resumo, indicando a etapa "resumo" e que é preciso pedir o resumo (ou a verificação) de novo.

**Limites, dados e registros**

- **FR-018**: Toda resposta da verificação e da comparação MUST manter a identificação de simulação e do protocolo acadêmico fictício; "sem pendências identificadas" e "resolvida" MUST NOT ser apresentados como aprovação do SUS; nenhuma mensagem diagnostica, define urgência ou promete vaga.
- **FR-019**: Valores e trechos dos documentos MUST continuar sendo apenas dados: não alteram comparação, resultado, resumo, estado nem mensagens.
- **FR-020**: Registros de funcionamento MUST conter só identificadores técnicos, etapas, códigos e contagens, nunca valores ou trechos dos documentos.
- **FR-021**: A paciente MUST continuar sem meio de alterar o documento clínico original pela aplicação; a correção de informação clínica segue a orientação de procurar a unidade emissora (RC-15).

### Key Entities *(include if feature involves data)*

- **Verificação da versão corrigida**: execução, a pedido da paciente, das etapas leitura → conferência → regras → resumo sobre a revisão atual; termina em novo resultado, em parada na conferência ou em falha com a etapa identificada. Não é registrada como entidade própria: o que fica registrado são a leitura, a análise e o resumo de cada etapa.
- **Comparação de análises (por pendência)**: relação calculada entre uma análise registrada e a análise registrada imediatamente antes dela no mesmo pedido; itens por critério com classificação "resolvida", "continua" ou "nova" e os resultados anterior e atual. Não é gravada; deriva das análises imutáveis.
- **Análise** (RC-13): fonte imutável dos resultados por regra; ganha a comparação na apresentação.
- **Documento** (RC-08/RC-09): passa a poder ser incluído depois da análise, criando nova revisão.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: No caso sintético de correção (encaminhamento sem motivo substituído pelo completo, ou identificação ausente incluída), a paciente chega ao novo resultado com comparação em no máximo 3 ações depois de enviar o arquivo corrigido (pedir verificação, conferir, pedir verificação), em 100% das execuções verificadas.
- **SC-002**: Em 100% dos cenários verificados entre o envio do arquivo corrigido e o novo resultado, 0 pendências aparecem como "resolvida".
- **SC-003**: Em 100% dos pares de análises sintéticas verificados, cada critério é classificado corretamente como resolvida, continua ou nova, e critérios sem pendência nas duas análises não aparecem.
- **SC-004**: Depois de N verificações com mudanças, o histórico mantém N análises com os dados originais (0 alteradas, 0 apagadas) e no máximo 1 vigente.
- **SC-005**: Em 100% das falhas provocadas (arquivo inválido, leitura indisponível, avaliação com erro, mudança durante a verificação, resumo inválido), a última análise concluída permanece igual, nenhum resultado parcial é registrado e a resposta indica a etapa a repetir.
- **SC-006**: Repetir a verificação sem mudança registra 0 análises e 0 resumos novos em 100% das tentativas verificadas; 10 pedidos simultâneos de verificação do mesmo pedido registram no máximo 1 análise nova.
- **SC-007**: 0 respostas e 0 registros de funcionamento dos cenários verificados contêm valor ou trecho de documento; 100% das respostas identificam a simulação.

## Assumptions

- **Conferência não é copiada entre revisões**: a leitura é presa à revisão (RC-10) e as decisões da paciente pertencem a uma leitura (RC-11). Depois de uma correção a paciente confere de novo a leitura da revisão atual; reaproveitar automaticamente confirmações de documentos não alterados fica fora desta história (melhoria possível, exigiria registrar a origem da decisão).
- **Verificação síncrona**: como na RC-13, leitura, regras e resumo são simulados e rápidos; a verificação espera a conclusão e responde com o resultado. Não há fila, nem processamento em segundo plano.
- **"Análise anterior" é a imediatamente anterior no histórico do pedido**, seja de revisão anterior (documentos corrigidos), seja da mesma revisão com outra conferência. É a referência de "Em relação à verificação anterior" das telas (feature 015, E3.5).
- **Estado do pedido**: alterar documentos continua sem mudar o estado (decisão da RC-09); quem move o pedido para `PROCESSANDO` e para o novo resultado é a análise disparada pela verificação.
- **Mensagens e textos** seguem `mobile/design/textos.md` (E3.5: "Resolvida", "Continua", "Nova"; "Os documentos mudaram. O resultado anterior não vale mais.") e a orientação da RC-15.
- **Telas**: esta história entrega o comportamento e o contrato da API; a apresentação Android e a validação com pessoas ficam com o épico 6 e não serão declaradas concluídas aqui.
- Todos os dados, documentos e protocolo são sintéticos e fictícios; leitura e resumo são simulados e identificados como tal.
