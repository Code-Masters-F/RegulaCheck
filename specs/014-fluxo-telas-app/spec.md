# Feature Specification: Fluxo de telas e informação de cada tela do aplicativo

**Feature Branch**: `014-fluxo-telas-app` (diretório da feature; sem branch própria — trabalho em `codex/007-design-system-exploracao`)

**Created**: 2026-11-13

**Status**: Draft

**Input**: User description: "Planejar o fluxo de telas do aplicativo da paciente (e do administrador) e as informações de cada tela, alinhado ao design system v1.0 e às rotas reais da API (specs 008–013 já no main), resolvendo ambiguidades do épico 007."

**Backlog**: Épico 10 — Telas do aplicativo (issue #14): RC-36 (#64), RC-37 (#65), RC-38 (#17), RC-39 (#16). Consome as histórias de backend RC-04, RC-07 a RC-12 (implementadas) e RC-13 a RC-17, RC-20 a RC-22 (contratadas, ainda não implementadas).

**Relação com o épico 007**: o épico 007 continua dono do processo de design (pen.dev, revisão, aprovação). Esta feature substitui o **inventário de telas e o conteúdo por tela** de `specs/007-epico-telas-pendev/contracts/telas.md`, que foi escrito antes das histórias 008–013 e diverge do backend atual. Os IDs de tela do 007 (E1.1 … E4.4) são mantidos; telas novas recebem IDs novos.

**Regra que governa esta feature (decisão do responsável, 13/11/2026)**: as telas **acompanham o que o backend decidiu**. Toda ação desenhada corresponde a uma operação existente ou contratada; toda informação exibida vem de um dado fornecido pelo sistema ou de um texto fixo de `mobile/design/textos.md`. O design não cria regra de negócio. Onde o backend limita o percurso, a tela explica a limitação; a limitação é registrada para o dono da história, sem propor lógica nova.

## Clarifications

### Session 2026-11-13

- Q: Quem decide regras de negócio e funcionalidade das telas? → A: O backend. O responsável pelo design não altera lógica; perguntas a ele se restringem a decisões de tela.
- Q: A paciente pode pedir a verificação dos critérios com campos lidos ainda sem decisão? → A: Segue o backend, que não bloqueia: a tela permite e mostra quantos campos estão sem decisão e que eles contarão como não verificados.
- Q: Como a correção trata documento que falta depois da primeira verificação? → A: Segue o backend: documento novo só é aceito em rascunho; depois disso a tela oferece apenas substituir ou remover. A limitação é explicada na tela e registrada para a RC-16.
- Q: Onde aparecem os sensores (estação de captura)? → A (decisão de tela, reversível): no anexo por foto, antes de fotografar. "Escolher arquivo" não passa pelos sensores.
- Q: Formato da conferência da leitura → A (decisão de tela, reversível): um documento por vez ("Documento 1 de 2"), salvando as decisões de cada documento antes de seguir.
- Q: Como identificar o pedido, se o sistema não tem código como "PED-0004"? → A: "Pedido {número}" usando o identificador fornecido pelo sistema. Os exemplos atuais do pen.dev com "PED-000X" devem ser ajustados.
- Q: Os quadros podem mostrar os valores literais da carga e dos catálogos de teste ("Paciente A (fictícia)", "Maria Exemplo da Silva", "texto sintetico" sem acento, "[SIMULADO]…")? → A: Não (decisão do responsável, 13/11/2026). A **estrutura** segue o backend — campos, estados, situações, regras, quantidades e referências —, mas os **valores exibidos** são exemplares realistas e fictícios, em português correto, para dar imersão na avaliação. O aviso acadêmico, o selo SIMULADO e a identificação do protocolo como fictício continuam obrigatórios.
- Q: Há barra de navegação inferior? → A: Não. A paciente tem uma raiz (Meus pedidos) e percursos lineares com Voltar; o administrador tem uma única tela.

## User Scenarios & Testing *(mandatory)*

### Mapa do fluxo

```text
Login (E1.1) ──paciente──▶ Meus pedidos (E1.2) ──▶ Pedido (E1.3, central de próximo passo)
     │                         │                         │
     └─administrador─▶ Protocolo e indicadores (E4.2)    ├─▶ Novo pedido / trocar serviço (E2.1)
                               └─▶ Novo pedido (E2.1) ──▶ Documentos (E2.2)
                                                          ├─▶ Anexar: tipo e origem (E2.7) ─foto─▶ Condições da captura (E4.1)
                                                          ├─▶ Recusas (E2.3) · Substituir/Remover/Excluir (E2.4–E2.6)
                                                          └─▶ Ler documentos (E3.1, SIMULADO)
                                                                 └─▶ Conferência, um documento por vez (E3.2, SIMULADO)
                                                                        └─▶ Verificar critérios (E3.8) ─▶ Resultado (E3.3)
                                                                               ├─▶ Detalhe da pendência (E3.4) ─▶ Substituir/Remover ─▶ Ler de novo (E3.5)
                                                                               └─▶ Concluir simulação (E3.7) ─▶ Comprovante (E3.6, SIMULADO)
Transversais: Sessão expirada (E1.4) · Sem permissão (E1.5) · Pedido não encontrado (E1.3) · Conexão ruim (E4.3) · Acessibilidade (E4.4)
```

Ordem do percurso em rascunho, segundo o backend: anexar (só em rascunho) → ler (simulado, não muda o estado) → conferir (não muda o estado) → verificar critérios (primeira verificação tira o pedido de rascunho). Depois disso, só é possível substituir ou remover documentos, e qualquer troca faz o resultado atual deixar de valer até nova leitura, conferência e verificação.

### User Story 1 - Entrar e retomar o próximo passo (Priority: P1) · RC-36

A paciente entra com e-mail e senha, vê seus pedidos e abre um deles. A tela do pedido diz em que etapa ele está e oferece uma única ação principal para seguir.

**Why this priority**: É a porta de entrada e o ponto de retomada de todo o percurso; sem ela nenhuma outra tela é alcançável.

**Independent Test**: Com dados sintéticos de pedidos nos sete estados, uma pessoa que não desenhou abre cada pedido e diz qual é o próximo passo apenas lendo a tela.

**Acceptance Scenarios**:

1. **Given** a tela de login, **When** a paciente informa credenciais válidas, **Then** vai para Meus pedidos com saudação pelo nome fornecido pela sessão.
2. **Given** credenciais inválidas, **When** tenta entrar, **Then** vê "E-mail ou senha incorretos", o e-mail permanece e a senha é limpa.
3. **Given** um perfil de administrador, **When** entra, **Then** vai direto para Protocolo e indicadores e não vê pedidos.
4. **Given** Meus pedidos com pedidos, **When** a lista carrega, **Then** cada item mostra número do pedido, serviço, estado (ícone + texto) e "Atualizado em", do mais recente ao mais antigo, e no máximo um pedido aparece em destaque.
5. **Given** um pedido em qualquer um dos sete estados, **When** a paciente o abre, **Then** vê a mensagem do estado e exatamente uma ação principal compatível com o backend (tabela em FR-012).
6. **Given** a sessão expirou, **When** qualquer tela recebe a recusa por sessão, **Then** a paciente vê "Sua sessão terminou. Entre novamente." e volta ao login.

---

### User Story 2 - Montar o pedido e anexar documentos (Priority: P1) · RC-37

A paciente escolhe o serviço, cria o rascunho e anexa até três documentos, indicando o tipo de cada um. Pode abrir, substituir e remover documentos e excluir o rascunho.

**Why this priority**: Sem documentos não há leitura, conferência nem verificação.

**Independent Test**: Criar um rascunho, anexar dois arquivos sintéticos (um PDF e uma imagem), ver os dados de cada arquivo, provocar cada recusa e excluir o rascunho com confirmação.

**Acceptance Scenarios**:

1. **Given** Novo pedido, **When** a lista de serviços carrega, **Then** cada serviço mostra especialidade, localidade, versão do protocolo e o aviso de protocolo fictício fornecido pelo sistema.
2. **Given** um rascunho com menos de 3 documentos, **When** a paciente toca em Anexar, **Then** escolhe primeiro o tipo (Encaminhamento, Identificação ou cartão do SUS, Autorização do responsável, Outro) e depois a origem (Fotografar ou Escolher arquivo).
3. **Given** um envio em andamento, **When** aguarda, **Then** a linha mostra "Enviando…", a ação fica bloqueada e "Enviado" só aparece depois da confirmação do sistema.
4. **Given** um arquivo recusado, **When** o sistema responde, **Then** a tela mostra a orientação daquela recusa e diz se é preciso escolher o arquivo de novo.
5. **Given** 3 documentos, **When** a paciente vê a lista, **Then** Anexar não aparece e a tela informa o limite.
6. **Given** um rascunho, **When** a paciente pede para excluí-lo, **Then** um diálogo nomeia o pedido e a quantidade de documentos que serão apagados, com "Cancelar" em destaque.

---

### User Story 3 - Ler e conferir a transcrição simulada (Priority: P1) · RC-38

A paciente pede a leitura simulada dos documentos e confere, documento por documento, o que foi lido: confirma, aponta diferença ou indica que uma informação existe e não foi lida. Não digita nem corrige valores.

**Why this priority**: A verificação dos critérios só usa campos confirmados; sem conferência o resultado tende a "Confira a leitura".

**Independent Test**: Com uma leitura sintética que tenha campos identificados, não localizados e ilegíveis e um documento não lido, registrar decisões em cada documento e ver a situação da conferência mudar.

**Acceptance Scenarios**:

1. **Given** um pedido com documentos, **When** a paciente toca em "Ler documentos", **Then** vê o estado de leitura com o selo SIMULADO e, ao concluir, a conferência do primeiro documento.
2. **Given** um campo identificado, **When** a paciente o vê, **Then** vê valor, origem (página e trecho, quando houver) e pode escolher a pergunta "Confere com o seu documento?" com "Está correto" ou "Está diferente".
3. **Given** um campo não localizado, **When** a paciente o vê, **Then** pode apenas indicar "Esta informação está no documento" e lê a orientação do sistema; não há como preencher o valor.
4. **Given** um campo ilegível ou um documento não lido, **When** a paciente o vê, **Then** não há decisão possível e a tela oferece substituir o arquivo, com a orientação fornecida.
5. **Given** decisões em um documento, **When** a paciente toca em "Salvar e continuar", **Then** todas as decisões daquele documento são salvas juntas ou nenhuma é, e a tela avança para o próximo documento.
6. **Given** que os documentos mudaram depois da leitura, **When** a paciente abre a conferência, **Then** ela fica somente para consulta, com "Os documentos mudaram; leia os documentos de novo." e a ação de ler de novo.

---

### User Story 4 - Verificar critérios e entender as pendências (Priority: P1) · RC-38

A paciente pede a verificação dos critérios, acompanha o processamento e vê o resultado geral, o resumo simulado e a lista de pendências. Em cada pendência entende o que falta, onde foi observado e quem pode resolver.

**Why this priority**: É o valor central do produto: dizer o que corrigir e quem resolve, sem diagnóstico nem promessa.

**Independent Test**: Para os cinco casos sintéticos do protocolo, conferir que a tela de resultado mostra o resultado geral esperado, as pendências com responsável e os critérios atendidos e não aplicáveis.

**Acceptance Scenarios**:

1. **Given** um pedido em rascunho prestes a ser verificado pela primeira vez, **When** a paciente toca em "Verificar critérios", **Then** é avisada de que depois disso não poderá anexar novos documentos, apenas substituir ou remover os enviados.
2. **Given** campos identificados sem decisão, **When** a paciente vai verificar, **Then** a tela mostra quantos são e que contarão como não verificados, sem impedir a verificação.
3. **Given** a verificação em andamento, **When** a paciente está no pedido, **Then** vê "Em verificação", a explicação de que isso não é um resultado e a ação "Atualizar".
4. **Given** um resultado, **When** a paciente o abre, **Then** vê o resultado geral, a revisão e a versão do protocolo avaliadas, o resumo SIMULADO, as pendências e os demais critérios com seu resultado.
5. **Given** uma pendência, **When** a paciente a abre, **Then** vê o critério, a explicação, a origem (documento, página, trecho em texto puro), o responsável (Paciente ou Unidade emissora) e somente as ações que o backend permite naquele estado.
6. **Given** resultado "Sem pendências identificadas", **When** a paciente o vê, **Then** a tela diz que isso não é aprovação do SUS e que os critérios são demonstrativos.
7. **Given** falha na verificação, **When** a paciente vê o pedido, **Then** a tela diz que nenhuma conclusão foi tirada e oferece "Tentar de novo".

---

### User Story 5 - Corrigir e verificar de novo (Priority: P2) · RC-38

Depois do resultado, a paciente substitui ou remove um documento, lê de novo, confere e verifica de novo. A tela deixa claro que o resultado anterior deixou de valer e só marca pendência como resolvida depois do novo resultado.

**Why this priority**: Fecha o ciclo de correção, mas depende de as histórias de análise e reenvio estarem publicadas.

**Independent Test**: Substituir o encaminhamento do caso sintético 2 e percorrer leitura, conferência e verificação até o novo resultado, conferindo a comparação com o anterior.

**Acceptance Scenarios**:

1. **Given** um pedido verificado, **When** a paciente substitui ou remove um documento, **Then** a confirmação avisa que o resultado atual deixará de valer.
2. **Given** um resultado que deixou de valer, **When** a paciente abre o pedido, **Then** vê "Os documentos mudaram. O resultado anterior não vale mais." e a ação "Ler documentos de novo".
3. **Given** um novo resultado e um anterior disponíveis, **When** a paciente abre o resultado, **Then** vê pendências resolvidas, que continuam e novas; antes do novo resultado nada aparece como resolvido.
4. **Given** uma pendência de documento ausente depois da primeira verificação, **When** a paciente abre o detalhe, **Then** a tela explica que o app só permite trocar documentos já enviados e oferece substituir um deles.

---

### User Story 6 - Concluir a simulação e ver o comprovante (Priority: P2) · RC-38

Com o pedido sem pendências identificadas, a paciente confirma a conclusão da simulação e recebe o comprovante simulado. Repetir a operação mostra o mesmo comprovante.

**Why this priority**: É o fim do percurso, mas só acontece depois de todos os passos P1.

**Independent Test**: A partir do caso sintético 1, concluir a simulação, voltar e concluir de novo, e ver o mesmo comprovante.

**Acceptance Scenarios**:

1. **Given** "Sem pendências identificadas", **When** a paciente toca em "Concluir simulação", **Then** um diálogo diz que nada será enviado ao SUS e que o pedido passará a ser só consulta.
2. **Given** a conclusão confirmada, **When** o sistema responde, **Then** a paciente vê o comprovante com o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", referência, data e hora, serviço e versão analisada.
3. **Given** um pedido já concluído, **When** a paciente o abre, **Then** a ação principal é "Ver comprovante" e documentos, leitura e conferência ficam só para consulta.

---

### User Story 7 - Ver as condições da captura ao fotografar (Priority: P3) · RC-39

Ao anexar por foto, a paciente vê a luminosidade e a distância medidas pelos sensores simulados e orientações para melhorar a captura. Sensores indisponíveis nunca a impedem de seguir.

**Why this priority**: Requisito da disciplina (IoT simulada), mas fora do caminho crítico de correção documental.

**Independent Test**: Com leituras sintéticas normais, fora da faixa, antigas e ausentes, conferir mensagem, orientação, idade da leitura e que "Fotografar" continua disponível.

**Acceptance Scenarios**:

1. **Given** leituras recentes, **When** a tela abre, **Then** mostra luminosidade em lux e distância em cm, a classificação fornecida pelo sistema e há quanto tempo foi a leitura, com o selo SIMULADO.
2. **Given** uma leitura fora da faixa, **When** exibida, **Then** a tela orienta melhorar a iluminação ou ajustar a distância; quando a leitura volta à faixa, o aviso muda.
3. **Given** leituras antigas ou indisponíveis, **When** exibidas, **Then** a tela identifica a leitura como desatualizada ou indisponível e mantém "Fotografar mesmo assim".

---

### User Story 8 - Consultar protocolo e indicadores como administrador (Priority: P3) · RC-39

O administrador entra e vê o protocolo ativo com suas regras e totais agregados, sem acesso a pedidos ou documentos individuais.

**Why this priority**: Exigência de perfil administrativo, independente do percurso da paciente.

**Independent Test**: Entrar como administrador, ver a tela de indicadores e confirmar que nenhuma tela da paciente é alcançável.

**Acceptance Scenarios**:

1. **Given** o administrador autenticado, **When** a tela carrega, **Then** mostra o protocolo ativo (identificador, versão, regras com código e nome), total de pedidos, análises por resultado e regras com mais pendências, com a nota de dados agregados de demonstração.
2. **Given** uma paciente, **When** tenta uma tela administrativa, **Then** vê Sem permissão.

---

### Edge Cases

- **Pedido de outra paciente ou inexistente**: mesma tela "Pedido não encontrado" (o sistema responde igual nos dois casos).
- **Leitura desatualizada** (documento enviado, substituído ou removido depois da leitura): conferência só consulta; a ação é ler de novo.
- **Documentos mudam durante a leitura ou a verificação**: a tela mostra "Os documentos mudaram; tente de novo." e não apresenta resultado parcial.
- **Leitura simulada indisponível**: "A leitura simulada não está disponível agora. Tente de novo."; nada é dado como lido.
- **Ler sem documentos**: "Anexe ao menos um documento antes de ler."
- **Resumo simulado indisponível**: o resultado por critério continua visível; o resumo mostra texto padrão identificado e "Tentar de novo".
- **Repetir envio do mesmo arquivo** depois de falha de rede: a tela não duplica o documento; se o sistema indicar tentativa reaproveitada com outro conteúdo, orienta escolher o arquivo de novo.
- **Pedido já concluído**: toda tela de alteração vira consulta; tentativas recusadas mostram "Este pedido já foi submetido e só pode ser consultado."
- **Excluir pedido que já saiu de rascunho**: a ação não aparece; se recusada, "Somente rascunhos podem ser excluídos."
- **Nenhum serviço ativo**: Novo pedido mostra "Nenhum serviço disponível agora." sem ação de criar.
- **Texto de documento ou de resumo com `<`, `>` ou `&`**: exibido como texto puro.
- **Nome de arquivo longo**: truncado na linha; nome completo ao abrir.
- **Conexão perdida no meio de uma ação**: o que foi digitado permanece; a tela diz se o arquivo precisa ser escolhido de novo; envio não é dado como concluído.
- **Fonte ampliada (200%)**: nenhum conteúdo essencial cortado nas telas representativas (E1.2, E2.2, E3.3).

## Requirements *(mandatory)*

### Functional Requirements — regras gerais

- **FR-001**: Toda ação desenhada MUST corresponder a uma operação existente ou contratada no backend; a tela MUST NOT oferecer ação que o backend recusa no estado em questão.
- **FR-002**: Toda informação exibida MUST vir de um dado fornecido pelo sistema ou de um texto fixo de `mobile/design/textos.md`; nenhum valor é inventado ou inferido pela tela (por exemplo, percentual de envio, código de pedido, prazo). Nos quadros, os valores desses campos são exemplares fictícios e realistas, coerentes com o tipo e o estado devolvidos pelo backend (`data-model.md` §5).
- **FR-003**: Toda tela MUST trazer o aviso acadêmico no rodapé; leitura, conferência, resumo, comprovante e sensores MUST trazer o selo SIMULADO.
- **FR-004**: Estados MUST aparecer como ícone + texto, no padrão de F4 do design system v1.0; nunca só por cor.
- **FR-005**: Cada tela MUST ter no máximo uma ação principal; ações de apoio usam contorno ou texto.
- **FR-006**: As telas MUST ser montadas só com componentes do design system v1.0 (`design/RegulaCheck.pen`, F2–F4).
- **FR-007**: O pedido MUST ser identificado como "Pedido {número}", usando o identificador fornecido pelo sistema; datas no fuso de Salvador, no formato "12/11/2026 às 10:06".
- **FR-008**: Linguagem MUST NOT afirmar diagnóstico, urgência, autorização, aprovação do SUS, vaga, prazo ou redução de fila; negações nos avisos são obrigatórias.
- **FR-009**: Texto vindo de documento, leitura ou resumo MUST ser exibido como texto puro.
- **FR-010**: Recusa por sessão MUST levar a Sessão expirada (E1.4) e ao login; recusa por perfil a Sem permissão (E1.5); recurso inexistente ou alheio a "não encontrado".
- **FR-011**: Navegação MUST NOT ter barra inferior; a raiz da paciente é Meus pedidos (com "Sair") e a do administrador é Protocolo e indicadores (com "Sair").
- **FR-012**: A ação principal de um pedido (destaque de E1.2 e E1.3) MUST seguir o estado e a situação fornecidos pelo backend:

| Estado do pedido | Condição exibida | Ação principal |
|---|---|---|
| Rascunho | sem documentos | Anexar documentos (E2.2) |
| Rascunho | com documentos e sem leitura atual | Ler documentos (E3.1) |
| Rascunho | leitura atual com campos sem decisão | Conferir leitura (E3.2) |
| Rascunho | leitura atual conferida | Verificar critérios (diálogo E3.8, FR-040) |
| Em verificação | verificação em andamento | Atualizar |
| Confira a leitura | resultado vigente | Ver o que conferir (E3.3) |
| Com pendências | resultado vigente | Ver pendências (E3.3) |
| Sem pendências identificadas | resultado vigente | Concluir simulação (E3.7) |
| Falha na verificação | — | Tentar de novo |
| Qualquer estado verificado | documentos mudaram, resultado não vigente | Ler documentos de novo (E3.1) |
| Simulação concluída | — | Ver comprovante (E3.6) |

### Functional Requirements — telas e informação

**E1.1 Login**
- **FR-020**: Mostrar campos E-mail e Senha, ação "Entrar" e o aviso acadêmico completo de entrada. Estados: padrão, enviando (ação bloqueada), credenciais inválidas (e-mail mantido, senha limpa), serviço indisponível, sem conexão. Sem cadastro nem recuperação de senha.
- **FR-021**: Depois de entrar, perfil paciente vai a E1.2 e perfil administrador a E4.2.

**E1.2 Meus pedidos**
- **FR-022**: Mostrar saudação com o nome fornecido pela sessão, título e a lista de pedidos próprios (até 50, do mais recente ao mais antigo). Cada item: "Pedido {número}", especialidade, estado (ícone + rótulo) e "Atualizado em".
- **FR-023**: O pedido mais recente em estado que pede ação da paciente (rascunho, confira a leitura, com pendências, sem pendências identificadas, falha na verificação) aparece em destaque com sua ação principal; os demais em lista simples.
- **FR-024**: Ações: "Novo pedido" (apoio) e "Sair". Estados: carregando, vazia (texto e "Novo pedido"), sucesso, erro com "Tentar de novo", sem conexão.

**E1.3 Pedido (central de próximo passo)**
- **FR-025**: Mostrar "Pedido {número}", especialidade e localidade, protocolo (versão, identificado como fictício), estado com sua mensagem, revisão atual, criado em e atualizado em.
- **FR-026**: Mostrar a superfície "Próximo passo" com a ação principal de FR-012 e as seções Documentos ({n} de 3), Leitura e conferência (situação) e Resultado (quando houver), cada uma com acesso à tela correspondente.
- **FR-027**: Em rascunho, o menu "Mais opções" do cabeçalho oferece "Trocar serviço" (E2.1) e "Excluir rascunho" (E2.6); o serviço aparece abaixo do título. Fora de rascunho, o menu não aparece.
- **FR-028**: Estados: carregando, não encontrado, erro, sem conexão.

**E2.1 Novo pedido / trocar serviço**
- **FR-029**: Mostrar os serviços ativos como opções de escolha única: especialidade, localidade, versão do protocolo e o aviso fictício fornecido pelo sistema. Ação principal "Criar rascunho" (ou "Salvar serviço" ao trocar). Estados: carregando, criando, erro, sem serviços. Trocar para o mesmo serviço não muda nada; trocar para outro informa que a revisão do pedido avança.

**E2.2 Documentos do pedido**
- **FR-030**: Mostrar os documentos disponíveis na ordem de envio. Cada linha: tipo documental (título), nome do arquivo, formato · tamanho · páginas (páginas só em PDF), estado do envio e ações "Abrir" e "Mais opções" (Substituir, Remover). Mostrar "{n} de 3" e os limites: PDF até 5 páginas, JPEG ou PNG, 5 MB cada.
- **FR-031**: "Anexar documento" aparece só em rascunho com menos de 3 documentos. Ação principal por situação: sem documentos → "Anexar documento"; com documentos e sem leitura atual → "Ler documentos", com a nota "Leitura simulada. Depois você confere o que foi lido."; com leitura atual → "Conferir leitura"; pedido verificado com resultado vigente → nenhuma ação principal (só ações de apoio por documento); pedido verificado com documentos alterados → "Ler documentos de novo"; pedido concluído → nenhuma.
- **FR-032**: Fora de rascunho e antes da conclusão, a tela não oferece anexar e avisa que trocar ou remover documento faz o resultado atual deixar de valer. Depois da conclusão, a tela é só consulta (Abrir).
- **FR-033**: Estados: vazia, com arquivos, enviando (linha "Enviando…", ações bloqueadas), envio interrompido ("Não enviado" + "Tentar de novo" como única ação, indicando se é preciso escolher o arquivo de novo), limite atingido, pedido verificado, pedido concluído.

**E2.7 Anexar: tipo e origem**
- **FR-034**: Primeiro, escolha única do tipo: Encaminhamento, Identificação ou cartão do SUS, Autorização do responsável, Outro. Depois, a origem: "Fotografar" (passa por E4.1) ou "Escolher arquivo".

**E2.3 Recusas de arquivo**
- **FR-035**: Uma orientação por recusa do backend, com o texto de `textos.md` e a ação seguinte: formato não suportado, extensão diferente do conteúdo, acima de 5 MB, PDF com mais de 5 páginas, PDF protegido por senha, arquivo ilegível, limite de 3 documentos, pedido que não aceita mais documentos.

**E2.4 a E2.6 Confirmações**
- **FR-036**: Substituir (E2.4) e Remover (E2.5) nomeiam o documento e, se o pedido já foi verificado, avisam que o resultado atual deixará de valer. Excluir rascunho (E2.6) nomeia o pedido e a quantidade de documentos apagados, "Cancelar" em destaque, só em rascunho.

**E3.1 Leitura simulada**
- **FR-037**: Mostrar "Lendo seus documentos" com selo SIMULADO e o aviso de leitura simulada fornecido pelo sistema. Estados: lendo, leitura indisponível ("Tentar de novo"), sem documentos, documentos mudaram durante a leitura. Ao concluir, abrir E3.2 no primeiro documento.

**E3.2 Conferência da leitura (um documento por vez)**
- **FR-038**: Mostrar "Documento {i} de {n}", tipo e nome do arquivo, o aviso de leitura simulada e o resumo da conferência (campos não verificados, campos sem decisão, divergências). Os seis campos lidos aparecem com rótulos legíveis: Tipo do documento, Nome, Data de nascimento, Motivo do encaminhamento, Data de emissão, Autorização assinada; primeiro os identificados e ilegíveis, depois os não localizados.
- **FR-039**: Por situação do campo, só as decisões que o backend aceita: identificado → a pergunta "Confere com o seu documento?" com "Está correto" ou "Está diferente"; não localizado → apenas "Esta informação está no documento"; ilegível → nenhuma decisão, com a orientação e "Substituir arquivo". Documento não lido (protegido ou leitura vazia) → motivo, orientação e "Substituir arquivo". Decidir de novo troca a decisão anterior.
- **FR-040**: Ação principal "Salvar e continuar" por documento; no último, "Concluir conferência", que leva ao pedido (E1.3). "Verificar critérios" abre o diálogo **E3.8** (tela nova): na primeira verificação de um rascunho, avisa "Depois de verificar, você não poderá anexar novos documentos, só substituir ou remover os enviados."; se houver campos identificados sem decisão, informa a quantidade e que eles contarão como não verificados, sem bloquear.
- **FR-041**: Leitura desatualizada ou pedido concluído: conferência só consulta, com a mensagem correspondente.

**E3.3 Resultado da verificação**
- **FR-042**: Mostrar resultado geral (ícone + texto), revisão e versão do protocolo avaliadas (e a data da verificação somente se o backend passar a fornecê-la; o contrato atual não tem esse campo); o resumo SIMULADO (ou o texto padrão de indisponibilidade com "Tentar de novo"); a lista de pendências (critério, tipo pendente ou não verificável, responsável); e os demais critérios com resultado atende ou não se aplica.
- **FR-043**: Em "Sem pendências identificadas", mostrar que não é aprovação do SUS e que os critérios são demonstrativos; ação principal "Concluir simulação". Em falha, "Tentar de novo". A verificação em andamento é mostrada no pedido (E1.3), não no resultado.

**E3.4 Detalhe da pendência**
- **FR-044**: Mostrar critério (código e descrição), versão do protocolo, tipo, explicação fornecida pelo sistema, motivo (quando não verificável), origem (documento, página, trecho em texto puro, situação do campo) e responsável.
- **FR-045**: Ações conforme responsável e motivo, só entre as que o backend permite: sempre "Abrir documento" quando houver documento de origem; unidade emissora → orientação de procurar a unidade + "Já tenho uma nova versão" (substituir); paciente com campo ilegível ou documento não lido → "Substituir arquivo"; paciente com transcrição não conferida ou divergência apontada → "Conferir leitura"; documento ausente depois da primeira verificação → explicação da limitação + "Substituir um documento".

**E3.5 Correção e comparação**
- **FR-046**: Depois de qualquer troca, o pedido e o resultado mostram que o resultado anterior não vale mais e oferecem "Ler documentos de novo"; nenhuma pendência é marcada como resolvida antes do novo resultado.
- **FR-047**: Com um resultado anterior disponível, o resultado mostra a comparação: resolvidas, continuam, novas.

**E3.7 Confirmar conclusão da simulação** (tela nova)
- **FR-048**: Diálogo: "Concluir a simulação?", explicando que nada será enviado ao SUS e que o pedido passará a ser só consulta. Ações: "Concluir simulação" (principal) e "Voltar".

**E3.6 Comprovante simulado**
- **FR-049**: Mostrar o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS", referência, data e hora, serviço e versão analisada (revisão e protocolo), e a nota de demonstração. Repetir a conclusão mostra o mesmo comprovante. Download fica fora (RC-34, melhoria P1).

**E4.1 Condições da captura**
- **FR-050**: Mostrar luminosidade (lux) e distância (cm), a classificação e a orientação fornecidas pelo sistema, a idade da leitura e a nota "Valores de demonstração. Eles não comprovam que o documento ficará legível."; as leituras se atualizam enquanto a tela está aberta (a frequência é da história de sensores e não é desenhada; o quadro só anota o comportamento). Ação principal "Fotografar"; com sensores indisponíveis ou leitura antiga, "Fotografar mesmo assim". Estados: normal, luminosidade fora, distância fora, recuperada, desatualizada, indisponível.

**E4.2 Protocolo e indicadores (administrador)**
- **FR-051**: Mostrar protocolo ativo (identificador, versão, número de regras e lista código + nome), total de pedidos, análises por resultado e regras com mais pendências, com a nota de dados agregados; ação "Sair". Estados: carregando, erro, sem permissão (paciente).

**E4.3 Conexão ruim e E4.4 Acessibilidade**
- **FR-052**: Tempo esgotado e sem conexão preservam o que foi digitado, informam se o arquivo precisa ser escolhido de novo e oferecem "Tentar novamente".
- **FR-053**: Anotações de acessibilidade (contraste, alvos de 48, ordem de leitura e rótulos) e versões com fonte ampliada de E1.2, E2.2 e E3.3.

### Key Entities *(include if feature involves data)*

- **Pedido**: número, serviço (especialidade, localidade, protocolo e versão), estado (7), revisão, criado em, atualizado em, documentos disponíveis, situação da análise da versão atual.
- **Documento**: tipo documental (4), nome do arquivo, formato, tamanho, páginas (PDF), estado do envio, enviado em.
- **Leitura simulada**: situação (atual ou desatualizada), aviso de simulação, documentos lidos (lido, protegido, leitura vazia) com seis campos cada (identificado com valor e origem, não localizado, ilegível).
- **Conferência**: situação (pendente, confira a leitura, conferida, desatualizada), resumo de contagens, decisão por campo (confirmada ou divergente com motivo), orientações.
- **Resultado da verificação**: resultado geral, revisão e protocolo avaliados, resultado por critério (6), pendências (critério, tipo, explicação, motivo, origem, responsável), resumo simulado.
- **Comprovante simulado**: referência, data e hora, serviço, versão analisada.
- **Leitura de sensor**: luminosidade (lux), distância (cm), instante, classificação.
- **Indicadores**: protocolo ativo e regras, totais agregados.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das ações desenhadas aparecem na tabela tela × operação do plano, ligadas a uma operação existente ou contratada; 0 ações sem correspondência.
- **SC-002**: 100% das informações exibidas têm origem rastreável (dado do sistema ou texto fixo); 0 valores inventados.
- **SC-003**: Uma pessoa que não desenhou identifica corretamente a ação principal em pelo menos 10 das 11 situações da tabela FR-012, só lendo a tela.
- **SC-004**: Uma pessoa que não desenhou percorre no protótipo o caminho login → comprovante do caso sintético 1 em até 5 minutos, sem ajuda.
- **SC-005**: Os 7 estados do pedido, as 3 situações de campo e as 4 decisões possíveis aparecem desenhados em pelo menos um quadro cada.
- **SC-006**: 0 afirmações proibidas e 100% das telas com aviso acadêmico; 100% das telas de leitura, conferência, resumo, comprovante e sensores com selo SIMULADO.
- **SC-007**: 0 instâncias de componentes `Legado/*` nos quadros desta feature.

## Assumptions

- **Escopo**: somente design no pen.dev e documentação; nenhum código do aplicativo ou do backend muda. O inventário fechado no plano tem 94 quadros (`contracts/inventario-quadros.md`), ante os 73 do épico 007.
- **Prioridade de entrega**: P1 (US1–US4) até o marco de 16/11 do percurso principal; P2 e P3 depois, conforme o cronograma do `CLAUDE.md`.
- **Backend como fonte**: implementado em `main` — login e sessão (RC-04), serviços e rascunho (RC-07), documentos (RC-08, RC-09), leitura simulada (RC-10), conferência (RC-11), motor de regras interno (RC-12). Contratado e ainda não implementado — análises e histórico (RC-13), resumo simulado (RC-14), resultado e reenvio (RC-15, RC-16), submissão simulada (RC-17), indicadores (RC-20), sensores (RC-21, RC-22). Telas destas últimas seguem `CONTRATO_API.md` e o contrato interno da RC-12 e são marcadas "contrato provisório" até a publicação.
- **Base**: branch `codex/007-design-system-exploracao` atualizada para `origin/main` em 13/11/2026 (merge do PR #13, `0091634`), com as specs 008–013.
- **Dados de exemplo — estrutura do backend, texto exemplar**: os quadros seguem a estrutura da carga `database/scripts/02_carga_sintetica.sql` (Pedidos 1, 2 e 4 da paciente; Pedido 3 de outra paciente, usado no "não encontrado") e as situações dos catálogos da leitura simulada (principal e de testes); estados que a carga não tem usam os Pedidos 5 a 8. Os valores exibidos (nomes, arquivos, trechos, resumos, localidade) são exemplares realistas em português correto, nunca os literais de teste. Mapa completo em `data-model.md` §5.
- **Classificação dos sensores**: a faixa e a orientação vêm do sistema; se a história de sensores não fornecer classificação, a tela mostra só o valor e a idade da leitura.
- **Estados do arquivo**: "Enviando" e "Não enviado" são estados da tela durante e depois de uma tentativa; o sistema só registra documentos enviados. O componente F4 "Aguardando envio" não é usado nesta feature.
- **Limitação registrada para a RC-16 (não é decisão de design)**: depois da primeira verificação o sistema recusa documento novo; uma pendência de documento ausente só pode ser corrigida substituindo um documento enviado. A tela explica isso; mudar o comportamento cabe ao dono da história de reenvio.
- **Numeração**: `014` é o próximo número livre em `origin/main`; se a história RC-13 publicar outra feature `014`, renumerar esta sem alterar conteúdo.
