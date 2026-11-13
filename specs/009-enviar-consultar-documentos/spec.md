# Feature Specification: Enviar e consultar documentos (RC-08)

**Feature Branch**: `009-enviar-consultar-documentos`

**Created**: 2026-11-13

**Status**: Especificação pronta para planejamento (nada implementado)

**Backlog**: RC-08 — Code-Masters-F/ClickBus#15 · História · P0 · A/C (API e cliente) · 4 h · até 14/11/2026 · depende de RC-06 (proteção de arquivos) e RC-07 (rascunho), ambas concluídas

**Input**: User description: "RC-08 — Enviar e consultar documentos. Como paciente, quero anexar o encaminhamento e documentos para que sejam conferidos."

## Contexto e limite desta história

Com o rascunho criado e o serviço escolhido (RC-07), a paciente precisa **anexar os documentos do encaminhamento** ao pedido. O protocolo acadêmico fictício pede, no máximo, o encaminhamento, um documento de identificação e, para menores de 18 anos, a autorização do responsável. Os arquivos são sempre sintéticos, e o sistema só guarda e entrega de volta o que a paciente enviou: **não lê, não interpreta e não avalia** o conteúdo.

Esta história entrega: enviar um arquivo ao próprio rascunho informando o tipo documental, ver a lista dos documentos do pedido com o estado de cada envio, abrir o arquivo de volta, repetir um envio interrompido sem criar duplicata e acompanhar a versão do conjunto documental. A conferência de tipo, tamanho e páginas dos arquivos já existe (RC-06); aqui ela passa a ser usada de ponta a ponta.

**Fica fora desta história:** substituir e remover documento (história própria do mesmo épico), extração e transcrição, análise de critérios, resumo de IA, submissão simulada, antivírus, OCR real, retenção/exclusão automática de arquivos e o desenho das telas (épico 007). Também não há processamento nenhum depois do envio: **enviar não inicia análise**, e o pedido continua em rascunho. Nada aqui diagnostica, prioriza ou coloca a paciente em fila real.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Anexar um documento ao rascunho (Priority: P1)

A paciente, em um rascunho seu, escolhe um arquivo (PDF, JPEG ou PNG), informa que tipo de documento é (por exemplo, encaminhamento) e o envia. Recebe a confirmação com nome do arquivo, tipo documental, tamanho, páginas e a revisão do pedido em que o arquivo entrou.

**Why this priority**: sem documento anexado não há o que conferir; é o núcleo da história e a base da extração e da análise que vêm depois.

**Independent Test**: autenticar como paciente sintética com um rascunho, enviar um PDF sintético válido com o tipo "encaminhamento" e conferir a confirmação, o vínculo com o pedido e o aumento da revisão.

**Acceptance Scenarios**:

1. **Given** uma paciente com rascunho próprio e um PDF sintético válido (até 5 páginas e 5 MB), **When** envia o arquivo informando o tipo documental, **Then** recebe confirmação de criação com o nome, o tipo documental, o formato, o tamanho, o número de páginas, o estado do envio e a revisão do pedido, e o documento fica ligado ao pedido dela.
2. **Given** a mesma situação com uma imagem JPEG ou PNG válida, **When** envia, **Then** o envio é aceito nas mesmas condições.
3. **Given** um envio concluído, **When** a paciente consulta o pedido, **Then** a revisão do pedido aumentou em um e o registro da revisão indica que um documento foi enviado.
4. **Given** um envio que traga um dono, identificador de paciente, estado ou revisão no pedido, **When** é processado, **Then** esses campos são ignorados ou recusados: o documento sempre pertence ao pedido do caminho da requisição e à paciente da sessão.
5. **Given** um tipo documental que não está entre os aceitos, ausente ou em branco, **When** a paciente envia, **Then** o envio é recusado com erro identificável no campo do tipo e nada é gravado.
6. **Given** um pedido que já tem 3 documentos disponíveis, **When** a paciente tenta enviar o quarto, **Then** o envio é recusado com a explicação do limite e nada é gravado.
7. **Given** um pedido que saiu do estado de rascunho, **When** a paciente tenta anexar um documento, **Then** a operação é recusada com conflito de estado e nenhum dado muda.

---

### User Story 2 - Consultar meus documentos e abrir o arquivo (Priority: P1)

A paciente vê a lista dos documentos do pedido, cada um com nome do arquivo, tipo documental e estado do envio, e abre um deles para conferir o que enviou. Só ela consegue abrir os seus arquivos.

**Why this priority**: é critério de aceite da história e é o que permite à paciente conferir o que de fato foi guardado antes de seguir.

**Independent Test**: com um documento enviado, listar os documentos do pedido, abrir o arquivo e comparar o conteúdo recebido com o enviado; repetir com a conta de outra paciente e conferir a recusa.

**Acceptance Scenarios**:

1. **Given** um pedido com documentos enviados, **When** a paciente lista os documentos, **Then** vê, para cada um, nome do arquivo, tipo documental, formato, tamanho, páginas, estado do envio, revisão de entrada e data do envio, na ordem em que foram enviados.
2. **Given** um pedido sem documentos, **When** a paciente lista, **Then** recebe uma lista vazia, sem erro.
3. **Given** um documento próprio, **When** a paciente o abre, **Then** recebe o mesmo conteúdo que enviou, como arquivo para baixar, sem que o nome interno de armazenamento apareça em nenhuma resposta.
4. **Given** um documento de outra paciente, **When** a paciente tenta abri-lo ou vê-lo na lista pelo identificador, **Then** recebe a mesma resposta de "não encontrado" de um documento inexistente.
5. **Given** um documento próprio informado com o identificador de **outro pedido** dela, **When** a paciente o abre pelo pedido errado, **Then** recebe "não encontrado": o documento só abre pelo pedido ao qual pertence.
6. **Given** uma pessoa sem autenticação, com perfil de administrador ou com o perfil do simulador, **When** tenta listar ou abrir documentos, **Then** é recusada (sem autenticação ou sem permissão) e nada é entregue.
7. **Given** o armazenamento de arquivos indisponível, **When** a paciente tenta abrir um arquivo, **Then** recebe uma mensagem de indisponibilidade sem detalhe técnico, e a lista de documentos continua disponível.

---

### User Story 3 - Recusar arquivo inválido e repetir envio interrompido (Priority: P1)

Se o arquivo não passa na conferência, a paciente recebe o motivo e nada fica registrado como documento. Se o envio foi interrompido (a conexão caiu, a resposta se perdeu ou ela tocou duas vezes), ela pode repetir o mesmo envio e o resultado é um único documento, nunca duas cópias sem aviso.

**Why this priority**: são dois critérios de aceite explícitos e protegem a integridade do conjunto documental que será analisado.

**Independent Test**: enviar arquivos inválidos (tipo falso, grande demais, PDF protegido ou com muitas páginas) e conferir que a lista continua vazia; enviar o mesmo arquivo duas vezes com a mesma identificação de tentativa e conferir um único documento.

**Acceptance Scenarios**:

1. **Given** um arquivo cujo conteúdo não é PDF, JPEG ou PNG, ou cuja extensão não corresponde ao conteúdo, **When** a paciente o envia, **Then** o envio é recusado com o motivo identificável e nada é gravado em lugar nenhum.
2. **Given** um arquivo acima de 5 MB, um PDF com mais de 5 páginas, um PDF protegido por senha ou um arquivo vazio, truncado ou corrompido, **When** é enviado, **Then** é recusado com o motivo correspondente, a revisão do pedido não muda e o documento não aparece na lista.
3. **Given** uma recusa por arquivo inválido, **When** a paciente consulta o pedido e a lista, **Then** o pedido está exatamente como antes do envio.
4. **Given** um envio concluído com uma identificação de tentativa, **When** a paciente repete o mesmo envio com a mesma identificação e o mesmo arquivo, **Then** recebe o documento já existente (mesmo identificador), sem novo documento e sem nova revisão.
5. **Given** uma identificação de tentativa já usada, **When** a paciente a reutiliza com um arquivo diferente ou outro tipo documental, **Then** o envio é recusado com conflito explícito e nada é criado ou alterado.
6. **Given** um envio interrompido antes da confirmação, **When** a paciente o repete, **Then** o resultado final é um único documento disponível, e nenhum arquivo fica guardado sem registro por causa de falha tratada pela aplicação (ver o limite conhecido do FR-013).
7. **Given** dois envios simultâneos com a mesma identificação de tentativa, **When** ambos são processados, **Then** o resultado final é um único documento e uma única revisão adicionada.
8. **Given** um envio sem identificação de tentativa, **When** a paciente o repete, **Then** o sistema o trata como um envio novo, e o limite de 3 documentos continua protegendo contra excesso.

---

### User Story 4 - Acompanhar a versão do conjunto documental e o estado do processamento (Priority: P2)

A paciente (e o aplicativo) consegue distinguir três coisas diferentes: o **upload concluído** de cada arquivo, a **versão do conjunto de documentos** do pedido e o fato de que **nenhuma análise foi feita**. Receber o arquivo não significa que ele foi conferido nem aprovado.

**Why this priority**: é critério de aceite da história e evita que o aplicativo mostre "enviado" como se fosse "analisado", o que enganaria a paciente sobre o andamento do pedido.

**Independent Test**: enviar dois documentos em sequência e conferir que a versão do conjunto sobe a cada envio concluído, que a lista de documentos informa o estado de cada envio e que o pedido informa que não há análise concluída.

**Acceptance Scenarios**:

1. **Given** um pedido novo (revisão 1, sem documentos), **When** a paciente envia o primeiro e depois o segundo documento, **Then** a versão do conjunto documental passa por 2 e 3, cada documento guarda a revisão em que entrou, e cada revisão fica registrada com o motivo de documento enviado.
2. **Given** um pedido com documentos enviados, **When** a paciente o consulta, **Then** a resposta traz a versão atual do conjunto documental e a quantidade de documentos disponíveis, e informa que a análise ainda não foi realizada para essa versão.
3. **Given** documentos enviados e nenhuma análise solicitada, **When** a paciente consulta o pedido, **Then** o estado do pedido continua sendo rascunho, nunca "processando" nem "concluído", e a resposta nunca sugere que os documentos foram avaliados.
4. **Given** um envio recusado ou repetido sem efeito (mesma identificação de tentativa), **When** a paciente consulta a versão do conjunto, **Then** ela não mudou.
5. **Given** os dois documentos do mesmo pedido enviados ao mesmo tempo, **When** ambos concluem, **Then** a versão final reflete exatamente os envios aplicados, sem salto nem repetição de número.

---

### Edge Cases

- Nome de arquivo com caracteres especiais, acentos, caminho embutido (`..\`, `/etc/`), muito longo ou vazio: o nome mostrado é sempre uma versão segura e limitada, e o nome real de armazenamento nunca é o dado pelo usuário.
- Extensão que não bate com o conteúdo (um executável renomeado para `.pdf`): recusado pela conferência de conteúdo.
- Envio sem nenhum arquivo, com mais de um arquivo na mesma requisição ou com parte do corpo ausente: recusado com erro identificável no campo do arquivo.
- Corpo de envio ilegível ou interrompido no meio: nada é gravado, e a paciente pode repetir.
- Falha ao gravar o registro depois de o arquivo ter sido guardado: o arquivo guardado é removido, a paciente recebe erro genérico de indisponibilidade e nada fica disponível.
- Falha ao gravar o arquivo na área privada: o envio falha sem deixar registro, e a paciente pode repetir.
- Armazenamento de arquivos não configurado: o envio responde indisponibilidade, e a listagem e o restante do pedido seguem funcionando.
- Quatro envios quase simultâneos num pedido que já tem 2 documentos: no máximo 3 ficam disponíveis, e os demais são recusados pelo limite.
- Documento que contenha texto parecido com instrução (por exemplo, "ignore as regras"): é só dado, nunca altera critérios nem comportamento; nenhum conteúdo do arquivo é lido ou interpretado nesta história.
- Pedido alheio, pedido inexistente e identificador de documento mal formado: sempre "não encontrado" para a paciente, sem revelar existência.
- Identificação de tentativa muito longa, vazia ou com caracteres inválidos: recusada com erro identificável.
- Duas contas diferentes usando a mesma identificação de tentativa: são independentes (a identificação vale por pedido e por paciente).

## Requirements *(mandatory)*

### Functional Requirements

**Enviar**
- **FR-001**: A paciente MUST poder enviar um arquivo PDF, JPEG ou PNG a um **rascunho seu**, informando o **tipo documental**. O formato é sempre determinado pelo conteúdo, nunca só pela extensão ou pelo tipo declarado pelo cliente.
- **FR-002**: O tipo documental MUST ser um destes valores fechados, alinhados ao protocolo acadêmico: encaminhamento, documento de identificação, autorização do responsável, outro. Valor ausente, em branco ou fora do conjunto MUST ser recusado com erro identificável no campo do tipo.
- **FR-003**: O envio MUST respeitar os limites já definidos: 5 MB por arquivo, PDF até 5 páginas, imagem JPEG ou PNG dentro do limite de pixels, nome de exibição de até 100 caracteres e **no máximo 3 documentos disponíveis por pedido**. Uma requisição traz **um** arquivo.
- **FR-004**: O documento MUST ser vinculado ao pedido do caminho da requisição e, pela posse do pedido, à paciente da sessão. O sistema MUST NOT aceitar dono, identificador de paciente, estado, revisão ou nome interno de armazenamento vindos do cliente.
- **FR-005**: O envio só MUST ser aceito com o pedido em **rascunho**. Em qualquer outro estado MUST ser recusado com conflito de estado, sem alterar nada.
- **FR-006**: O envio MUST incrementar em um a **revisão do pedido** e registrar essa revisão com o motivo de documento enviado, de forma atômica com o registro do documento: ou o documento e a revisão existem juntos, ou nenhum dos dois.
- **FR-007**: O sistema MUST NOT iniciar extração, análise, resumo ou qualquer processamento do conteúdo ao receber um arquivo, e MUST NOT alterar o estado do pedido (continua em rascunho).

**Arquivo inválido**
- **FR-008**: Um arquivo que não passe na conferência (tipo, extensão, tamanho, páginas, proteção por senha, corrupção, imagem grande demais) MUST ser recusado com o código e a mensagem do contrato de erros já existente, sem criar documento, sem criar revisão e sem deixar arquivo guardado. A recusa MUST ocorrer antes de qualquer persistência.
- **FR-009**: O sistema MUST NOT tratar um arquivo recusado, ou cujo registro falhou, como documento disponível em nenhuma listagem ou consulta.

**Repetir envio**
- **FR-010**: O envio MUST aceitar uma **identificação de tentativa** opcional, enviada pelo cliente e válida por pedido e por paciente. Com a mesma identificação e o mesmo conteúdo e tipo documental, repetir o envio MUST devolver o documento já existente (mesmo identificador, mesmos dados), sem criar documento, sem nova revisão e sem gravar outro arquivo.
- **FR-011**: Reutilizar uma identificação de tentativa com **conteúdo ou tipo documental diferentes** MUST ser recusado com conflito explícito, sem criar nem alterar nada.
- **FR-012**: Dois envios simultâneos com a mesma identificação de tentativa MUST resultar em um único documento e uma única revisão adicionada. O limite de 3 documentos MUST valer também sob envios simultâneos.
- **FR-013**: Um envio que falhe ou seja interrompido por erro que a aplicação consegue tratar MUST NOT deixar arquivo guardado sem registro correspondente nem registro sem arquivo. Se a gravação do registro falhar depois de o arquivo ser guardado, o arquivo MUST ser removido. **Limite conhecido:** se o próprio processo da aplicação for encerrado entre guardar o arquivo e confirmar o registro, pode restar um arquivo sem registro na área privada (nunca um registro sem arquivo); ele não fica acessível por nenhuma rota, e a limpeza de órfãos está fora do escopo.
- **FR-014**: Sem identificação de tentativa, o sistema MUST tratar cada envio como novo e documentar que a proteção contra duplicata silenciosa depende dela (e do limite de 3 documentos).

**Consultar**
- **FR-015**: A paciente MUST poder listar os documentos disponíveis de um pedido seu, na ordem de envio, e ver para cada um: identificador, nome do arquivo, tipo documental, formato, tamanho, páginas, **estado do envio**, revisão de entrada e instante do envio. A lista vazia MUST ser resposta de sucesso.
- **FR-016**: A paciente MUST poder abrir o arquivo de um documento seu, recebendo exatamente o conteúdo guardado, como arquivo para baixar, com as proteções de entrega já definidas na RC-06. O nome interno de armazenamento MUST NOT aparecer em nenhuma resposta ou log.
- **FR-017**: Consulta e download MUST filtrar pela dona na mesma operação de busca e exigir que o documento pertença ao pedido informado. Documento ou pedido alheio, inexistente, ou de outro pedido da mesma paciente MUST produzir a mesma resposta de "não encontrado".
- **FR-018**: A consulta ao pedido MUST informar a **versão do conjunto documental** (a revisão atual), a quantidade de documentos disponíveis e que **nenhuma análise foi realizada** para a versão atual, deixando clara a distinção entre upload concluído e análise concluída.
- **FR-019**: O estado do envio de um documento MUST ser um valor fechado e documentado; neste momento, o único estado de um documento disponível é "enviado". O contrato MUST permitir que histórias seguintes acrescentem estados de processamento sem alterar o significado do atual.

**Perfis, validação e erros**
- **FR-020**: Todas as operações MUST exigir autenticação e perfil de paciente; administrador e simulador MUST ser recusados. A ordem de verificação segue o padrão existente: sem token, perfil indevido, pedido/documento não encontrado ou alheio, contrato inválido, regra de negócio e indisponibilidade.
- **FR-021**: Os erros MUST usar o formato padronizado do projeto, com código, mensagem compreensível e o campo envolvido quando houver (tipo documental, arquivo, identificação de tentativa), sem pilha, sem SQL, sem caminho de armazenamento e sem conteúdo do arquivo.
- **FR-022**: Logs MUST registrar apenas identificadores técnicos, etapa e códigos seguros; MUST NOT registrar nome original do arquivo, conteúdo, nome interno ou token.
- **FR-023**: O conteúdo dos arquivos MUST ser tratado como dado não confiável: nada nele altera regras, permissões ou comportamento, e nenhum texto dele é interpretado nesta história.
- **FR-024**: O sistema MUST NOT diagnosticar, definir urgência, autorizar atendimento ou sugerir, na resposta, que o documento foi aprovado, conferido ou analisado.
- **FR-025**: A API, os contratos, a documentação de ambiente e o modelo de dados MUST ser atualizados com os novos endpoints, o tipo documental, a identificação de tentativa, os estados do envio e a necessidade da configuração de armazenamento.

### Key Entities

- **Documento**: arquivo anexado a um pedido. Atributos: identificador, pedido, nome de exibição (seguro e limitado), formato (PDF/JPEG/PNG, pelo conteúdo), tipo documental (encaminhamento, identificação, autorização do responsável, outro), tamanho, número de páginas, revisão de entrada, instante do envio, estado do envio e identificação de tentativa (opcional). O local de armazenamento é interno e nunca exposto. Pertence a um único pedido.
- **Pedido (RC-07)**: ganha a versão do conjunto documental (sua revisão atual), a contagem de documentos disponíveis e a indicação de que nenhuma análise foi realizada. Continua em rascunho.
- **Revisão (RC-07)**: cada envio concluído cria uma nova, com o motivo de documento enviado.
- **Tentativa de envio**: identificação opcional dada pelo cliente que permite repetir um envio com segurança; vale por pedido e por paciente e fica ligada ao documento criado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em teste de demonstração com a paciente sintética, enviar um documento válido a um rascunho e abri-lo de volta leva menos de 1 minuto.
- **SC-002**: 100% dos arquivos inválidos de uma bateria sintética (tipo falso, extensão trocada, acima de 5 MB, PDF com mais de 5 páginas, PDF protegido, vazio, corrompido, imagem grande demais) são recusados, e a lista de documentos e a revisão do pedido permanecem idênticas às de antes do envio.
- **SC-003**: Repetir 10 vezes o mesmo envio com a mesma identificação de tentativa resulta sempre em exatamente 1 documento e 1 revisão adicionada.
- **SC-004**: Em 20 envios simultâneos ao mesmo pedido, nunca ficam mais de 3 documentos disponíveis, e a numeração de revisões não tem salto nem repetição.
- **SC-005**: Em 100% das tentativas de listar ou abrir documento de outra paciente, de outro pedido, inexistente, ou com perfil indevido, o resultado é recusa sem revelar se o recurso existe e sem entregar conteúdo.
- **SC-006**: Em 100% das respostas de envio e consulta, o pedido aparece como não analisado; nenhuma resposta indica documento conferido ou aprovado.
- **SC-007**: Nenhum nome interno de armazenamento, caminho, nome original de arquivo, token ou conteúdo de arquivo aparece em logs ou respostas de erro, comprovado pelos testes de higiene de log.

## Assumptions

- A paciente envia um arquivo por requisição; para anexar vários, repete a operação (no máximo 3 documentos disponíveis por pedido, limite herdado da RC-06).
- Os tipos documentais seguem o protocolo acadêmico (`docs/protocolo-academico.md`): encaminhamento, documento de identificação, autorização do responsável e "outro". A obrigatoriedade de cada tipo é avaliada só na análise (histórias seguintes), nunca no envio: a paciente pode anexar em qualquer ordem e quantidade, dentro do limite.
- A identificação de tentativa é opcional e gerada pelo cliente; sem ela, não há deduplicação do envio (decisão herdada do padrão da RC-07: o servidor não deduplica toques duplos sem chave).
- Substituir e remover documento ficam para a história seguinte do épico; por isso, nesta história um documento enviado não é alterado nem apagado, e o limite de 3 só libera espaço quando essa história existir.
- Estados transitórios de um envio em andamento ("enviando", "falhou") pertencem ao aplicativo, que os mostra enquanto espera a resposta; a API só conhece documentos concluídos, porque arquivo recusado ou não registrado nunca é persistido como documento.
- O estado do envio tem, por ora, um único valor para documentos disponíveis; estados de processamento (extração, conferência) serão acrescentados pelas histórias seguintes sem mudar o significado.
- O envio é permitido apenas em rascunho. Reenvio depois do primeiro processamento (que cria nova revisão e volta o pedido a "processando") pertence à história de reenvio.
- O aplicativo móvel e as telas não fazem parte desta entrega; a API é verificável sozinha. Os arquivos de teste são sintéticos.
- O banco do grupo é Oracle compartilhado, com tabelas de prefixo `RC_`. Qualquer alteração de esquema necessária é feita por script aditivo e executada pelo grupo; o esquema atual já tem a tabela de documentos, sem os campos de tipo documental e de identificação de tentativa.
- Depende de configuração da área privada de arquivos (`DOCUMENT_STORAGE_PATH`) para o envio funcionar de ponta a ponta; sem ela o envio responde indisponibilidade, e a consulta ao pedido continua funcionando.
- Dependências já concluídas: autenticação e sessão (RC-04), perfis e isolamento (RC-05), proteção de arquivos e entradas (RC-06) e rascunho com revisões (RC-07).
