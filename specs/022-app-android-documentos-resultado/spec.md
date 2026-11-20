# Feature Specification: App Android — envio de documentos e resultado da análise (RC-19)

**Feature Branch**: `022-app-android-documentos-resultado`

**Created**: 17/11/2026

**Status**: Implementada e verificada em 17/11/2026 (PR #74): 82 testes; percurso com backend real e Oracle em cerca de 4 min, 3 pedidos conferidos com `GET …/analises`, arquivo inválido, rede cortada e logcat ([evidência](../../docs/evidencias/2026-11-17_rc19-app-documentos-resultado.md)). Textos do resultado ajustados em 19/11/2026 (PR #82; 123 testes, 0 falhas, conferido no emulador)

**Input**: Issue [RC-19 #42](https://github.com/Code-Masters-F/RegulaCheck/issues/42) (Épico 6, [#40](https://github.com/Code-Masters-F/RegulaCheck/issues/40)), incluindo a seção "Acrescentado em 17/11/2026" (tela da RC-15, [#37](https://github.com/Code-Masters-F/RegulaCheck/issues/37)). Base: app da RC-18 ([spec 017](../017-app-android-login-pedidos/spec.md)). Contrato de apresentação do resultado: [spec 019 — apresentacao-resultado.md](https://github.com/Code-Masters-F/RegulaCheck/blob/main/specs/019-exibir-resultado-correcao/contracts/apresentacao-resultado.md) (na `main`).

Os critérios de aceite da #42 são a fonte da verdade e não são copiados aqui; esta spec organiza-os em histórias e explicita os limites desta versão. Referência visual: `mobile/design/INDEX.md` (E2.1, E2.2, E2.3, E2.7, E3.1, E3.8, E3.3, E3.4) e textos exatos em `mobile/design/textos.md`.

## Clarifications

### Session 2026-11-19

- Q: A tela de resultado repete o aviso de simulação e mostra informações técnicas (revisão, código da regra, "só no desenho nesta versão") que não ajudam a paciente. O que fica? → A (decisão do responsável, 19/11/2026, depois da gravação da demo): a simulação fica identificada uma vez por tela, pelo selo SIMULADO do cabeçalho e pelo rodapé acadêmico. Saem: o selo próprio do resumo e a nota genérica de simulação dele (a nota do resumo padrão, `PADRAO`, continua), a revisão em "Verificado em" (o "Resultado anterior" ainda identifica a revisão avaliada, FR-017), o aviso da API fora de "Sem pendências identificadas", as frases "só no desenho nesta versão" (resultado, leitura e detalhe) e o código da regra na visão da paciente. Os avisos que negam aprovação do SUS e garantia de aceitação permanecem (FR-021). Só texto: nenhum comportamento nem chamada à API muda.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Criar um pedido com o serviço demonstrativo (Priority: P1)

A paciente, já autenticada, toca em "Novo pedido", vê o serviço demonstrativo (o primeiro disponível) identificado como protocolo acadêmico fictício, apenas confirma e chega à tela de documentos do rascunho criado.

**Why this priority**: sem rascunho não há documentos nem análise; é a porta de entrada do percurso.

**Independent Test**: com uma conta de paciente, criar um rascunho e conferir que ele aparece em "Meus pedidos" com o mesmo serviço.

**Acceptance Scenarios**:

1. **Given** há serviços ativos, **When** abre "Novo pedido", **Then** vê o primeiro serviço com especialidade, localidade, versão e o aviso de que é fictício, e o botão "Criar rascunho".
2. **Given** tocou "Criar rascunho", **When** a criação está em andamento, **Then** o botão mostra "Criando…" e não aceita novo toque.
3. **Given** a criação falha de forma recuperável (rede, tempo, 503), **When** a falha aparece, **Then** o serviço continua mostrado e há "Tentar de novo"; em tempo esgotado a paciente é avisada de que o rascunho pode ter sido criado.
4. **Given** não há serviços, **When** abre "Novo pedido", **Then** vê "Nenhum serviço disponível agora." sem ação de criar.

---

### User Story 2 - Enviar documentos com confirmação da API (Priority: P1)

A paciente escolhe o tipo do documento, seleciona um arquivo pelo seletor do sistema (PDF, JPEG ou PNG, sem permissão de armazenamento) e acompanha o envio. O arquivo só entra na lista depois que a API confirma. Recusas mostram a mensagem da API e pedem nova escolha; falhas de rede permitem repetir o mesmo envio sem duplicar.

**Why this priority**: os critérios só podem ser verificados sobre documentos realmente enviados.

**Independent Test**: anexar um PDF sintético de `samples/`, repetir após cortar a rede e conferir que a lista e `GET …/documentos` mostram um único documento.

**Acceptance Scenarios**:

1. **Given** o rascunho tem menos de 3 documentos, **When** a paciente escolhe tipo e arquivo, **Then** a linha "Enviando…" aparece, as ações ficam bloqueadas e, após 201 ou 200, o documento aparece como "Enviado".
2. **Given** um envio falhou por rede ou tempo, **When** toca "Tentar de novo", **Then** o app reenvia o mesmo arquivo com a mesma chave de envio; uma repetição já gravada volta 200 e não duplica.
3. **Given** a API recusa o arquivo (400, 413, 415 ou 422), **When** a recusa chega, **Then** uma faixa mostra a mensagem da API, diz que nada foi salvo e pede para escolher o arquivo de novo; o arquivo não entra na lista.
4. **Given** o arquivo escolhido deixou de estar acessível (permissão perdida), **When** tenta enviar, **Then** o app pede para escolher o arquivo outra vez.
5. **Given** a tela de documentos, **When** é exibida, **Then** os limites estão visíveis (3 arquivos, 5 MB cada, PDF até 5 páginas, JPEG ou PNG).

---

### User Story 3 - Ler e verificar os critérios sem duplicar operações (Priority: P1)

Com documentos enviados, a paciente pede a leitura simulada e, em seguida, a verificação dos critérios. Durante cada operação os botões ficam bloqueados ("Lendo…", "Analisando…"); nada é repetido automaticamente; uma conclusão só aparece com confirmação da API.

**Why this priority**: é o passo que produz o resultado que a paciente precisa entender.

**Independent Test**: ler e verificar um pedido; repetir toques durante as operações e conferir no histórico de análises que há uma única análise nova.

**Acceptance Scenarios**:

1. **Given** documentos enviados, **When** toca "Ler documentos", **Then** vê "Lendo seus documentos" com SIMULADO e, ao terminar, o que foi lido de cada documento e o botão "Verificar critérios".
2. **Given** a leitura terminou, **When** toca "Verificar critérios", **Then** confirma num diálogo e o botão passa a "Analisando…", bloqueado, até a resposta.
3. **Given** a análise passa do tempo ou a conexão cai, **When** a falha aparece, **Then** o app consulta o histórico de análises antes de oferecer nova tentativa e mostra o resultado se ele já existe.
4. **Given** já há análise em andamento (409 `ANALISE_EM_ANDAMENTO`), **When** a resposta chega, **Then** vê a espera em segundos informada pela API e pode consultar o resultado depois, sem repetição automática.
5. **Given** a leitura ou a análise está indisponível (503), **When** a falha aparece, **Then** vê mensagem própria e "Tentar de novo"; nada é dado como lido ou verificado.
6. **Given** não há documentos (422 `SEM_DOCUMENTOS`) ou não há leitura atual (409), **When** a resposta chega, **Then** a paciente é levada a anexar ou ler de novo.

---

### User Story 4 - Entender o resultado e cada pendência (Priority: P1)

A paciente vê o resultado da análise vigente em texto e ícone, o resumo simulado (ou o aviso de resumo indisponível com nova tentativa), a lista do que falta com quem resolve e os demais critérios. No detalhe de cada pendência vê critério, versão do protocolo, situação, responsável registrado pela regra, orientação e trecho de origem quando existir, distinguindo o que ela mesma resolve do que exige a unidade emissora.

**Why this priority**: é o valor final da história (RC-15 incorporada à RC-19).

**Independent Test**: em 3 pedidos, comparar a tela com `GET …/analises` (mesmas pendências, mesma análise vigente).

**Acceptance Scenarios**:

1. **Given** uma análise vigente, **When** abre o resultado, **Then** vê `rotuloResultado` com ícone, data, versão do protocolo, selo SIMULADO no cabeçalho e só os dados da análise vigente.
2. **Given** a análise vigente não tem resumo, **When** abre o resultado, **Then** vê "O resumo não está disponível agora. Os critérios abaixo continuam valendo." e "Tentar de novo"; resultado e pendências continuam visíveis.
3. **Given** o resumo veio como padrão (`PADRAO`), **When** abre o resultado, **Then** vê a explicação padrão identificada como SIMULADO e "Tentar de novo".
4. **Given** resultado "Confira a leitura", **When** abre o resultado, **Then** vê que informação não verificável não significa falta.
5. **Given** resultado sem pendências identificadas, **When** abre o resultado, **Then** vê que isso não é aprovação do SUS, que os critérios são demonstrativos e que não há garantia de vaga nem prazo.
6. **Given** uma pendência, **When** abre o detalhe, **Then** vê critério, versão, situação, "Responsável registrado pela regra", a orientação da API como texto puro, o documento de origem e página/trecho apenas se existirem, e nenhum botão de ação indisponível nesta versão.
7. **Given** não há análise vigente, a análise está em andamento ou a consulta falhou, **When** abre o resultado, **Then** vê o estado em texto, sem conclusão nova, com opção de consultar de novo quando cabível; análises antigas só aparecem como "Resultado anterior".

---

### User Story 5 - Conexão ruim e acessibilidade mínima (Priority: P2)

Em todas as operações novas, sem conexão, tempo esgotado, sessão expirada, acesso negado e pedido não encontrado têm mensagem legível e caminho de recuperação; a escolha da paciente é preservada nas falhas recuperáveis; estado nunca é comunicado só por cor.

**Why this priority**: absorve o antigo escopo de "conexão ruim e acessibilidade" do épico; transversal às histórias 1–4.

**Independent Test**: cortar a rede do emulador durante cada operação e conferir mensagem, preservação e retomada.

**Acceptance Scenarios**:

1. **Given** qualquer operação nova, **When** a rede cai, **Then** vê "Sem conexão com a internet." ou a mensagem de tempo esgotado e "Tentar novamente", e o que escolheu foi mantido.
2. **Given** a sessão expira durante qualquer operação, **When** chega o 401, **Then** volta ao login com o aviso de sessão expirada (comportamento da RC-18).
3. **Given** uma tela nova, **When** lida por leitor de tela, **Then** campos e ícones de ação têm rótulo, títulos são cabeçalhos e estados têm texto.

### Edge Cases

- Toques repetidos em "Criar rascunho", "Anexar documento", "Tentar de novo", "Ler documentos", "Verificar critérios" e no "Tentar de novo" do resumo não geram operações duplicadas.
- Criação de rascunho em tempo esgotado: a API não deduplica criação; o app avisa que o rascunho pode ter sido criado e orienta conferir "Meus pedidos" antes de repetir.
- Chave de envio reaproveitada com outro conteúdo (409 `CHAVE_ENVIO_REUTILIZADA`): pede nova escolha do arquivo.
- Pedido que deixou de aceitar documentos (409 `CONFLITO_ESTADO` no envio) ou que já foi verificado: sem "Anexar documento".
- Arquivo fora do catálogo da leitura simulada: o documento aparece como não lido, com motivo e orientação do backend.
- Texto da API com `<`, `>` ou `&` aparece literalmente.
- Análise vigente sem resumo, com resumo `PADRAO`, ou resumo que responde 409 (o histórico é consultado de novo antes de reapresentar).
- Pedido com histórico mas sem análise vigente; pedido em `FALHA_ANALISE`; análise `EM_ANDAMENTO`.
- Valores desconhecidos de enums (tipo, situação, motivo): exibidos de forma neutra, sem derrubar a tela.

## Requirements *(mandatory)*

### Functional Requirements

**Novo pedido (E2.1)**

- **FR-001**: O app MUST mostrar somente o primeiro serviço devolvido pela lista de serviços, com especialidade, localidade, versão e aviso fictício do backend; a paciente só confirma.
- **FR-002**: Lista vazia MUST mostrar "Nenhum serviço disponível agora." e bloquear a criação; falha da consulta MUST oferecer "Tentar de novo".
- **FR-003**: O botão de criar MUST ficar bloqueado durante a criação; falha recuperável MUST manter o serviço visível com "Tentar de novo"; serviço recusado MUST mostrar "Escolha um dos serviços da lista."; tempo esgotado MUST avisar que a criação pode ter ocorrido.
- **FR-004**: Criado o rascunho, o app MUST abrir a tela de documentos desse pedido.

**Documentos (E2.2, E2.3, E2.7)**

- **FR-005**: A escolha do arquivo MUST usar o seletor do sistema limitado a PDF, JPEG e PNG, sem pedir permissão de armazenamento, depois da escolha do tipo documental.
- **FR-006**: A chave de envio MUST ser gerada ao escolher o arquivo e reutilizada em todas as tentativas daquele arquivo.
- **FR-007**: Um documento MUST entrar na lista somente depois de 201 ou 200 da API; durante o envio a linha mostra "Enviando…" e as demais ações ficam bloqueadas.
- **FR-008**: Recusa 400/413/415/422 MUST mostrar a mensagem da API numa faixa com "Nada foi salvo." e "Escolher outro arquivo", sem listar o arquivo; 409 `CHAVE_ENVIO_REUTILIZADA` ou arquivo inacessível MUST pedir nova escolha; 409 de estado MUST informar que o pedido não recebe mais documentos.
- **FR-009**: Falha de rede, tempo ou 503 MUST marcar o arquivo como "Não enviado" com "Tentar de novo" (mesmo arquivo, mesma chave).
- **FR-010**: Os limites (3 arquivos, 5 MB, PDF até 5 páginas, JPEG ou PNG) MUST ficar visíveis; com 3 documentos ou fora de rascunho, "Anexar documento" MUST sumir.
- **FR-011**: Com ao menos um documento e o pedido em rascunho, MUST haver "Ler documentos".

**Leitura e análise (E3.1, E3.8)**

- **FR-012**: Leitura e análise MUST bloquear seus botões enquanto há operação ("Lendo…", "Analisando…"), MUST usar tempo de leitura estendido e MUST NOT repetir automaticamente.
- **FR-013**: A leitura MUST mostrar SIMULADO e o aviso de leitura simulada; concluída, MUST mostrar a situação de cada documento lido (e motivo/orientação do backend quando não lido); 503 MUST oferecer "Tentar de novo"; 422 `SEM_DOCUMENTOS` MUST levar a anexar; 409 MUST oferecer ler de novo ou informar que o pedido só pode ser consultado.
- **FR-014**: A análise MUST ser pedida após confirmação num diálogo; 201/200 MUST abrir o resultado; 409 `ANALISE_EM_ANDAMENTO` MUST mostrar a espera de `Retry-After`; 409 sem leitura MUST oferecer ler de novo; 503 MUST oferecer "Tentar de novo".
- **FR-015**: Em tempo esgotado ou queda de conexão durante a análise, o app MUST consultar o histórico de análises antes de oferecer nova tentativa: análise vigente → mostrar resultado; em andamento → informar e permitir consultar depois; sem resultado → oferecer "Tentar de novo"; consulta também falhou → oferecer consultar de novo.
- **FR-016**: Depois de uma análise confirmada, o app MUST pedir o resumo simulado uma única vez, sem repetir automaticamente em falha.

**Resultado e pendência (E3.3, E3.4 — contrato da spec 019)**

- **FR-017**: O resultado MUST usar somente a análise com `vigente=true` cujo `id` é `vigenteId`; análises não vigentes só aparecem como "Resultado anterior", com a revisão avaliada.
- **FR-018**: O resultado MUST exibir `rotuloResultado` com ícone, data de `executadaEm` (sem a revisão), versão do protocolo, selo SIMULADO no cabeçalho, a lista "O que falta" (critério, tipo, quem resolve) e os demais critérios (Atende, Não se aplica), sem o código da regra; o aviso da API aparece só em "Sem pendências identificadas" (ajuste de 19/11/2026).
- **FR-019**: Resumo presente MUST aparecer sob o selo SIMULADO do cabeçalho da tela, sem selo próprio nem nota genérica de simulação (a nota de `PADRAO` permanece); ausente MUST mostrar o texto de indisponível com "Tentar de novo"; `PADRAO` MUST mostrar a explicação padrão identificada com "Tentar de novo"; 409 no resumo MUST reconsultar o histórico antes de reapresentar; falha do resumo MUST NOT esconder resultado ou pendências.
- **FR-020**: "Confira a leitura" MUST informar que informação não verificável não significa falta. A frase "só no desenho nesta versão" saiu do resultado, da leitura e do detalhe em 19/11/2026.
- **FR-021**: Todo resultado MUST manter o aviso permanente de que nenhuma pendência identificada não garante aceitação pela regulação e de que os critérios são demonstrativos; "Sem pendências identificadas" MUST negar aprovação do SUS, vaga e prazo.
- **FR-022**: O detalhe da pendência MUST mostrar `descricaoRegra` (sem o `codigoRegra` desde 19/11/2026), versão do protocolo, tipo e motivo, explicação, "Responsável registrado pela regra" (valor da API, sem reescrever), `orientacao` da API e, por evidência, o nome do arquivo obtido de `documentos[]` pelo `documentoId`, o campo, a situação e página/trecho apenas quando existirem.
- **FR-023**: O detalhe MUST distinguir o que a paciente resolve do que exige a unidade emissora sem afirmar qual original está errado e MUST NOT oferecer edição do original, reenvio, substituição ou qualquer botão inoperante; quando a correção depender de arquivo novo, MUST explicar que o reenvio não está disponível nesta versão.
- **FR-024**: Sem análise, em andamento, em falha da análise, e em 401/403/404/503/rede, o resultado MUST seguir a tabela do contrato de apresentação, sem apresentar conclusão não confirmada.

**Transversais**

- **FR-025**: Todo texto vindo da API MUST ser exibido como texto puro.
- **FR-026**: Toda tela nova MUST ter o rodapé acadêmico; leitura, resultado e detalhe MUST ter o selo SIMULADO no cabeçalho, que também cobre o resumo.
- **FR-027**: Falhas recuperáveis MUST preservar o que a paciente escolheu (serviço, tipo, arquivo); dado indisponível MUST NOT aparecer como resultado negativo.
- **FR-028**: Nenhum log MUST conter token, senha, cabeçalho, corpo de requisição/resposta ou conteúdo de documento.
- **FR-029**: As ações da RC-18 que avisavam "etapa ainda não disponível" MUST navegar para as telas desta história.
- **FR-030**: O app MUST NOT alterar o backend nem implementar reenvio, substituição, remoção, conferência da transcrição, abertura de documento, fotografia ou conclusão da simulação.

### Key Entities

- **Serviço demonstrativo**: protocolo acadêmico fictício ativo (identificador, versão, especialidade, localidade, aviso).
- **Documento enviado**: arquivo confirmado pela API (tipo documental, nome de exibição, formato, tamanho, páginas, estado do envio).
- **Envio em curso**: escolha local ainda não confirmada (tipo, arquivo, chave de envio, situação: enviando, não enviado, recusado, escolher de novo).
- **Leitura simulada**: situação por documento (lido, protegido, leitura vazia) com aviso e orientação do backend.
- **Análise**: resultado geral e rótulo, revisão, vigência, protocolo, aviso, documentos avaliados, resultados por regra, pendências e resumo opcional.
- **Pendência**: critério, tipo, motivo, explicação, responsável, orientação e evidências (documento, campo, situação, origem opcional).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Com o backend real, o percurso entrar → criar pedido → anexar 2 documentos → ler → verificar → ver pendências é concluído em até 5 minutos, cronometrado e registrado.
- **SC-002**: Em 3 pedidos conferidos, as pendências e o resultado mostrados são idênticos aos da análise vigente do histórico de análises (100% de coincidência).
- **SC-003**: Toques repetidos durante criação, envio, leitura, análise e resumo produzem 0 pedidos, documentos ou análises duplicados.
- **SC-004**: Em 100% das falhas exercitadas (arquivo inválido, rede cortada no envio e na análise, serviço indisponível), a paciente vê mensagem legível e um caminho para tentar de novo, sem conclusão não confirmada.
- **SC-005**: 100% das telas novas têm o rodapé acadêmico; 100% das telas de leitura, resultado e pendência têm SIMULADO.
- **SC-006**: Numa execução revisada, o log do processo do app não contém token, senha nem conteúdo de documento.
- **SC-007**: Testes automatizados cobrem envio 201 e 200, recusa 413/415, 422 `SEM_DOCUMENTOS`, 409 sem leitura, 409 `ANALISE_EM_ANDAMENTO` com `Retry-After` e 503, todos passando.
- **SC-008** (SC-005 da spec 019): numa avaliação com pelo menos 3 pessoas que não desenharam o fluxo, ao menos 2 dizem em até 2 minutos por cenário qual pendência resolvem e qual exige a unidade emissora; resultado real registrado ou registrado como não realizado.

## Assumptions

- Backend com a RC-15 (commit `65cbadf`, na `main`): `rotuloResultado` e `pendencias[].orientacao` presentes; resumo pela RC-14. Se o backend em execução não tiver esses campos, o app usa os rótulos locais equivalentes e omite a orientação, sem falhar.
- O percurso usa só "Escolher arquivo"; "Fotografar" e as condições da captura (E2.7 Origem, E4.1) continuam só no desenho (sensores são opcionais, RC-22).
- A conferência da transcrição (E3.2) fica fora (#40); o diálogo E3.8 não mostra a contagem de informações sem decisão, porque o app não consulta a conferência.
- "Abrir" documento, "Mais opções", substituir, remover, "Já tenho uma nova versão" e "Concluir simulação" não aparecem (rotas fora da #42 ou histórias RC-09/RC-16/RC-17); os textos do desenho que os pressupõem são ajustados e registrados em `textos.md` como RC-19.
- O pedido de resumo após a análise conta como primeira tentativa, não como retentativa automática; falhas posteriores só são repetidas pela paciente.
- A base é a branch da RC-18 (PR #71 ainda aberto); o PR desta feature depende dele.
- Atualização de 17/11/2026: a `main` recebeu a RC-16 (reenvio, comparação) e a RC-17 (submissão simulada); a feature passou a ser a 022 (research D13). O app ainda não oferece reenvio nem conclusão; os textos dizem "ainda não disponível neste app" em vez de afirmar impossibilidade.
