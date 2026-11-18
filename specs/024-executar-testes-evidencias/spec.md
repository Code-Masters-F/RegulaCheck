# Feature Specification: Executar testes e registrar evidências (RC-25)

**Feature Branch**: `024-executar-testes-evidencias`

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "RC-25 (issue #51): Executar testes e registrar evidências. Como equipe, quero comprovar que o fluxo integrado funciona antes de gravar a apresentação. P0, 5 h, dependências RC-06 a RC-24 conforme cenário. Executar os 16 cenários (T01..T16) do plano `docs/testes/plano-de-testes.md`, com no mínimo 5 execuções com evidência individual; registrar saída real, status, versão testada, data, print/log/relatório e falhas em `docs/evidencias/<ID-DO-CASO>/`; corrigir defeitos bloqueadores e repetir só os cenários afetados e o percurso principal (P01); ao menos um teste cobre upload real → extração (simulada) → regras → resumo simulado → correção → reanálise → submissão simulada. Casos com impedimento conhecido (RC-20 em T03, RC-22 em T16-06) só ficam Bloqueado após tentativa registrada."

## Contexto

O plano (RC-24, PR #77) descreve 94 casos (T01..T16 e o percurso integrado P01), todos "Não executado". Esta feature **executa** o plano e transforma cada resultado em registro verificável. Ela não cria funcionalidade nova: o que existe é testado como está, e qualquer correção só ocorre para defeito bloqueador.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Executar o percurso principal P01 com evidência (Priority: P1)

A equipe executa, de ponta a ponta, o percurso da paciente: entrar, criar pedido, enviar documentos reais, ler (extração simulada), conferir, analisar pelas regras, ver o resumo simulado, corrigir e reenviar, reanalisar e fazer a submissão simulada, e guarda a evidência do percurso inteiro.

**Why this priority**: é a prova de que "o fluxo integrado funciona" antes de gravar a apresentação, o objetivo declarado da issue.

**Independent Test**: com a API, o banco e um dispositivo disponíveis, seguir os passos do P01 e conferir que a pasta de evidência do P01 contém todos os itens mínimos e que o status final corresponde ao que de fato ocorreu.

**Acceptance Scenarios**:

1. **Given** ambiente de pé e conta de paciente fictícia, **When** a equipe executa o P01 do início ao fim, **Then** cada etapa tem resultado bruto registrado e o caso recebe status conforme TODOS os itens esperados (Aprovado só se todos conferirem).
2. **Given** que uma etapa do P01 diverge do esperado, **When** a equipe registra o resultado, **Then** o caso fica Reprovado com a divergência descrita e o esperado do plano não é alterado.
3. **Given** que o percurso passa pela leitura e pelo resumo, **When** o resultado é registrado, **Then** fica explícito que extração e resumo são simulados (decisão de 12/11/2026).

---

### User Story 2 - Executar os 16 cenários e atingir o mínimo acadêmico (Priority: P1)

A equipe executa os casos de cada cenário T01..T16, cada um com evidência individual em sua própria pasta, e preenche no plano resultado obtido, status, responsável e data.

**Why this priority**: o critério de aceite exige todos os 16 cenários e no mínimo 5 execuções com evidência individual; sem isso a issue não é atendida.

**Independent Test**: contar as pastas de evidência e os cenários com ao menos um caso executado; conferir que cada caso executado tem resultado, status, responsável e data preenchidos no plano.

**Acceptance Scenarios**:

1. **Given** um caso do plano, **When** ele é executado, **Then** existe `docs/evidencias/<ID-DO-CASO>/` com data e hora, versão/commit, passos (com o `id` do pedido), resultado bruto e status final com executor.
2. **Given** os 16 cenários, **When** a execução termina, **Then** cada cenário tem ao menos um caso executado ou, quando não foi possível, o motivo registrado.
3. **Given** um caso executado por dublê (falha da IA, falha do leitor), **When** ele é registrado, **Then** consta como dublê e não como integração real.

---

### User Story 3 - Registrar bloqueios e impedimentos conhecidos (Priority: P2)

Casos com impedimento conhecido (rotas de indicadores da RC-20 em T03; alertas na interface da RC-22 em T16-06) e casos que dependem de algo ausente (amostra, dispositivo, serviço) são tentados; só então recebem "Bloqueado", com o motivo e a tentativa registrados.

**Why this priority**: o plano proíbe marcar "Bloqueado" sem tentativa e proíbe fingir que uma funcionalidade ausente funciona; isso protege a honestidade da apresentação.

**Independent Test**: para cada caso "Bloqueado", existe evidência da tentativa (resposta HTTP, captura ou saída) e o motivo.

**Acceptance Scenarios**:

1. **Given** um caso com impedimento conhecido, **When** a equipe tenta executá-lo, **Then** a tentativa é registrada e o status passa a Bloqueado com o motivo, sem inventar resultado.
2. **Given** que a RC-20 ou a RC-22 passou a existir antes da execução, **When** o caso é executado, **Then** ele é tratado como caso normal (Aprovado ou Reprovado) e o impedimento é removido do registro.

---

### User Story 4 - Corrigir bloqueadores e repetir só o afetado (Priority: P2)

Quando um defeito impede o percurso principal ou um cenário essencial, a equipe corrige o defeito dentro do escopo e repete somente os casos afetados e o P01, mantendo o histórico da falha.

**Why this priority**: o critério pede correção de bloqueadores e reteste focado, não reexecução total.

**Independent Test**: para cada defeito corrigido, o registro mostra a falha original, a correção, os casos repetidos e o resultado do reteste, com o P01 repetido ao final.

**Acceptance Scenarios**:

1. **Given** um caso Reprovado por defeito bloqueador, **When** o defeito é corrigido, **Then** os casos afetados e o P01 são repetidos e as duas execuções ficam registradas (a falha não é apagada).
2. **Given** um defeito não bloqueador, **When** ele é encontrado, **Then** fica registrado como achado pendente, sem correção forçada.

---

### User Story 5 - Consolidar o resultado da rodada (Priority: P3)

A equipe consolida contagens por status, falhas encontradas, versão testada e lacunas, e atualiza o plano e os índices para que a rodada possa ser revisada por outro integrante.

**Why this priority**: torna o resultado auditável e apresentável, mas depende das execuções anteriores.

**Independent Test**: o resumo bate com o número de pastas de evidência e com os status gravados no plano.

**Acceptance Scenarios**:

1. **Given** a rodada concluída, **When** o resumo é gerado, **Then** as contagens por status somam o total de casos e coincidem com os status do plano.
2. **Given** pendências restantes (revisão por outro integrante, casos não executados), **When** o resumo é publicado, **Then** elas aparecem como pendentes, sem declarar aprovação do plano.

---

### Edge Cases

- Banco indisponível ou sem credenciais: casos "API real + Oracle" não podem ser executados; devem ficar Bloqueados com a tentativa registrada (a API responde 503 `DEPENDENCIA_INDISPONIVEL`).
- Emulador ou dispositivo indisponível: casos do app ficam Bloqueados, nunca Aprovados por analogia com evidências antigas (RC-18/RC-19 não substituem execução).
- Documento de amostra ausente (JPEG, PDF protegido): caso fica Bloqueado, ou a amostra sintética é gerada sem alterar as amostras existentes (o hash identifica a leitura simulada).
- Pedido já usado em execução anterior: nunca reutilizar; cada repetição cria pedido novo.
- Sessão curta (TTL) ou corte de rede deixados ativos por engano: restaurar ao padrão ao fim do caso.
- Caso parcialmente verificável: não aprova; registrar o que conferiu e o que falhou.
- Tentação de ajustar o esperado do plano para coincidir com o obtido: proibido; é um achado.
- Dado sensível acidental (token, senha, chave, `DB_*`) em captura ou log: não pode ir para o repositório.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A equipe MUST executar os casos do plano atribuídos a cada cenário T01..T16 e ao percurso P01, na versão testável identificada por commit.
- **FR-002**: Cada caso executado MUST ter uma pasta de evidência individual `docs/evidencias/<ID-DO-CASO>/` com: data e hora, versão/commit da API e do app, passos ou comandos (com o `id` do pedido criado), resultado bruto (saída, resposta HTTP, captura ou relatório), status final e responsável (integrante da equipe que supervisiona e autoriza a execução; quando o agente executa os comandos, o registro diz "executado com apoio do Claude Code").
- **FR-003**: O plano MUST ser atualizado, por caso executado, com resultado obtido, status, evidência (caminho), responsável e data; casos não executados MUST permanecer "Não executado".
- **FR-004**: O status de cada caso MUST seguir as regras do plano: Aprovado somente se todo o esperado conferir; Reprovado quando algo diverge; Bloqueado somente após tentativa com motivo registrado.
- **FR-005**: Pelo menos uma execução MUST cobrir, de ponta a ponta, upload real → extração (simulada) → regras → resumo simulado → correção → reanálise → submissão simulada, e MUST aparecer como evidência do P01.
- **FR-006**: O conjunto de execuções MUST atender ao mínimo acadêmico de 5 execuções com evidência individual, e MUST tentar cada um dos 16 cenários. Cada cenário termina com ao menos um caso executado (Aprovado ou Reprovado) ou, se nenhum puder ser executado, com todos os casos tentados e marcados Bloqueado com a tentativa e o motivo; cenário sem tentativa registrada não atende a este requisito.
- **FR-007**: Casos com impedimento conhecido (T03 pela RC-20; alertas na interface de T16 pela RC-22) MUST ser tentados e só então marcados Bloqueado, com o impedimento descrito.
- **FR-008**: Casos executados com dublê (falha da IA, falha do leitor) MUST ser identificados como "Dublê (não é integração real)".
- **FR-009**: Defeitos bloqueadores MUST ser corrigidos no escopo da correção, os casos afetados e o P01 MUST ser repetidos, e a falha original MUST permanecer registrada junto ao reteste.
- **FR-010**: Toda divergência entre esperado e obtido MUST virar achado com evidência; o esperado do plano MUST NOT ser alterado para coincidir.
- **FR-011**: Nenhuma evidência pode conter token, senha (inclusive a de demonstração), chave do simulador, variáveis `DB_*` ou documento real; apenas dados sintéticos.
- **FR-012**: Extração e resumo MUST ser descritos como simulados em todas as evidências e no resumo da rodada; nenhum registro pode afirmar leitura ou IA reais.
- **FR-013**: A execução MUST NOT executar operação destrutiva sobre o banco (limpeza, reaplicação da carga) sem autorização específica; cada repetição usa pedido novo.
- **FR-014**: A rodada MUST produzir um resumo com versão testada, contagem de casos por status, falhas encontradas, defeitos corrigidos, lacunas e pendências, e MUST manter a revisão por outro integrante como pendente.
- **FR-015**: Evidência de execução anterior de outra data MUST NOT ser usada para aprovar um caso; só pode ser citada como referência.

### Key Entities

- **Caso de teste**: item do plano (ID `Txx-yy` ou `P01`), com pré-condição, entrada, passos, esperado, impedimento conhecido, resultado obtido, status, evidência, responsável e data.
- **Execução**: tentativa de um caso numa versão e data; um caso pode ter mais de uma (falha e reteste).
- **Evidência**: pasta por caso com o conteúdo mínimo; é a prova do status.
- **Achado**: divergência ou defeito observado, com gravidade (bloqueador ou não), evidência e destino (corrigido, pendente).
- **Resumo da rodada**: consolidação de versão testada, contagens por status, achados, lacunas e pendências.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Todos os 16 cenários (T01..T16) têm pelo menos um caso executado com evidência ou todos os casos tentados e marcados Bloqueado com tentativa e motivo; zero cenário sem tentativa registrada.
- **SC-002**: Pelo menos 5 casos distintos têm evidência individual completa (os 5 itens mínimos).
- **SC-003**: O percurso P01 foi executado de ponta a ponta com a cadeia completa, com evidência, e repetido após qualquer correção de bloqueador.
- **SC-004**: 100% dos casos Aprovados têm evidência; 100% dos Bloqueados têm tentativa e motivo; 0 casos Aprovados sem execução.
- **SC-005**: As contagens por status no resumo somam o total de casos do plano e coincidem com os status gravados no plano.
- **SC-006**: 0 ocorrências de token, senha, chave ou `DB_*` nas evidências (verificado por busca no repositório).
- **SC-007**: Qualquer integrante consegue, lendo só a pasta de um caso, entender o que foi feito, em qual versão e com que resultado.

## Assumptions

- O prazo da issue (17/11) já passou; a execução ocorre em 18/11 e o impacto é informado no resumo, sem reduzir as exigências.
- A API, o banco da equipe, o emulador Android e o simulador de sensores estão disponíveis ou podem ser preparados; credenciais do banco são fornecidas pelo grupo e nunca gravadas no repositório.
- Os 94 casos do plano são o universo de execução. O mínimo da issue (16 cenários, 5 evidências) é piso; casos que não puderem ser executados nesta rodada ficam "Não executado" ou "Bloqueado" conforme tenham sido tentados.
- Extração e resumo são simulados (decisão de 12/11/2026); o percurso "real" refere-se ao envio de arquivos reais ao backend, não a OCR/IA reais.
- RC-20 (indicadores) e RC-22 (alertas na interface) ainda serão implementadas; se existirem até a execução, os casos correspondentes são executados normalmente.
- Correção de defeito é limitada a bloqueadores do percurso ou de cenário essencial; melhorias e achados menores viram pendência.
- Os arquivos de amostra e gabaritos da RC-23 são a fonte do esperado; não são regenerados.
- A revisão do plano e desta rodada por outro integrante é externa a esta feature e permanece pendente.
- Esta feature não faz commit, push, comentário ou fechamento de issue sem autorização; isso é decidido pelo responsável após a execução.
