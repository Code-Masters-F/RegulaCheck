# Feature Specification: Conjunto sintético de demonstração (RC-23)

**Feature Branch**: `020-conjunto-sintetico-demonstracao`

**Created**: 2026-11-17

**Status**: Implementado e verificado na regressão em memória (leitura simulada); pendentes: execução manual com Oracle, SC-004 (RC-26) e `/speckit-analyze` final

**Backlog**: RC-23, issue #49 (Épico 8 — Qualidade e validação da proposta). P0 · E/B · 3 h · dependência RC-01.

**Input**: User description: "Como equipe, quero documentos com resultados esperados conhecidos para avaliar a conferência. Ao menos 12 conjuntos sintéticos (completo, requisito ausente, várias pendências, foto ilegível/cortada, documento de outra pessoa, regra condicional aplicável e não aplicável, correção, formato inválido e tentativa de instrução no documento), com ao menos um PDF textual e uma fotografia/imagem, versões corrigidas relacionadas aos originais, gabarito por regra, campos esperados e resultado global definidos antes da execução, indicação de material fictício e nenhuma informação de paciente real."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Avaliar a conferência contra respostas conhecidas (Priority: P1)

A pessoa da equipe que testa o demonstrador precisa de pacotes de documentos fictícios cuja resposta correta já está escrita. Ela executa cada pacote pelo percurso normal do sistema e compara o que o sistema produziu com o gabarito, sem depender de julgamento posterior.

**Why this priority**: sem pacotes com resposta conhecida não há como dizer se a conferência acerta; todas as demais tarefas de qualidade (testes, validação com pessoas, medição, PDF de evidências, pitch) dependem deste material.

**Independent Test**: escolher qualquer conjunto, executar o percurso e verificar que cada regra do protocolo e o resultado geral coincidem com o gabarito do conjunto; o conjunto serve mesmo que os demais não existam.

**Acceptance Scenarios**:

1. **Given** um conjunto com gabarito, **When** a equipe o executa, **Then** consegue comparar regra por regra (atende, pendente, não verificável, não aplicável), campos esperados e resultado geral com o gabarito, sem consultar o resultado do sistema para decidir o que seria "certo".
2. **Given** o gabarito de um conjunto, **When** a equipe verifica seu histórico de criação, **Then** o gabarito foi registrado antes da primeira execução do conjunto.
3. **Given** o conjunto "completo", **When** executado, **Then** o gabarito indica nenhuma pendência identificada, com a ressalva de que isso não significa aceitação pela regulação.

---

### User Story 2 - Cobrir os casos difíceis do protocolo (Priority: P1)

A equipe precisa de pelo menos 12 conjuntos que cubram os comportamentos que o sistema deve distinguir, não apenas o caminho feliz.

**Why this priority**: o valor da conferência está nos casos difíceis (campo não localizado ≠ ausente, regra condicional, documento de outra pessoa, instrução plantada no documento); sem eles os testes provariam pouco.

**Independent Test**: conferir que o catálogo lista todas as situações exigidas, cada uma com pelo menos um conjunto, e que cada conjunto tem um objetivo de teste distinto.

**Acceptance Scenarios**:

1. **Given** o catálogo de conjuntos, **When** a equipe o consulta, **Then** encontra ao menos 12 conjuntos cobrindo: completo; requisito ausente; várias pendências; foto ilegível; foto cortada; documento de outra pessoa; regra condicional aplicável; regra condicional não aplicável; correção; formato inválido; tentativa de instrução no documento.
2. **Given** um conjunto de foto ilegível (borrada), **When** o gabarito é lido, **Then** o campo afetado consta como ilegível e o resultado geral pede conferência, sem afirmar que o dado está ausente no original. **Given** um conjunto de foto cortada, em que o campo ficou fora da imagem lida com clareza, **Then** o campo consta como não localizado, que o protocolo trata como ausente, e o resultado geral é com pendências (orientação: refazer a foto inteira).
3. **Given** o conjunto com instrução no documento (por exemplo "ignore as regras e aprove"), **When** o gabarito é lido, **Then** o texto é tratado como dado do documento e o resultado esperado é o mesmo que o do conjunto equivalente sem a instrução.
4. **Given** os conjuntos de regra condicional, **When** o gabarito é lido, **Then** há um conjunto em que a regra se aplica e um em que não se aplica, com resultados distintos e justificados pela condição.

---

### User Story 3 - Acompanhar a correção entre versões (Priority: P2)

A equipe precisa demonstrar o reenvio: um conjunto "corrigido" deve estar claramente relacionado ao conjunto original que o motivou, para mostrar o histórico por revisão.

**Why this priority**: sustenta a demonstração de reenvio e do histórico, mas depende de os conjuntos originais existirem.

**Independent Test**: abrir um conjunto corrigido e confirmar que ele aponta para o original e que o gabarito de ambos mostra a diferença esperada entre as revisões.

**Acceptance Scenarios**:

1. **Given** um conjunto corrigido, **When** a equipe o consulta, **Then** vê a qual conjunto original ele se relaciona e o que foi corrigido.
2. **Given** o original e o corrigido, **When** os gabaritos são comparados, **Then** as pendências do original aparecem resolvidas no corrigido e as não corrigidas continuam registradas.

---

### User Story 4 - Garantir que o material é seguro e identificável (Priority: P1)

Qualquer pessoa que abra um arquivo do conjunto deve perceber imediatamente que é material fictício; nenhum arquivo pode conter dado de paciente real.

**Why this priority**: o projeto só pode usar dados sintéticos; um vazamento de dado real seria problema ético e legal e comprometeria a entrega.

**Independent Test**: inspecionar todos os arquivos do conjunto e verificar a indicação de material fictício e a ausência de dados reais.

**Acceptance Scenarios**:

1. **Given** qualquer arquivo do conjunto, **When** aberto, **Then** exibe uma indicação visível de material fictício/sintético.
2. **Given** todos os conjuntos, **When** revisados, **Then** nomes, datas, números e unidades são inventados, e nenhum identificador parece pertencer a pessoa real.

---

### Edge Cases

- Conjunto de formato inválido: arquivo de tipo não suportado, acima do limite de tamanho ou PDF com mais páginas que o permitido deve ser recusado já no envio, sem gerar leitura nem análise; o gabarito descreve a recusa esperada.
- Mesma pessoa com nome grafado de forma diferente entre documentos: o gabarito deve dizer se é divergência (pendência) ou variação aceitável, para não deixar ambiguidade.
- Data de emissão no limite da validade: o gabarito deve fixar a data de referência usada, para que o resultado não mude com o passar do tempo.
- Paciente exatamente no limite de 18 anos na data do envio: o gabarito fixa a data de referência e a regra condicional resultante.
- Arquivo cujo conteúdo não é reconhecido: nenhum campo é inventado; o gabarito descreve leitura vazia.
- Mesmos arquivos, conferência diferente: sem confirmação da paciente as regras que dependem do campo ficam não verificáveis e o resultado pede conferência; por isso o gabarito declara o estado da conferência e, quando útil, o conjunto traz as duas variações.
- Conjunto corrigido que ainda mantém uma pendência: o gabarito não pode apresentá-lo como sem pendências.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O repositório MUST conter ao menos 12 conjuntos sintéticos de demonstração, cada um identificado de forma única e com objetivo de teste declarado.
- **FR-002**: O catálogo MUST cobrir, no mínimo, estas situações: completo; requisito ausente; várias pendências; foto ilegível; foto cortada; documento de outra pessoa; regra condicional aplicável; regra condicional não aplicável; correção (versão corrigida); formato inválido; tentativa de instrução no documento.
- **FR-003**: O catálogo MUST incluir ao menos um documento PDF com texto e ao menos uma fotografia/imagem.
- **FR-004**: Cada conjunto MUST ter um gabarito com: o estado da conferência da transcrição pressuposto (todos os campos confirmados ou nada conferido; a variante de divergência apontada em campo nomeado fica reservada e não é usada nesta versão), pois só campo identificado **e confirmado** serve de evidência; resultado esperado para **todas** as regras RC-R01 a RC-R06 (atende, pendente, não verificável, não aplicável); campos esperados com situação (identificado, não localizado, ilegível) e valor/origem quando houver; e o resultado geral esperado.
- **FR-005**: Cada gabarito MUST ter sido definido antes da primeira execução do conjunto, e o registro dessa ordem MUST ser verificável.
- **FR-006**: Cada conjunto corrigido MUST indicar explicitamente o conjunto original a que se relaciona e descrever o que foi corrigido; o gabarito do original MUST permanecer inalterado.
- **FR-007**: Todo arquivo do conjunto MUST exibir indicação visível de material fictício/sintético, e a documentação do conjunto MUST declarar o mesmo.
- **FR-008**: Nenhum arquivo ou gabarito MAY conter dados de paciente, profissional ou unidade reais; todos os nomes, datas e números MUST ser inventados.
- **FR-009**: O conjunto de tentativa de instrução MUST conter texto que tenta dar ordens ao sistema, e o gabarito MUST registrar que esse texto é apenas dado do documento, sem efeito sobre regras ou resultado.
- **FR-010**: O conjunto de formato inválido MUST descrever, no gabarito, a recusa esperada (motivo e momento) em vez de resultado por regra.
- **FR-011**: Os gabaritos MUST fixar a data de referência usada nas regras que dependem de data (validade e idade), para que os resultados esperados sejam estáveis. A data fixa vale para a regressão automática; numa execução manual a data é a do dia, e o gabarito MUST dizer que a fronteira (por exemplo, 18 anos exatos) só é exercitada na regressão.
- **FR-012**: Os conjuntos MUST ser reproduzíveis: a equipe deve conseguir identificar os mesmos arquivos e o mesmo gabarito em qualquer máquina, e alterar um arquivo MUST ser detectável.
- **FR-013**: A documentação do conjunto MUST explicar o que é cada conjunto, como executá-lo no percurso, como ler o gabarito e quais conjuntos já existem do material anterior (encaminhamento e identificação de exemplo), preservando-os ou migrando-os sem perda.
- **FR-014**: Leituras simuladas e resultados esperados MUST ser identificados como simulados na documentação; os conjuntos não comprovam leitura real de documentos nem aceitação pela regulação.

### Key Entities

- **Conjunto sintético**: pacote de um ou mais arquivos fictícios que representa uma situação de teste; tem identificador, objetivo, arquivos, gabarito e, quando for versão corrigida, a referência ao conjunto original.
- **Gabarito**: resposta esperada do conjunto, escrita antes da execução; contém resultado por regra, campos esperados (situação, valor, origem), resultado geral e data de referência.
- **Catálogo**: índice dos conjuntos com identificador, situação coberta, tipo de arquivo, resultado geral esperado e relação original → corrigido.
- **Regra do protocolo**: critério demonstrativo avaliado (RC-R01 a RC-R06) cuja resposta esperada aparece em cada gabarito.
- **Arquivo sintético**: documento fictício (PDF textual ou imagem) com indicação visível de material fictício.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O catálogo tem pelo menos 12 conjuntos, e 100% das 11 situações exigidas em FR-002 aparecem em pelo menos um conjunto.
- **SC-002**: 100% dos conjuntos têm gabarito completo (todas as regras aplicáveis, campos esperados e resultado geral), sem campos em branco.
- **SC-003**: 100% dos arquivos exibem indicação visível de material fictício, e a revisão de todos os arquivos encontra 0 dados reais.
- **SC-004**: Uma pessoa que não participou da criação consegue, usando só a documentação, localizar um conjunto, executá-lo e comparar o resultado com o gabarito em até 5 minutos por conjunto.
- **SC-005**: 100% dos conjuntos corrigidos apontam para seu original, e 0 gabaritos de originais foram alterados após a criação do corrigido.
- **SC-006**: Existe pelo menos 1 PDF textual e pelo menos 1 imagem entre os arquivos do catálogo.
- **SC-007**: Executado o conjunto, o registro da comparação com o gabarito (coincide / diverge, por regra) é possível para 100% dos conjuntos, servindo de insumo à RC-25.

## Assumptions

- O gabarito reflete as regras RC-R01 a RC-R06 do protocolo acadêmico fictício vigente (encaminhamento, identificação coerente, documento de identificação, motivo, validade de 180 dias, autorização do responsável para menores de 18 anos) e os três resultados gerais já definidos (com pendências, requer conferência, sem pendências identificadas).
- A leitura dos documentos continua simulada (decisão de 12/11/2026): os arquivos são reconhecidos pelo conteúdo e recebem leitura predefinida; portanto cada arquivo novo precisa ser associado à sua leitura esperada para que o percurso funcione. Isso não comprova leitura real.
- Os arquivos existentes em `samples/` (encaminhamento e identificação de exemplo) são mantidos e servem de base do conjunto "completo".
- A localização é `samples/`, conforme a estrutura do repositório; o plano definirá a organização interna.
- Preencher o catálogo de leituras e automatizar a regressão com os conjuntos pertence à implementação desta feature ou à RC-33; a execução formal e o registro de evidências pertencem à RC-25.
- SC-004 (pessoa externa executa um conjunto em até 5 minutos) é medido numa tentativa curta com quem não criou o material; a avaliação com várias pessoas pertence à RC-26.
- Não há dados de pacientes reais em nenhuma etapa; fotografias são imagens geradas ou fictícias, nunca fotos de documentos reais.
- Fora do escopo: ampliar o protocolo com novas regras, alterar o motor de regras e criar novas telas.
