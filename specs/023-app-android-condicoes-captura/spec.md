# Feature Specification: App Android — condições da captura com os sensores simulados (RC-22, parte do app)

**Feature Branch**: `023-app-android-condicoes-captura`

**Created**: 18/11/2026

**Status**: Implementada e verificada em 18/11/2026 (112 testes; percurso no emulador com backend real e simulador; ver tasks.md)

**Input**: Issue [RC-22 #47](https://github.com/Code-Masters-F/RegulaCheck/issues/47) (Épico 7), parte do app. A tela é dependência do Épico 6 ([#40](https://github.com/Code-Masters-F/RegulaCheck/issues/40)). O backend e o simulador já foram entregues na [spec 016](../016-iot-simulada/spec.md) (PR #68), cujo texto diz que "a tela do app não faz parte desta entrega": esta feature é essa tela. Base do app: [spec 017](../017-app-android-login-pedidos/spec.md) (RC-18) e [spec 022](../022-app-android-documentos-resultado/spec.md) (RC-19).

Os critérios de aceite da #47 são a fonte da verdade. As regras de telemetria (faixas demonstrativas, prazo de 60 s, situações e códigos de orientação) são as da spec 016 e do contrato [api-iot.md](../016-iot-simulada/contracts/api-iot.md); esta spec não as copia nem as altera. Referência visual: `mobile/design/INDEX.md`, "06 · E2.7 Anexar" (Origem `ncnpi`) e "07 · E4.1 Condições da captura" (`o3kkGI`, `fOrmB`, `NyDDm`, `DGf2T`, `Cna25`, `kaHJz`). Os textos exatos estão em `mobile/design/textos.md`.

## Contexto e limite

A spec 016 tornou funcional a estação de captura simulada. A paciente abre uma sessão de captura do próprio pedido, o simulador da equipe envia leituras de luminosidade (lux) e distância (cm) e a API devolve, para cada sensor, a última leitura, a situação e uma orientação. Falta a tela do app que mostra isso à paciente **antes** de ela anexar uma foto do documento.

O app desta versão **não usa a câmera** (fora do escopo da #40/#42). O caminho "Fotografar" do desenho é adaptado assim: a paciente vê as condições da captura (E4.1), fotografa o documento com a câmera do próprio aparelho e escolhe a foto no mesmo seletor do sistema da RC-19, com o tipo do documento já escolhido. Os textos que prometiam abrir a câmera foram ajustados (ver FR-013).

**Fica fora:** câmera dentro do app, qualquer alteração no backend ou no simulador, tela de administrador, encerramento de sessão de captura, qualquer conclusão sobre a legibilidade do arquivo e qualquer bloqueio de envio por causa dos sensores.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver as condições da captura antes de anexar uma foto (Priority: P1)

Na folha "Anexar documento", depois de escolher o tipo, a paciente escolhe a origem. Com "Escolher arquivo", vai direto ao seletor do sistema, como já acontece na RC-19. Com "Fotografar", abre a tela "Condições da captura", que mostra os dois sensores simulados: luminosidade e distância, com valor e unidade, situação em texto e ícone, orientação e a idade da leitura. A tela também mostra a nota de demonstração e o selo SIMULADO. Pela ação principal, ela segue para escolher a foto no seletor do sistema, e o envio continua o mesmo da RC-19.

**Why this priority**: este é o critério 1 da #47 (o app consulta a telemetria da sessão da própria paciente e exibe os dois sensores) e a dependência que o Épico 6 tinha deixado aberta.

**Independent Test**: com o backend e uma sessão recebendo a sequência `normal` do simulador, abrir um pedido em rascunho, tocar em "Anexar documento" → tipo → "Fotografar" e conferir os dois sensores "Adequada", com valor, unidade e "Leitura há {tempo} · sensor simulado". Depois, seguir para o seletor, escolher uma imagem e ver o documento entrar na lista só após a confirmação.

**Acceptance Scenarios**:

1. **Given** a folha aberta com o tipo escolhido, **When** a paciente toca em "Escolher arquivo", **Then** o seletor do sistema abre com PDF, JPEG e PNG, como na RC-19.
2. **Given** a folha aberta com o tipo escolhido, **When** ela toca em "Fotografar", **Then** a tela "Condições da captura" abre, cria ou reaproveita a sessão de captura do pedido e consulta a telemetria.
3. **Given** leituras recentes dentro da faixa, **When** a telemetria chega, **Then** cada sensor mostra nome, valor com unidade ("410 lux", "31,5 cm"), a situação "Adequada" com ícone e "Leitura há {tempo} · sensor simulado".
4. **Given** a tela aberta, **When** a paciente toca na ação principal, **Then** o seletor do sistema abre com o tipo já escolhido. Depois, o envio, as recusas e "Tentar de novo" se comportam como na RC-19.
5. **Given** a tela aberta, **When** a paciente toca em "Escolher arquivo" ou em voltar, **Then** ela vai ao seletor ou volta aos documentos, sem perder o que já estava na lista.

---

### User Story 2 - Orientação muda com as leituras (Priority: P1)

Enquanto a tela está visível, ela se atualiza sozinha a cada poucos segundos. Quando um sensor sai da faixa, a situação muda ("Baixa", "Alta", "Fora do ideal") e aparece a orientação devolvida pela API, como "Ilumine melhor o documento" ou "Afaste o celular". Quando o sensor volta à faixa, a tela mostra "Adequada de novo", "Condições adequadas de novo." e o valor anterior.

**Why this priority**: este é o critério 2 da #47 (fora da faixa gera a orientação; a recuperação altera o aviso).

**Independent Test**: com a tela aberta, o usuário roda a sequência `fora-da-faixa` e depois a `recuperacao`. A tela deve mostrar primeiro as orientações e depois a recuperação, sem que a paciente toque em nada.

**Acceptance Scenarios**:

1. **Given** a luminosidade abaixo da faixa, **When** a tela se atualiza, **Then** a luminosidade mostra "Baixa", com ícone e a orientação da API como texto puro, e a distância mantém a própria situação.
2. **Given** a distância fora da faixa, **When** a tela se atualiza, **Then** a distância mostra "Fora do ideal", com a orientação da API (aproximar ou afastar).
3. **Given** um sensor fora da faixa durante esta visita à tela, **When** uma leitura posterior o coloca dentro da faixa, **Then** ele mostra "Adequada de novo", "Condições adequadas de novo." e "antes: {valor} {unidade}". O anúncio também chega ao leitor de tela.
4. **Given** uma consulta ainda em andamento, **When** chega a hora da próxima atualização, **Then** nenhuma outra consulta começa: elas nunca se sobrepõem.
5. **Given** a paciente sai da tela (voltar, seletor aberto, app em segundo plano), **When** o tempo passa, **Then** nenhuma consulta é feita até a tela voltar a ficar visível.

---

### User Story 3 - Leitura antiga, sensores indisponíveis e erros não bloqueiam (Priority: P2)

Se a leitura passou do prazo de atualidade, o sensor aparece como "Desatualizada", com "Última leitura há {tempo}". Sem sessão, sem leitura, sem conexão ou com o serviço fora do ar, a tela mostra "Sensores indisponíveis agora." e "Você pode anexar e salvar seus documentos normalmente.". Em todos esses estados, a paciente pode seguir para a escolha da foto. Pedido já submetido, sem permissão ou não encontrado recebe uma explicação própria.

**Why this priority**: este é o critério 3 da #47 e a regra do projeto (CLAUDE.md §12: os sensores não bloqueiam o envio).

**Independent Test**: depois de mais de 60 s sem envio do simulador, ver "Desatualizada"; com a rede do emulador cortada, ver "indisponível". Nos dois casos, seguir e anexar um arquivo.

**Acceptance Scenarios**:

1. **Given** a última leitura com mais de 60 s, **When** a tela se atualiza, **Then** o sensor mostra "Desatualizada", com "Última leitura há {tempo} · sensor simulado", e a ação principal passa a "… mesmo assim".
2. **Given** um pedido sem sessão ou sem leituras (os dois sensores `SEM_LEITURA`), **When** a tela consulta, **Then** ela mostra "Sensores indisponíveis agora." e "Você pode anexar e salvar seus documentos normalmente.", com valor "—" e a ação "… mesmo assim".
3. **Given** a rede caída, um tempo esgotado ou a resposta 503, **When** a tela consulta, **Then** ela mostra o estado "indisponível" sem travar, continua tentando a consulta periódica enquanto estiver visível e mantém a ação de seguir.
4. **Given** um pedido já submetido (409 ao abrir a sessão), **When** a tela abre, **Then** ela explica que o pedido não recebe mais documentos e oferece "Voltar".
5. **Given** a resposta 403 ou 404, **When** a tela abre ou se atualiza, **Then** ela mostra "sem permissão" ou "pedido não encontrado", com o caminho de volta. Um 401 leva ao login, como no resto do app.

---

### Edge Cases

- Leitura com instante pouco à frente do relógio do servidor: a idade é calculada pelo `consultadaEm` da própria resposta e nunca é negativa ("há 0 s").
- Só um sensor `SEM_LEITURA` (o outro tem leitura): ele mostra "Sem leitura", "—" e a orientação da API; o outro segue normal.
- Uma situação desconhecida, vinda de uma versão futura da API: aparece como "Sem leitura" e não bloqueia nada.
- Orientação vazia ou ausente: a tela usa um texto local, escolhido pelo código.
- A resposta não traz exatamente os dois sensores: a tela mostra o que vier, por tipo, e trata o sensor ausente como sem leitura.
- Ao girar a tela, a escolha do tipo e o fato de a E4.1 estar aberta são preservados.
- Abrir a E4.1 de novo: a memória de "recuperada" começa vazia e vale só dentro de cada visita.
- 401 durante a atualização periódica: a sessão local é apagada, o app vai ao login e a atualização para.

## Requirements *(mandatory)*

### Functional Requirements

**Folha E2.7 (Origem)**

- **FR-001**: Depois da escolha do tipo, a folha MUST mostrar o passo "Como você quer anexar?", com "Fotografar" e "Escolher arquivo", além de "Cancelar".
- **FR-002**: "Escolher arquivo" MUST abrir o seletor do sistema da RC-19 (PDF, JPEG e PNG), com o tipo escolhido.
- **FR-003**: "Fotografar" MUST abrir a E4.1 do mesmo pedido, levando o tipo escolhido.

**E4.1 Condições da captura**

- **FR-004**: Ao abrir, a E4.1 MUST pedir a sessão de captura do pedido (operação idempotente; 201 ou 200 servem igualmente) e consultar a telemetria do pedido.
- **FR-005**: Enquanto estiver visível, a E4.1 MUST consultar a telemetria em intervalo fixo de 3 a 5 s. Cada consulta começa só depois que a anterior termina, e a atualização MUST parar quando a tela sai de vista. Isso não é retentativa: a consulta é só leitura, e o pedido de sessão não se repete sozinho dentro da mesma visita.
- **FR-006**: Para cada sensor (luminosidade e distância, nesta ordem), a E4.1 MUST mostrar o nome, o valor com unidade em formato pt-BR (ou "—" sem leitura), a situação em texto com ícone (nunca só cor) e a idade da leitura ("Leitura há {tempo} · sensor simulado" ou "Última leitura há {tempo} · sensor simulado").
- **FR-007**: Fora da faixa, desatualizada ou sem leitura, a E4.1 MUST mostrar a `orientacao.mensagem` da API como texto puro. Dentro da faixa, não mostra orientação, como no desenho.
- **FR-008**: A E4.1 MUST mostrar "Adequada de novo", "Condições adequadas de novo." e "antes: {valor} {unidade}" quando um sensor que esteve fora da faixa nesta visita volta para dentro dela. A marca some se ele sair da faixa de novo.
- **FR-009**: Quando os dois sensores estão sem leitura (sem sessão ou sem leituras), ou quando a consulta falha por rede, por tempo esgotado ou por 5xx, a E4.1 MUST mostrar o estado "indisponível" ("Sensores indisponíveis agora." e "Você pode anexar e salvar seus documentos normalmente.").
- **FR-010**: A E4.1 MUST manter disponíveis, em todos os estados de telemetria, a ação principal (seguir para a foto) e "Escolher arquivo". O rótulo da ação principal traz "mesmo assim" quando algum sensor está desatualizado ou sem leitura, ou quando a tela está indisponível.
- **FR-011**: A E4.1 MUST mostrar o selo SIMULADO no cabeçalho e a nota "Valores de demonstração. Eles não comprovam que o documento ficará legível."
- **FR-012**: Erros: 401 leva ao login (mecanismo existente); 403 mostra "sem permissão"; 404 mostra "pedido não encontrado", com "Voltar para meus pedidos"; 409 ao abrir a sessão explica que o pedido não recebe mais documentos e oferece "Voltar". Em todos esses casos, a atualização periódica para.
- **FR-013**: Os textos que prometiam câmera MUST ser ajustados: apoio de "Fotografar" na folha, ação principal e apoio do título da E4.1. Os textos novos ou ajustados MUST constar em `mobile/design/textos.md`.
- **FR-014**: O app MUST NOT registrar em log o token, a senha, a chave do simulador nem o corpo da telemetria ou dos documentos (garantia existente do `ApiClient`, sem interceptor de log).
- **FR-015**: A documentação do app MUST explicar o que cada sensor mede (luminosidade em lux; distância em cm), que os limites são demonstrativos e que as leituras não comprovam a legibilidade do arquivo (critério 4 da #47; o detalhe está na spec 016).

### Key Entities

- **Sessão de captura**: sessão do pedido aberta pela paciente (id, início, aberta, criada). O app não guarda o id além da visita à tela.
- **Telemetria**: resposta de `GET …/leituras` (instante da consulta, prazo de atualidade, aviso e dois sensores). É lida e exibida, sem regras próprias.
- **Apresentação do sensor**: o que a tela mostra de cada sensor (rótulo da situação, ícone, valor formatado, idade, orientação, recuperada e valor anterior). É derivada da telemetria e da consulta anterior da mesma visita.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Nos 6 estados do desenho (normal, luminosidade fora, distância fora, recuperada, desatualizada e indisponível), a E4.1 mostra os textos de `textos.md` e mantém a ação de seguir disponível, em 6 de 6 casos.
- **SC-002**: Com o simulador mudando as leituras, a tela reflete a mudança em até 5 s, sem toque da paciente.
- **SC-003**: Em 100% das observações de teste, há no máximo uma consulta de telemetria em andamento por vez, e nenhuma consulta acontece depois que a tela sai de vista.
- **SC-004**: A paciente anexa um documento depois da E4.1 com sensores dentro e fora da faixa (2 de 2 casos), e o documento só entra na lista após a confirmação da API.
- **SC-005**: Nenhuma linha do logcat do processo do app contém token, senha, chave do simulador ou corpo de documento.

## Assumptions

- O backend e o simulador da spec 016 já estão na `main` e não mudam. O app consome só `POST /solicitacoes/{id}/sessoes-captura` e `GET /solicitacoes/{id}/leituras`.
- O intervalo de atualização é de 4 s, dentro da faixa de 3 a 5 s pedida.
- A idade da leitura usa o relógio do servidor (`consultadaEm − medidaEm`), não o do aparelho.
- "Recuperada" é memória da visita à tela, sem persistência.
- A E4.1 só é oferecida para pedido em rascunho que ainda aceita documento, porque a folha só existe nesse caso. O 409 trata a corrida em que o pedido foi submetido em outro lugar.
- A ação principal abre o seletor filtrado para imagens (JPEG e PNG, porque é uma foto). "Escolher arquivo" abre com PDF, JPEG e PNG. Os dois usam o mesmo seletor do sistema da RC-19.
