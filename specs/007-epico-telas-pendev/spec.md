# Feature Specification: Épico de telas do aplicativo no pen.dev (somente design)

**Feature Branch**: `007-epico-telas-pendev`

**Created**: 2026-11-12

**Status**: Em andamento — documentos de apoio e textos prontos; quadros no pen.dev ainda não desenhados (ver tasks.md)

**Backlog**: novo épico "Telas do aplicativo (pen.dev)" a ser registrado em Code-Masters-F/ClickBus depois da aprovação deste texto. Cobre a parte de **tela** das histórias RC-07, RC-08, RC-09, RC-11, RC-15, RC-16, RC-17, RC-18, RC-19, RC-20 e RC-22. A parte de **API** dessas histórias continua nas issues originais.

**Input**: User description: "Épico de telas do RegulaCheck, somente design, sem backend, desenvolvido no pen.dev por uma pessoa Dev que vai assumir o épico, com dados sintéticos e sem chamar a API."

## Contexto e limite deste épico

O aplicativo do RegulaCheck será Android nativo, mas ainda não existe: a pasta `mobile/` só descreve o comportamento esperado. A API e seus contratos já estão definidos (`CONTRATO_API.md`, `docs/api/`). Este épico entrega o **desenho das telas** em arquivos do pen.dev, com **dados fictícios**, para que (a) o grupo veja e valide o percurso da paciente e do administrador antes de programar e (b) a pessoa que implementar o app tenha uma referência visual e de comportamento.

**Fica fora do épico:** backend, código Android/Kotlin, integração com a API, publicação de aplicativo e qualquer tela de cadastro, recuperação de senha ou MFA. Nenhuma tela chama a API: tudo o que aparece é texto e dado inventado, claramente sintético.

A pessoa Dev responsável assume o épico por inteiro. As histórias abaixo estão em ordem de prioridade e cada uma pode ser entregue e revisada sozinha.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fundamentos visuais e percurso de entrada: login, lista e detalhe de pedidos (Priority: P1)

A pessoa responsável cria a base visual (cores, tipografia, componentes, avisos de "acadêmico/sintético") e desenha o login, a lista de pedidos e o detalhe de um pedido, cada um com seus estados: carregando, vazio, erro, sem conexão, sessão expirada e sucesso.

**Why this priority**: todas as outras telas reaproveitam esses fundamentos e é o primeiro contato da paciente com o app; sem eles não há como revisar o restante de forma consistente.

**Independent Test**: abrir o arquivo de design, percorrer login → lista → detalhe e conferir, em cada tela, que existem todos os estados previstos e o aviso acadêmico, sem nenhuma outra tela pronta.

**Acceptance Scenarios**:

1. **Given** a tela de login, **When** a pessoa avalia o desenho, **Then** há campos de e-mail e senha, botão de entrar e variantes para: enviando (botão desabilitado com progresso), credenciais inválidas (mensagem recebida, e-mail mantido, senha limpa), serviço indisponível e falha de rede, além do aviso de que o app é acadêmico e usa dados sintéticos.
2. **Given** a lista de pedidos, **When** avaliada, **Then** cada item mostra estado do pedido, serviço e data da última atualização, e existem as variantes carregando, vazia (com orientação do próximo passo), erro (com botão de tentar novamente) e sucesso com pedidos em vários estados.
3. **Given** o detalhe do pedido, **When** avaliado, **Then** mostra os dados do pedido, a revisão atual, o estado e a próxima ação necessária, com variantes para cada um dos sete estados do pedido e para "item não encontrado".
4. **Given** qualquer tela, **When** a sessão expira, **Then** existe o desenho de volta ao login com a explicação de que é preciso entrar de novo; permissão negada (403) mostra "sem permissão para esta tela".
5. **Given** a lista, **When** o estado de um pedido é exibido, **Then** o estado é identificado por texto e ícone, nunca só por cor.

---

### User Story 2 - Envio: rascunho, serviço, documentos, substituição e exclusão (Priority: P1)

A pessoa desenha o fluxo em que a paciente cria o rascunho escolhendo localidade e serviço (com a indicação do protocolo acadêmico), anexa e consulta documentos, substitui ou remove um documento e exclui um rascunho com confirmação explícita.

**Why this priority**: é o início do percurso principal da demonstração e concentra os estados de erro de arquivo, que precisam de orientação clara.

**Independent Test**: percorrer rascunho → anexar documentos → substituir um → excluir o rascunho, conferindo cada estado e mensagem.

**Acceptance Scenarios**:

1. **Given** o formulário de rascunho, **When** avaliado, **Then** a localidade e o serviço demonstrativos aparecem como opções, com a indicação visível de "protocolo acadêmico fictício, não é exigência oficial", e há variantes de salvando, erro e campos preservados após falha.
2. **Given** a tela de documentos, **When** avaliada, **Then** a lista mostra nome, tipo, tamanho, estado do envio (aguardando, enviando, enviado, falhou) e a ação de abrir, com os limites visíveis (até 3 arquivos, 5 MB cada, PDF até 5 páginas, JPEG ou PNG).
3. **Given** um envio recusado, **When** avaliado, **Then** existem variantes para tipo não suportado, extensão incompatível, arquivo grande demais, PDF com mais de 5 páginas, PDF protegido por senha, arquivo ilegível e excesso de arquivos, cada uma com orientação e dizendo quando é preciso selecionar o arquivo de novo.
4. **Given** um documento enviado, **When** a pessoa avalia a substituição e a remoção, **Then** há confirmação, e fica claro que mudar o documento invalida a conferência anterior e exige nova análise.
5. **Given** um rascunho, **When** a pessoa avalia a exclusão, **Then** existe uma confirmação explícita que nomeia o que será apagado, com a opção de cancelar em destaque.
6. **Given** um envio interrompido, **When** avaliado, **Then** há o desenho de tentar de novo sem sugerir que o envio foi concluído.

---

### User Story 3 - Resultado, pendências, conferência da transcrição, reenvio e comprovante simulado (Priority: P1)

A pessoa desenha o núcleo da demonstração: a conferência da transcrição extraída, o resultado da análise com o resumo simulado e a lista de pendências, o reenvio da versão corrigida com a comparação entre pendências anteriores e atuais, e o comprovante em tela da submissão simulada.

**Why this priority**: é o valor central do RegulaCheck e a parte que mais exige cuidado com linguagem (sem diagnóstico, sem promessa).

**Independent Test**: percorrer extração → conferência → resultado com pendências → reenvio → resultado sem pendências → comprovante, conferindo textos e avisos.

**Acceptance Scenarios**:

1. **Given** a conferência da transcrição, **When** avaliada, **Then** cada campo extraído mostra seu valor, a origem (arquivo, página, trecho) e o status (identificado, não localizado ou ilegível), com os não verificáveis destacados por texto e ícone; a paciente pode confirmar ou marcar divergência; há aviso de que a leitura é simulada.
2. **Given** um campo ausente do documento, **When** avaliado, **Then** a tela orienta procurar a unidade emissora e não oferece preencher a informação clínica como se constasse no documento.
3. **Given** o resultado da análise, **When** avaliado, **Then** mostra o resultado geral, o resumo identificado como **simulado** e a lista de pendências, cada uma com o critério, a versão do protocolo, o trecho de origem quando houver e o responsável pela correção (paciente ou unidade emissora), sem depender só de cor.
4. **Given** pendências de foto cortada ou arquivo ausente, **When** avaliadas, **Then** a orientação é reenviar o arquivo; para informação clínica ausente ou documento médico incorreto, a orientação é procurar a unidade emissora.
5. **Given** o resultado "sem pendências identificadas", **When** avaliado, **Then** o texto deixa claro que isso não é aprovação do SUS nem promessa de vaga.
6. **Given** a tela de reenvio, **When** avaliada, **Then** mostra a comparação simples entre as pendências anteriores e as atuais, e uma pendência só aparece como "resolvida" depois do novo resultado, nunca pelo simples envio.
7. **Given** o comprovante da submissão simulada, **When** avaliado, **Then** exibe referência interna, data e o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", e a variante de repetir a operação mostra o mesmo comprovante.
8. **Given** estados de processamento, **When** avaliados, **Then** existem variantes para "processando" (que não equivale a concluído), "falha na análise" com tentar de novo, e "requer conferência".

---

### User Story 4 - Sensores, visão do administrador e conexão ruim com acessibilidade (Priority: P2)

A pessoa desenha os alertas de iluminação e distância da estação de captura, a visão do administrador (protocolo e indicadores agregados) e as telas e componentes de conexão ruim e acessibilidade que valem para todo o app.

**Why this priority**: completa a demonstração (sensores e perfil administrador) e garante qualidade de uso; depende dos fundamentos e do percurso principal.

**Independent Test**: conferir os alertas de sensores em cada condição, a visão do administrador sem nenhum dado individual e o guia de acessibilidade aplicado às telas.

**Acceptance Scenarios**:

1. **Given** a estação de captura, **When** avaliada, **Then** mostra luminosidade (lux) e distância (cm) com orientação quando fora da faixa, o aviso mudando quando volta ao normal, a leitura antiga marcada como desatualizada e o estado "sensores indisponíveis", que não bloqueia o envio nem o salvamento.
2. **Given** a visão do administrador, **When** avaliada, **Then** mostra o protocolo ativo e suas regras, o total de pedidos, as análises por resultado e as regras com mais pendências, e **não** exibe documento, nome, e-mail nem identificador de paciente; há a variante "sem permissão" para uma paciente que tenta acessar.
3. **Given** uma falha de conexão ou tempo esgotado, **When** avaliada, **Then** existe mensagem clara com botão de tentar novamente, os campos digitados são preservados, e a tela nunca declara envio concluído sem confirmação.
4. **Given** o conjunto das telas, **When** a pessoa aplica a lista de acessibilidade, **Then** todas atendem: informação nunca só por cor, contraste legível, alvos de toque confortáveis, textos de erro que dizem o que fazer, ordem de leitura lógica, rótulos para leitores de tela e texto ampliável sem quebrar o layout.

---

### Edge Cases

- Nome de arquivo muito longo, com acentos ou caracteres estranhos: o desenho mostra truncamento e o nome completo ao abrir.
- Texto vindo do documento ou do resumo com símbolos como `<` ou `&`: aparece como texto, sem formatação.
- Lista com muitos pedidos: o desenho prevê rolagem e a ordenação pela última atualização.
- Pedido em `PROCESSANDO` enquanto a paciente tenta alterar documentos: a tela explica que uma nova revisão será iniciada e o resultado anterior deixará de valer.
- Conta que perde a permissão no meio do uso: voltar ao login ou mostrar "sem permissão" conforme o código recebido.
- Primeiro uso, sem nenhum pedido: o estado vazio explica como começar.
- Telas em aparelho pequeno e em fonte aumentada: nenhum texto essencial é cortado.

## Requirements *(mandatory)*

### Functional Requirements

**Entrega e organização**

- **FR-001**: O épico MUST entregar os desenhos em arquivos do pen.dev guardados no repositório (pasta `mobile/design/`), com nomes e organização que permitam achar cada tela e cada estado.
- **FR-002**: Cada tela MUST cobrir os estados aplicáveis entre: carregando, vazio, sucesso, erro, sem conexão, sessão expirada (401), sem permissão (403) e item não encontrado (404). Quais estados valem para cada tela, e se são cobertos por quadro próprio ou por quadro/componente compartilhado, é definido pela **matriz tela × estado** em `contracts/telas.md`; essa matriz é o critério de revisão.
- **FR-003**: O conjunto MUST incluir um catálogo de componentes e estilos (cores, tipografia, espaçamento, botões, campos, avisos, cartões de estado) reutilizados pelas telas.
- **FR-004**: Cada tela MUST indicar, em nota do design, a rota da API e o contrato de que depende (referência a `CONTRATO_API.md` e `docs/api/`), sem chamar a API.
- **FR-005**: O épico MUST incluir um mapa de percursos que mostre como as telas se ligam (entrada, envio, resultado, reenvio, comprovante, administrador).
- **FR-006**: Cada história do épico MUST ter critérios de aceite verificáveis por revisão visual com o checklist de revisão do épico.

**Conteúdo e linguagem (valem para todas as telas)**

- **FR-007**: Todas as telas MUST usar somente dados sintéticos e fictícios, identificados como tais, e MUST mostrar o aviso de que o app é acadêmico e o protocolo é fictício, não exigência oficial.
- **FR-008**: O resumo de pendências e a extração MUST aparecer identificados como **simulados**; nenhuma tela pode sugerir leitura ou IA reais.
- **FR-009**: Nenhuma tela MUST diagnosticar, definir urgência, autorizar atendimento, prometer vaga, aprovação ou redução de fila, nem apresentar "sem pendências identificadas" como aprovação do SUS. Frases que negam essas afirmações (por exemplo, "não é aprovação do SUS") são obrigatórias nos avisos e permitidas.
- **FR-010**: Informação clínica ausente ou documento médico incorreto MUST orientar a paciente a procurar a unidade emissora; a tela MUST NOT oferecer preencher a informação clínica como se constasse no documento original.
- **FR-011**: O comprovante MUST exibir o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS" e MUST NOT parecer documento clínico nem protocolo oficial.
- **FR-012**: Texto vindo de documento ou do resumo MUST ser desenhado como texto puro (sem formatação interpretada).
- **FR-013**: Mensagens MUST estar em português do Brasil, em linguagem simples, e dizer o que a paciente pode fazer em seguida.

**Percursos de telas**

- **FR-014**: Login MUST cobrir: enviando, credenciais inválidas (e-mail mantido, senha limpa), serviço indisponível e falha de rede, e MUST NOT incluir cadastro, recuperação de senha nem MFA.
- **FR-015**: A lista e o detalhe de pedidos MUST mostrar estado, serviço e última atualização, e o detalhe MUST ter uma variante para cada um dos sete estados do pedido (`RASCUNHO`, `PROCESSANDO`, `REQUER_CONFERENCIA`, `COM_PENDENCIAS`, `SEM_PENDENCIAS_IDENTIFICADAS`, `FALHA_ANALISE`, `SUBMETIDA_SIMULACAO`).
- **FR-016**: O rascunho MUST oferecer localidade e serviço demonstrativos com indicação do protocolo acadêmico.
- **FR-017**: A tela de documentos MUST mostrar nome, tipo, tamanho, estado do envio e ação de abrir, os limites do MVP e uma variante para cada recusa de arquivo (tipo, extensão, tamanho, páginas, proteção, ilegível, quantidade), dizendo quando é preciso selecionar o arquivo de novo.
- **FR-018**: Substituir, remover e excluir rascunho MUST ter confirmação explícita; a exclusão MUST nomear o que será apagado; substituir MUST avisar que invalida a conferência anterior.
- **FR-019**: A conferência da transcrição MUST mostrar valor, origem e status (identificado, não localizado, ilegível) de cada campo, destacar os não verificáveis e permitir confirmar ou marcar divergência.
- **FR-020**: O resultado MUST mostrar resultado geral, resumo simulado e pendências com critério, versão do protocolo, trecho de origem (quando houver) e responsável pela correção; MUST NOT depender só de cor.
- **FR-021**: O reenvio MUST mostrar a comparação simples entre pendências anteriores e atuais e MUST NOT marcar pendência como resolvida antes do novo resultado.
- **FR-022**: Os estados `PROCESSANDO` (que não equivale a concluído), `FALHA_ANALISE` (com tentar de novo) e `REQUER_CONFERENCIA` MUST ter desenho próprio.
- **FR-023**: Os alertas de sensores MUST mostrar lux e cm, orientar quando fora da faixa, identificar leitura antiga como desatualizada e tratar sensores indisponíveis sem bloquear envio ou salvamento.
- **FR-024**: A visão do administrador MUST mostrar protocolo ativo, regras e agregados (total de pedidos, análises por resultado, regras com mais pendências) e MUST NOT exibir documento nem dado que identifique paciente.
- **FR-025**: Falha de conexão e tempo esgotado MUST ter mensagem e botão de tentar novamente, preservando os campos digitados e nunca declarando envio concluído sem confirmação.

**Acessibilidade**

- **FR-026**: Todas as telas MUST cumprir a lista de acessibilidade do épico: informação nunca só por cor, contraste adequado (texto com razão de pelo menos 4,5:1), alvos de toque confortáveis (pelo menos 48 dp, texto base de pelo menos 16 sp), erros que dizem como corrigir, ordem de leitura lógica, rótulos para leitores de tela e suporte a texto ampliado.

**Verificação**

- **FR-027**: O épico MUST ter um checklist de revisão das telas (conteúdo, estados, acessibilidade, linguagem proibida) e o resultado da revisão MUST ser registrado, indicando o que foi e o que não foi revisado.

### Key Entities *(include if feature involves data)*

- **Arquivo de design**: arquivo `.pen` com as telas de um grupo; possui páginas ou quadros nomeados por tela e estado.
- **Tela**: desenho de uma visão do app; possui variantes de estado e uma nota com a rota e o contrato da API de que dependerá.
- **Variante de estado**: versão de uma tela para carregando, vazio, sucesso, erro, sem conexão, sessão expirada, sem permissão ou item não encontrado.
- **Componente**: elemento reutilizável do catálogo (botão, campo, cartão de pedido, selo de estado, aviso acadêmico, aviso de simulação).
- **Dado fictício**: texto de exemplo (nomes, datas, pedidos, pendências) que não pertence a pessoa real.
- **Percurso**: sequência de telas que conta uma jornada (entrada, envio, resultado, reenvio, comprovante, administrador).
- **Checklist de revisão**: lista de verificação aplicada às telas, com o resultado registrado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das telas previstas nas quatro histórias existem no design e, para cada tela, 100% das células da matriz tela × estado estão cumpridas: quadro próprio onde a matriz exige, e nota apontando o quadro ou componente compartilhado onde a matriz diz "coberto".
- **SC-002**: Em 100% das telas aparece o aviso acadêmico/sintético, e em 100% das telas que mostram resumo, extração ou comprovante a identificação de simulado.
- **SC-003**: A revisão de linguagem não encontra, em nenhuma tela, texto que **afirme** diagnóstico, urgência, autorização de atendimento, vaga ou aprovação, nem trate "sem pendências identificadas" como aprovação do SUS (0 ocorrências). Avisos que **negam** isso ("isto não é aprovação do SUS", "não entra em fila real") são exigidos e permitidos.
- **SC-004**: 100% das telas passam na lista de acessibilidade do épico, e em 0 telas uma informação depende só de cor.
- **SC-005**: Uma pessoa do grupo que não desenhou as telas consegue percorrer o percurso principal (login → rascunho → documentos → resultado com pendências → reenvio → comprovante) apenas pelo mapa de percursos, sem ajuda, em até 5 minutos. Este critério só é dado como verificado se medido com uma pessoa.
- **SC-006**: Uma pessoa que vai implementar o app localiza, em até 2 minutos, para qualquer tela, a rota da API e o contrato de que ela depende.
- **SC-007**: O responsável pelo grupo revisa e aprova o design de cada história, e as correções pedidas ficam registradas.

## Assumptions

- O trabalho de design usa o pen.dev e seu servidor de ferramentas (pencil MCP), operado por uma pessoa Dev que assume o épico; a revisão e a aprovação do grupo não são substituídas por apoio automatizado.
- Os arquivos de design ficam em `mobile/design/` no repositório; arquivos `.pen` são tratados como binários do projeto e não são lidos como texto.
- O app é Android nativo (Kotlin), decisão já adotada; o design segue as convenções de telas do Android para aparelho celular, em retrato, e não define código.
- A fonte de verdade de conteúdo e comportamento é o repositório: `mobile/README.md`, `CONTRATO_API.md`, `docs/api/` (estados, erros, contratos), `docs/protocolo-academico.md` e as issues RC-07, RC-08, RC-09, RC-11, RC-15, RC-16, RC-17, RC-18, RC-19, RC-20 e RC-22. Quando uma tela mostrar algo que o contrato ainda não prevê, a diferença fica anotada, não inventada.
- Os dados fictícios usam o recorte já adotado: Município Exemplo (fictício), consulta ambulatorial em cardiologia como contexto documental e `PROTOCOLO-ACADEMICO-FICTICIO v0.1`.
- A paciente é a usuária principal; o administrador tem uma visão separada e restrita a agregados. O simulador de sensores não tem tela própria: o app só exibe os alertas.
- Não há protótipo executável nem código; "ligar" telas é feito no mapa de percursos, não por navegação programada, salvo decisão posterior do grupo.
- A parte de API de cada história original (RC-07 a RC-22) continua nas issues existentes; este épico não as fecha, apenas cobre o desenho de suas telas.
- Fora do escopo: cadastro, recuperação de senha, MFA, notificações, modo escuro, outros idiomas, tablet e publicação do app.
- Imagens e documentos usados nos desenhos são sintéticos; nenhum documento real de paciente entra no design.
