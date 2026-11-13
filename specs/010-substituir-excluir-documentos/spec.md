# Feature Specification: Substituir documento e excluir rascunho (RC-09)

**Feature Branch**: `010-substituir-excluir-documentos`

**Created**: 2026-11-13

**Status**: Implementada e verificada sem banco (384 testes, 0 falhas); testes contra o Oracle executados (4 da RC-09, 0 falhas) (ver `tasks.md`, "Resultado da verificação")

**Backlog**: RC-09 — Code-Masters-F/ClickBus#16 · História · P0 · A/C (API e cliente) · 2 h · até 15/11/2026 · depende de RC-08 (envio e consulta de documentos), concluída

**Input**: User description: "RC-09 — Substituir documento e excluir rascunho. Como paciente, quero corrigir os arquivos enviados ou remover um rascunho que não vou utilizar."

## Contexto e limite desta história

Com os documentos anexados ao rascunho (RC-08), a paciente precisa poder **corrigir** o que enviou errado e **desistir** de um pedido que não vai usar. Hoje ela só consegue acrescentar documentos: um arquivo enviado por engano ocupa uma das 3 vagas do pedido e não sai mais, e um rascunho abandonado fica na lista para sempre.

Esta história entrega três ações, todas só da paciente dona e só sobre dados sintéticos:

1. **Remover** um documento do pedido (libera a vaga).
2. **Substituir** um documento por outro arquivo (a mesma vaga, com a conferência de tipo, tamanho e páginas da RC-06).
3. **Excluir** o rascunho inteiro, com **confirmação explícita**.

Regra transversal: **qualquer mudança no conjunto documental muda a versão do conjunto e invalida a conferência anterior**, de modo que a paciente precisa de uma nova análise para ter resultado válido. Nenhuma das três ações inicia análise: o sistema só guarda, troca e apaga arquivos, sem lê-los, interpretá-los ou avaliá-los.

**Fica fora desta história:** a análise em si, a extração e a transcrição, o resumo de IA, a submissão simulada (só se respeita o estado resultante dela), retenção ou limpeza automática por prazo, lixeira ou desfazer, antivírus, o desenho das telas (épico 007) e a exclusão de qualquer pedido que não seja rascunho.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Remover um documento do pedido (Priority: P1)

A paciente, em um pedido seu ainda não submetido, escolhe um documento que não quer mais e o remove. O documento deixa de aparecer na lista, o arquivo guardado é apagado e a vaga volta a ficar livre.

**Why this priority**: é o conserto mais simples e mais frequente (arquivo errado, ilegível ou de outra pessoa); sem ele o limite de 3 documentos trava a paciente.

**Independent Test**: com um rascunho com 3 documentos, remover um, conferir que a lista tem 2, que o arquivo não abre mais, que a versão do conjunto subiu e que um novo envio passa a ser aceito.

**Acceptance Scenarios**:

1. **Given** um rascunho próprio com documentos, **When** a paciente remove um deles, **Then** recebe a resposta de sucesso documentada, o documento sai da lista, a versão do conjunto documental aumenta em um e o registro da revisão indica que um documento foi removido.
2. **Given** a remoção concluída, **When** a paciente tenta abrir o arquivo removido, **Then** recebe "não encontrado" e o arquivo guardado não existe mais.
3. **Given** um pedido com 3 documentos e uma remoção concluída, **When** a paciente envia um novo documento, **Then** o envio é aceito.
4. **Given** um documento já removido, **When** a paciente repete o pedido de remoção, **Then** recebe "não encontrado", sem alterar versão nem contagem (repetir não remove um segundo documento nem erra de forma confusa).
5. **Given** um documento de outra paciente, de outro pedido ou inexistente, **When** a paciente tenta removê-lo, **Then** recebe a mesma resposta de "não encontrado" nos três casos e nada muda.
6. **Given** uma pessoa sem autenticação ou com perfil de administrador ou do simulador, **When** tenta remover, **Then** é recusada e nada muda.

---

### User Story 2 - Substituir um documento por outro arquivo (Priority: P1)

A paciente, em um pedido seu ainda não submetido, troca um documento por um arquivo corrigido. O novo arquivo passa pela mesma conferência do envio; se for aceito, entra no lugar do antigo e o antigo é apagado. Se for recusado, o documento antigo continua intacto.

**Why this priority**: é o critério central "corrigir os arquivos enviados"; evita que a paciente precise remover e reenviar em dois passos, correndo o risco de perder a vaga se o novo arquivo for recusado.

**Independent Test**: substituir um PDF por outro PDF válido e conferir lista, versão e arquivo; repetir com um arquivo inválido e conferir que o original permanece.

**Acceptance Scenarios**:

1. **Given** um rascunho próprio com um documento, **When** a paciente o substitui por um arquivo válido informando o tipo documental, **Then** o documento antigo deixa de estar disponível, o novo aparece na lista no mesmo pedido, a versão do conjunto aumenta em um (uma só vez, não duas) e o registro da revisão indica substituição.
2. **Given** a substituição concluída, **When** a paciente abre o documento antigo, **Then** recebe "não encontrado"; ao abrir o novo, recebe o conteúdo enviado por último.
3. **Given** um arquivo novo inválido (formato falso, acima de 5 MB, PDF com senha ou com mais de 5 páginas, extensão incoerente), **When** a paciente tenta substituir, **Then** a operação é recusada com o mesmo erro do envio e **o documento antigo, a versão do conjunto e os arquivos guardados ficam como estavam**.
4. **Given** um pedido com 3 documentos, **When** a paciente substitui um deles, **Then** a operação é aceita: substituir não conta como um quarto documento.
5. **Given** a mesma tentativa de substituição repetida por falha de conexão, **When** a paciente reenvia com a mesma identificação de tentativa, **Then** nenhum documento duplicado é criado e a resposta mostra o resultado já obtido.
6. **Given** a mesma identificação de tentativa usada com arquivo ou tipo diferente, **When** a paciente reenvia, **Then** a operação é recusada como conflito e nada muda.
7. **Given** um documento de outra paciente, de outro pedido, já removido ou inexistente, **When** a paciente tenta substituí-lo, **Then** recebe "não encontrado" (idêntico nos casos) e nada muda.

---

### User Story 3 - Excluir um rascunho com confirmação (Priority: P1)

A paciente desiste de um rascunho e o exclui. Como a ação não tem volta, o sistema só a executa se a paciente confirmar explicitamente. Ao excluir, o pedido, seus documentos e os arquivos guardados somem.

**Why this priority**: critério de aceite da história; sem isso a lista de pedidos acumula rascunhos inúteis e arquivos sintéticos ficam guardados sem uso.

**Independent Test**: criar um rascunho com documentos, tentar excluir sem confirmação (nada acontece), excluir com confirmação e conferir que pedido, documentos e arquivos sumiram.

**Acceptance Scenarios**:

1. **Given** um rascunho próprio, **When** a paciente o exclui **com** confirmação explícita, **Then** recebe a resposta de sucesso documentada, o pedido some da lista e não pode mais ser aberto, e seus documentos e arquivos guardados são apagados.
2. **Given** um rascunho próprio, **When** a paciente pede a exclusão **sem** confirmação (ausente, em branco ou com valor diferente do exigido), **Then** a operação é recusada com erro identificável e **nada é apagado**.
3. **Given** um pedido que não existe, já foi excluído ou é de outra paciente, **When** a paciente tenta excluí-lo, **Then** recebe a mesma resposta de "não encontrado" nos três casos e nada muda.
4. **Given** um pedido que não está em rascunho, **When** a paciente tenta excluí-lo, **Then** a operação é recusada com conflito de estado e nada é apagado (ver User Story 4).
5. **Given** uma pessoa sem autenticação ou com perfil de administrador ou do simulador, **When** tenta excluir, **Then** é recusada e nada muda.
6. **Given** a exclusão de um rascunho que a paciente criou, **When** ela cria um novo pedido em seguida, **Then** o novo pedido funciona normalmente (a exclusão não deixa resíduo que bloqueie a criação).

---

### User Story 4 - Pedido submetido em simulação fica só para consulta (Priority: P1)

Depois da submissão simulada, o pedido é um registro final. As três ações acima são recusadas com um conflito documentado, e a paciente continua podendo consultar o pedido e abrir os documentos.

**Why this priority**: é critério de aceite e protege o que foi "submetido" de ser alterado depois; a recusa precisa ser clara para a paciente.

**Independent Test**: com um pedido já submetido em simulação, tentar remover, substituir e excluir e conferir três conflitos, sem nenhuma alteração; consultar e abrir um documento com sucesso.

**Acceptance Scenarios**:

1. **Given** um pedido em estado de submissão simulada, **When** a paciente tenta remover um documento, substituí-lo ou excluir o pedido, **Then** cada tentativa é recusada com conflito de estado e mensagem que explica que o pedido foi submetido e só pode ser consultado.
2. **Given** a mesma situação, **When** a paciente consulta o pedido, lista os documentos e abre um documento, **Then** tudo continua funcionando.
3. **Given** as recusas, **When** a paciente consulta o pedido de novo, **Then** versão do conjunto, documentos e arquivos estão exatamente como antes.

---

### User Story 5 - Mudança nos documentos invalida a conferência anterior (Priority: P2)

Quando o pedido já passou por uma conferência (análise) e a paciente remove ou substitui um documento, o resultado anterior deixa de valer e o pedido passa a exigir nova análise, sem que o sistema a dispare sozinho.

**Why this priority**: é critério de aceite e regra de integridade, mas só tem efeito visível quando a análise existir (histórias seguintes); por isso fica abaixo das ações básicas, e esta história entrega a **regra e o contrato**, verificáveis hoje no que existe.

**Independent Test**: no que existe hoje (rascunho, sem análise): conferir que a resposta do pedido continua indicando que nenhuma análise foi realizada para a nova versão do conjunto. Para estados com análise: ver o comportamento exigido nos cenários abaixo, a ser exercitado quando a análise existir.

**Acceptance Scenarios**:

1. **Given** um rascunho (nenhuma análise ainda), **When** a paciente remove ou substitui um documento, **Then** a versão do conjunto documental aumenta e a resposta do pedido indica que **nenhuma análise foi realizada para essa versão**.
2. **Given** um pedido não submetido que já tem resultado de conferência (história futura), **When** a paciente remove ou substitui um documento, **Then** o resultado anterior deixa de ser vigente, o histórico dele é preservado, o pedido passa a exigir nova análise e **a análise não é iniciada automaticamente**.
3. **Given** uma análise em andamento durante a remoção ou substituição (história futura), **When** a mudança é concluída, **Then** o resultado da versão antiga não é publicado como vigente.

---

### Edge Cases

- **Duas ações ao mesmo tempo** no mesmo pedido (remover e substituir o mesmo documento, ou substituir e excluir o rascunho): exatamente uma vence; a outra recebe "não encontrado" ou conflito; versão sem salto nem repetição; nenhum arquivo órfão nem registro sem arquivo.
- **Remover e enviar ao mesmo tempo** perto do limite de 3: o total de documentos disponíveis nunca passa de 3.
- **Falha ao apagar o arquivo guardado** depois de a mudança ter sido registrada: a paciente já não vê o documento, a operação responde sucesso, e o arquivo remanescente é registrado só em log técnico (identificadores e contagem, sem nome interno); nunca fica um documento disponível sem arquivo.
- **Falha ao registrar a mudança** (banco indisponível): nada muda, o arquivo antigo permanece, o arquivo novo da substituição é descartado, e a resposta é de indisponibilidade sem detalhe técnico.
- **Substituição sem arquivo, com mais de um arquivo, com tipo documental inválido ou com identificação de tentativa malformada**: erro de contrato idêntico ao do envio, sem alterar nada.
- **Documento da carga sintética** (sem arquivo guardado de verdade): pode ser removido e substituído normalmente; a remoção não falha por não haver arquivo.
- **Exclusão do rascunho com documentos**: pedido, revisões, documentos e arquivos saem juntos; não sobra nada ligado ao pedido.
- **Identificador mal formado** na rota: mesma resposta de "não encontrado" de um inexistente.
- **Excluir rascunho que acabou de ser criado**, sem documentos: funciona do mesmo modo.

## Requirements *(mandatory)*

### Functional Requirements

**Remover documento**

- **FR-001**: A paciente DEVE poder remover um documento de um pedido seu que não esteja em submissão simulada; a remoção libera a vaga e o documento deixa de aparecer na lista e de abrir.
- **FR-002**: A remoção DEVE aumentar a versão do conjunto documental em um, com registro de revisão que indique remoção de documento, de forma atômica com a retirada do documento: ou tudo acontece, ou nada.
- **FR-003**: O arquivo guardado do documento removido DEVE ser apagado; não pode restar documento disponível sem arquivo nem arquivo guardado sem documento disponível (sem órfãos ativos).
- **FR-004**: Repetir a remoção de um documento já removido DEVE responder "não encontrado" sem alterar versão nem contagem.

**Substituir documento**

- **FR-005**: A paciente DEVE poder substituir um documento por outro arquivo, em pedido seu que não esteja em submissão simulada, informando o tipo documental do novo arquivo.
- **FR-006**: O arquivo novo DEVE passar pela mesma conferência do envio (formato real, tamanho de até 5 MB, PDF de até 5 páginas e sem senha, imagem dentro do limite, extensão coerente) e pelas mesmas regras de nome e tipo documental. Recusa NÃO pode alterar o documento antigo, a versão do conjunto nem deixar arquivo guardado.
- **FR-007**: A substituição DEVE ser atômica: o documento antigo sai e o novo entra na mesma operação; a versão do conjunto aumenta em **um** (não dois) com registro de revisão que indique substituição; a substituição não conta como documento adicional para o limite de 3.
- **FR-008**: Substituir DEVE aceitar a mesma identificação opcional de tentativa do envio; repetir a mesma tentativa não duplica, e a mesma identificação com conteúdo ou tipo diferente é conflito.
- **FR-009**: O arquivo do documento antigo DEVE ser apagado depois que a substituição for registrada; se a substituição falhar, o arquivo novo é descartado e o antigo permanece.

**Excluir rascunho**

- **FR-010**: A paciente DEVE poder excluir um pedido seu em estado de rascunho, e a exclusão só DEVE ocorrer com confirmação explícita enviada na própria requisição; sem confirmação, ou com confirmação diferente da exigida, a operação é recusada e nada é apagado.
- **FR-011**: A exclusão DEVE apagar o pedido, seu histórico de revisões, seus documentos e todos os arquivos guardados, de forma que nada permaneça ligado ao pedido; a paciente não consegue mais abri-lo, listá-lo nem abrir seus documentos.
- **FR-012**: Pedido que não esteja em rascunho DEVE ser recusado na exclusão com conflito de estado (ver FR-016).

**Invalidação da conferência**

- **FR-013**: Toda remoção ou substituição DEVE tornar a conferência anterior não vigente (preservando o histórico dela), fazer o pedido exigir nova análise e NÃO iniciar a análise automaticamente.
- **FR-014**: A resposta do pedido DEVE continuar distinguindo documentos enviados de análise concluída: para a nova versão do conjunto, a análise consta como não realizada.
- **FR-015**: Resultado de análise que começou em uma versão anterior NÃO DEVE ser publicado como vigente depois de remoção ou substituição.

**Pedido submetido e autorização**

- **FR-016**: Pedido em submissão simulada DEVE ficar somente para consulta: remover, substituir e excluir por estas rotas respondem conflito de estado documentado, sem alterar nada; consultar o pedido e abrir documentos continuam permitidos.
- **FR-017**: As três ações DEVEM ser exclusivas do perfil da paciente, e a paciente é sempre a da sessão; pedido ou documento inexistente, alheio, de outro pedido, já removido ou com identificador mal formado respondem "não encontrado" de forma idêntica.
- **FR-018**: Entradas com dono, identificador de paciente, estado, revisão ou caminho NÃO DEVEM influenciar a operação.

**Confiabilidade, contrato e rastreabilidade**

- **FR-019**: Cada ação DEVE ser segura sob concorrência: duas ações simultâneas no mesmo pedido não geram versão repetida, salto de versão, mais de 3 documentos disponíveis, nem arquivo órfão.
- **FR-020**: Falha de banco ou de armazenamento DEVE responder indisponibilidade sem detalhe técnico, sem deixar mudança pela metade.
- **FR-021**: Os contratos das três ações (códigos de resposta, corpo, erros, confirmação exigida) DEVEM ser documentados no contrato da API, no OpenAPI, na coleção de testes e nos documentos de estados, de dados e de autorização, e os testes DEVEM cobrir recusa por autorização, estado, entrada inválida, concorrência e falha de armazenamento.
- **FR-022**: Nenhum log nem resposta DEVE expor o conteúdo dos documentos, o nome interno de armazenamento, a confirmação sensível ou detalhe técnico.

### Key Entities

- **Documento**: arquivo sintético vinculado a um pedido, com tipo documental e estado de disponibilidade. Passa a poder deixar de estar disponível (removido) ou ser trocado por outro (substituído), conservando o registro da revisão em que isso ocorreu.
- **Pedido (solicitação)**: ganha a possibilidade de ser excluído enquanto rascunho; sua versão do conjunto documental sobe a cada mudança; em submissão simulada fica imutável por estas rotas.
- **Revisão**: uma linha por mudança do conjunto documental, com motivo (envio, substituição ou remoção); a numeração não tem salto nem repetição.
- **Confirmação de exclusão**: declaração explícita, na requisição, de que a paciente quer apagar o rascunho de forma definitiva.
- **Política de arquivos**: o arquivo guardado de um documento removido, substituído ou de um rascunho excluído é apagado; o registro histórico da remoção e substituição permanece enquanto o pedido existir, e some junto com o rascunho excluído.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A paciente remove um documento errado e envia o correto em menos de 1 minuto, sem precisar de ajuda, em 100% das tentativas válidas de uma demonstração sintética.
- **SC-002**: Em 100% das substituições recusadas por arquivo inválido, o documento antigo, a versão do conjunto e os arquivos guardados permanecem exatamente como antes (verificado por conferência antes e depois).
- **SC-003**: Em 100% das tentativas de excluir rascunho sem confirmação explícita, nada é apagado.
- **SC-004**: Depois de uma remoção, substituição ou exclusão, 0 arquivos órfãos ativos e 0 documentos disponíveis sem arquivo (verificado ao final de cada cenário de teste, inclusive com ações simultâneas).
- **SC-005**: Sob 20 ações simultâneas sobre o mesmo pedido, a numeração de revisões não tem salto nem repetição e nunca há mais de 3 documentos disponíveis.
- **SC-006**: Em 100% das tentativas sobre pedido submetido em simulação, a resposta é o conflito documentado e nada é alterado; a consulta continua funcionando.
- **SC-007**: Em 100% dos casos de documento ou pedido de outra paciente, inexistente, já removido ou com identificador malformado, a resposta é idêntica e não revela se o recurso existe.
- **SC-008**: Nenhuma resposta nem log das três ações contém conteúdo de documento ou nome interno de armazenamento, verificado por teste automatizado.

## Assumptions

- Os documentos e pedidos são sintéticos e o protocolo é acadêmico fictício; o sistema só guarda, troca e apaga arquivos, sem lê-los nem avaliá-los.
- Nesta fase, o único estado de pedido alcançável com documentos é o rascunho; os estados de análise e a submissão simulada ainda não são produzidos por nenhuma rota. As regras para esses estados (FR-013 a FR-016) são especificadas agora e **testadas sobre pedidos montados diretamente em cada estado**, a serem exercitadas de ponta a ponta quando as histórias de análise e submissão existirem.
- **Política de arquivos (decisão desta história):** o arquivo de documento removido ou substituído é **apagado**; o registro do documento é **mantido** como histórico enquanto o pedido existir (não conta como disponível, não abre, não aparece na lista), e a exclusão do rascunho apaga **tudo**, registros e arquivos. Não há lixeira nem desfazer.
- Remover e substituir vale para pedidos não submetidos; em estados de análise, a mudança é aceita e invalida a conferência anterior (sem iniciar nova análise); só a submissão simulada bloqueia.
- Excluir só vale para rascunho; pedidos que já passaram por análise não podem ser excluídos por esta rota (conflito de estado), para não perder o histórico de análise.
- Substituição é uma operação única (não duas chamadas) para não consumir nem perder a vaga se o arquivo novo for recusado; ela reutiliza os limites e a conferência da RC-08 e a mesma identificação opcional de tentativa.
- A confirmação de exclusão é um valor explícito enviado na requisição (a forma exata, como cabeçalho ou campo, é decidida no plano); ausência ou valor diferente é recusado.
- Perfil administrador e simulador não têm acesso a estas rotas, como nas anteriores.
- Falha ao apagar um arquivo depois de registrada a mudança não desfaz a mudança: o arquivo remanescente é registrado só em log técnico, sem nome interno, e nunca fica visível ao usuário.
- Não há retenção por prazo nem limpeza automática agendada nesta história.
- O desenho das telas fica no épico 007; esta história entrega apenas a API.
