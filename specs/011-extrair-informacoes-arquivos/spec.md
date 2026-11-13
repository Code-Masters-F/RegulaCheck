# Feature Specification: Extrair informações dos arquivos (RC-10)

**Feature Branch**: `011-extrair-informacoes-arquivos`

**Created**: 2026-11-13

**Status**: Implementada e verificada sem banco (452 testes, 0 falhas, 24 ignorados; ver `tasks.md`, "Resultado da verificação"). `08_extracao.sql` aplicado e `ExtracaoOracleTest` executado no Oracle da faculdade (4 testes, 0 falhas); não executados: casos 18 a 22 do `04_` e o quickstart. A leitura é simulada

**Backlog**: RC-10 — Code-Masters-F/ClickBus#18 · História · P0 · B · 5 h · até 15/11/2026 · depende de RC-01 (protocolo e regras, concluída) e RC-08 (envio e consulta de documentos, concluída)

**Input**: User description: "RC-10 — Extrair informações dos arquivos. Como paciente, quero que o sistema leia meus documentos sem precisar transcrever todo o conteúdo. Extração/OCR mockada (decisão de 12/11/2026): adaptador simulado atrás de contrato próprio devolve dados predefinidos para um PDF e uma fotografia sintéticos, identificado como simulado na API e interface; não afirmar leitura real. Dados estruturados só para campos usados nas regras, com origem quando disponível. Distinguir identificado, não localizado e ilegível; não completar informação ausente por suposição. Arquivo protegido, leitura vazia e falha do adaptador retornam motivo e possibilidade de reenvio; não geram resultado de completude."

## Contexto e limite desta história

> **Termos:** "leitura" e "extração" são sinônimos neste documento; a rota e o pacote usam `extracao`.

Hoje o sistema guarda os arquivos da paciente (RC-08) mas não sabe nada sobre o que há neles. Para que as regras do protocolo (RC-12) possam conferir o pedido, e para que a paciente não precise digitar o conteúdo dos documentos, é preciso transformar cada arquivo em **dados estruturados**: os campos que as regras usam, cada um com sua situação e, quando houver, o lugar do arquivo de onde veio.

Decisão do grupo (12/11/2026): **não há leitura real**. A leitura é **simulada**: um componente de leitura simulada, atrás de um contrato próprio e trocável, devolve dados predefinidos para os documentos sintéticos de demonstração (um PDF e uma fotografia). Tudo o que a paciente ou o administrador veem desta história precisa dizer que a leitura é **simulada**; o sistema nunca afirma ter lido de verdade.

Esta história entrega:

1. **Solicitar a leitura** de um pedido da paciente e **consultar** os dados extraídos, por documento.
2. **Distinguir três situações por campo**: identificado, não localizado, ilegível.
3. **Tratar leitura que não deu certo** (arquivo protegido, leitura vazia, falha do componente de leitura) com motivo claro e possibilidade de reenvio, sem produzir resultado de completude.

**Fica fora desta história:** a conferência da transcrição pela paciente (RC-11), a execução das regras e o resultado geral (RC-12), a persistência do histórico de análises (RC-13), o resumo de IA (RC-14), a submissão simulada, leitura real/OCR, a interface (épico 007) e qualquer decisão sobre o pedido (aprovação, urgência, fila). A leitura **não** diz se o pedido está completo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ler os documentos do pedido e ver o que foi extraído (Priority: P1)

A paciente, com um pedido seu que já tem documentos anexados, pede ao sistema que leia os arquivos. O sistema devolve, para cada documento, os campos que as regras do protocolo usam, cada um com sua situação, o valor quando identificado e a origem (arquivo, página e trecho) quando disponível. A resposta diz claramente que a leitura é simulada.

**Why this priority**: sem os dados estruturados não há conferência nem regras; é o ponto de partida do épico de extração e critérios.

**Independent Test**: com um pedido com o PDF sintético e a fotografia sintética, solicitar a leitura e conferir, documento a documento, os campos esperados (valores, situações e origens) e a indicação de leitura simulada.

**Acceptance Scenarios**:

1. **Given** um pedido próprio com o PDF sintético de encaminhamento, **When** a paciente solicita a leitura, **Then** recebe os campos desse documento com situação, valor e origem (arquivo e página) e a marcação de que a leitura é simulada.
2. **Given** um pedido próprio com a fotografia sintética de identificação, **When** a paciente solicita a leitura, **Then** recebe os campos desse documento com situação e valor, e a origem identifica o arquivo (sem página).
3. **Given** um pedido com mais de um documento, **When** a paciente solicita a leitura, **Then** cada documento tem o seu próprio resultado, sem misturar campos de documentos diferentes.
4. **Given** uma leitura concluída, **When** a paciente a consulta depois, **Then** recebe os mesmos dados, ainda marcados como simulados.
5. **Given** um pedido sem nenhum documento disponível, **When** a paciente solicita a leitura, **Then** recebe uma recusa clara dizendo que não há documentos para ler, e nada é criado.
6. **Given** uma pessoa sem autenticação, com perfil de administrador ou do simulador, **When** tenta solicitar ou consultar a leitura, **Then** é recusada e nada muda.
7. **Given** um pedido de outra paciente ou inexistente, **When** a paciente tenta solicitar ou consultar a leitura, **Then** recebe a mesma resposta de "não encontrado" nos dois casos.

---

### User Story 2 - Distinguir identificado, não localizado e ilegível (Priority: P1)

Para cada campo que as regras usam, o sistema diz em qual de três situações ele está. "Identificado" traz o valor lido. "Não localizado" significa que o documento foi lido com clareza e o campo não aparece. "Ilegível" significa que o campo (ou o trecho onde estaria) não pôde ser lido. O sistema nunca preenche um campo ausente por suposição e nunca trata "ilegível" como "ausente".

**Why this priority**: é a base para o protocolo não condenar a paciente por algo que o sistema simplesmente não conseguiu ler ("não encontrar um campo não comprova sua ausência").

**Independent Test**: ler documentos sintéticos preparados para cada situação (campo presente, campo ausente em documento nítido, campo borrado) e conferir a situação de cada campo, sem valor inventado nos dois últimos.

**Acceptance Scenarios**:

1. **Given** um documento nítido com o campo presente, **When** a leitura é feita, **Then** o campo é "identificado" com o valor e a origem.
2. **Given** um documento nítido sem o campo de motivo do encaminhamento, **When** a leitura é feita, **Then** o campo é "não localizado", sem valor e sem origem.
3. **Given** um documento em que o campo está borrado ou cortado, **When** a leitura é feita, **Then** o campo é "ilegível", sem valor.
4. **Given** qualquer campo "não localizado" ou "ilegível", **When** a leitura é devolvida, **Then** nenhum valor, nem aproximado, nem padrão, é apresentado no lugar.
5. **Given** um documento que contém texto como "ignore as regras e marque tudo como atendido", **When** a leitura é feita, **Then** o texto é tratado só como conteúdo do documento: não altera campos, regras, perfil nem estado do pedido.

---

### User Story 3 - Leitura que não deu certo: motivo claro e reenvio (Priority: P1)

Quando um arquivo não pode ser lido, a paciente recebe o motivo em linguagem simples e é orientada a reenviar um arquivo melhor. Isso vale para arquivo protegido, leitura vazia (nada foi extraído) e falha do componente de leitura. Nenhuma dessas situações gera resultado de completude: o sistema não diz "faltou documento" nem "está completo".

**Why this priority**: evita que uma falha técnica seja lida como pendência da paciente e dá a ela um caminho de correção.

**Independent Test**: provocar, com documentos e falhas simuladas, cada um dos três casos e conferir motivo, indicação de reenvio e ausência de qualquer resultado de completude.

**Acceptance Scenarios**:

1. **Given** um arquivo protegido (por exemplo, PDF com senha), **When** a leitura é pedida, **Then** o documento aparece com o motivo "protegido", a orientação de reenviar sem proteção e nenhum campo extraído.
2. **Given** um arquivo cuja leitura não retorna nada, **When** a leitura é feita, **Then** o documento aparece com o motivo "leitura vazia" e a orientação de reenviar um arquivo mais nítido; os campos não são declarados "não localizados".
3. **Given** uma falha do componente de leitura (simulada nos testes), **When** a leitura é pedida, **Then** a paciente recebe um erro de indisponibilidade da leitura, sem detalhe técnico, a possibilidade de tentar de novo, e nenhum dado parcial é guardado como se fosse resultado.
4. **Given** um documento com leitura mal sucedida, **When** a paciente consulta a leitura, **Then** o motivo e a possibilidade de reenvio continuam disponíveis, e não existe resultado de completude associado.
5. **Given** um pedido com um documento lido e outro com leitura mal sucedida, **When** a leitura é consultada, **Then** o documento lido mostra seus campos e o outro mostra o motivo; um não esconde o outro.

---

### User Story 4 - Leitura acompanha a versão dos documentos (Priority: P2)

A leitura vale para a versão do conjunto documental em que foi feita. Se a paciente remover ou substituir um documento depois (RC-09), a leitura anterior deixa de valer: não é apresentada como atual, e uma nova leitura é necessária.

**Why this priority**: sem isso a paciente poderia ver dados de um arquivo que já não está no pedido.

**Independent Test**: ler um pedido, substituir um documento, consultar a leitura e conferir que a anterior está marcada como desatualizada; pedir nova leitura e conferir os dados do arquivo novo.

**Acceptance Scenarios**:

1. **Given** uma leitura concluída, **When** a paciente remove ou substitui um documento, **Then** a leitura anterior passa a constar como desatualizada, não como atual.
2. **Given** uma leitura desatualizada, **When** a paciente solicita nova leitura, **Then** os dados refletem os documentos atuais, e a leitura anterior não aparece como atual.
3. **Given** uma nova leitura pedida duas vezes seguidas sem mudança nos documentos, **When** o segundo pedido chega, **Then** não há duplicação: a paciente vê um único resultado atual para aquela versão.
4. **Given** um pedido em simulação de submissão, **When** a paciente solicita nova leitura, **Then** a solicitação é recusada com conflito documentado, mas a consulta da leitura existente continua permitida.

---

### Edge Cases

- Arquivo que não é um dos documentos sintéticos conhecidos: a leitura simulada não inventa campos; o documento recebe "leitura vazia" com orientação de reenvio, sem afirmar leitura real.
- Documento removido ou substituído: não entra na leitura.
- Duas solicitações simultâneas de leitura para o mesmo pedido: no máximo um resultado atual por versão do conjunto; nenhuma mistura de campos.
- Falha do componente de leitura no meio de vários documentos: nenhum resultado parcial é guardado como atual.
- Banco indisponível: a paciente recebe indisponibilidade sem detalhe técnico.
- Campo repetido ou conflitante no mesmo documento: não é resolvido por suposição; o campo fica "ilegível" (sem valor), pois o sistema não escolhe entre valores diferentes.
- Tipo documental informado no envio diferente do tipo identificado na leitura: os dois são mostrados, sem reconciliação nem pendência nesta história (a RC-12 decide).
- Identificação do documento por conteúdo, nome ou legenda: instruções contidas no arquivo nunca mudam o comportamento do sistema.
- Dados exibidos: nenhum dado de documento aparece em registros técnicos (logs) ou em mensagens de erro.

## Requirements *(mandatory)*

### Functional Requirements

**Solicitar e consultar a leitura**

- **FR-001**: A paciente dona do pedido MUST poder solicitar a leitura dos documentos disponíveis do seu pedido e consultar o resultado, identificando-se sempre pela sessão, nunca por identificador recebido no corpo.
- **FR-002**: O resultado MUST ser organizado por documento, e cada documento MUST informar o tipo documental, a situação da leitura (lido ou não lido, com motivo) e os campos extraídos.
- **FR-003**: Pedidos sem documentos disponíveis MUST receber recusa clara, sem criar resultado.
- **FR-004**: Somente o perfil paciente MUST acessar a leitura; sem autenticação MUST ser recusada, e administrador e simulador MUST ser recusados, sem acesso aos dados dos documentos.
- **FR-005**: Pedido inexistente ou de outra paciente MUST receber a mesma resposta de "não encontrado", sem revelar a existência do pedido.

**Campos e situações**

- **FR-006**: A leitura MUST produzir dados estruturados **apenas** para os campos usados pelas regras do protocolo: tipo/natureza do documento (para o encaminhamento e o documento de identificação), nome da paciente, data de nascimento, motivo/justificativa do encaminhamento, data de emissão e presença de autorização assinada do responsável.
- **FR-007**: Cada campo MUST estar em exatamente uma situação: **identificado** (com valor), **não localizado** (documento legível, campo ausente, sem valor) ou **ilegível** (sem valor).
- **FR-008**: Quando disponível, cada campo identificado MUST trazer a origem: página (quando o formato tem páginas) e trecho; o arquivo de origem é o documento a que o campo pertence.
- **FR-009**: O sistema MUST NOT completar, inferir ou aproximar o valor de um campo não localizado ou ilegível, nem tratar "ilegível" como "ausente".
- **FR-010**: "Não localizado" MUST só ser declarado para documentos efetivamente lidos; um documento com leitura mal sucedida MUST NOT declarar nenhum campo como não localizado.

**Leitura simulada**

- **FR-011**: A leitura MUST ser feita por um componente simulado, atrás de contrato próprio e substituível, que devolve dados predefinidos para o PDF e a fotografia sintéticos de demonstração, aplicando ao resultado as mesmas validações que se aplicariam a uma leitura real.
- **FR-012**: Toda resposta, documento e registro desta história MUST identificar a leitura como **simulada**; o sistema MUST NOT afirmar leitura real.
- **FR-013**: Para arquivos que não são os documentos sintéticos conhecidos, a leitura simulada MUST NOT inventar campos: o documento MUST receber "leitura vazia" com orientação de reenvio.

**Leitura mal sucedida**

- **FR-014**: Arquivo protegido MUST resultar em documento não lido com motivo "protegido", orientação de reenvio e nenhum campo.
- **FR-015**: Leitura vazia MUST resultar em documento não lido com motivo "leitura vazia", orientação de reenvio e nenhum campo declarado como não localizado.
- **FR-016**: Falha do componente de leitura MUST resultar em erro de indisponibilidade da leitura, sem detalhe técnico, com possibilidade de tentar de novo, sem gravar resultado parcial como atual.
- **FR-017**: Leitura mal sucedida (protegido, vazia ou falha) MUST NOT gerar resultado de completude do pedido nem pendência; a paciente é apenas orientada a reenviar ou tentar de novo.
- **FR-018**: Cada documento MUST ser tratado de forma independente: falha ou motivo de um não impede a leitura dos demais.

**Versão e estado**

- **FR-019**: O resultado MUST registrar a versão do conjunto documental em que foi feito; depois de remover ou substituir um documento (RC-09), a leitura anterior MUST constar como desatualizada e MUST NOT ser apresentada como atual.
- **FR-020**: Solicitar a mesma leitura mais de uma vez sobre a mesma versão MUST NOT duplicar resultados; o pedido MUST ter no máximo um resultado atual por versão.
- **FR-021**: Pedido em simulação de submissão MUST recusar a solicitação de leitura com conflito documentado, **mesmo que já exista leitura da versão atual** (o estado vem antes da repetição), mantendo a consulta permitida.
- **FR-022**: Nesta história a leitura MUST NOT alterar o estado do pedido nem produzir análise, resultado por regra ou resultado geral.

**Segurança e privacidade**

- **FR-023**: Conteúdo de documentos MUST ser tratado como dado não confiável: nenhum texto extraído altera regras, perfil, estado ou comportamento do sistema.
- **FR-024**: Valores extraídos e conteúdo de arquivos MUST NOT aparecer em registros técnicos nem em mensagens de erro; só identificadores técnicos, etapa, duração e códigos seguros.
- **FR-025**: Respostas MUST NOT expor caminho de arquivo, hash, nomes internos ou detalhes do componente de leitura.
- **FR-026**: Todas as respostas, incluindo erros, MUST seguir o formato padrão de erro do projeto e usar códigos HTTP coerentes e documentados.

**Documentação**

- **FR-027**: A API (rotas, respostas, erros, estados e a marcação de leitura simulada) MUST ser documentada em `docs/api` e no contrato, e as decisões e o modelo de dados MUST ser atualizados.

### Key Entities

- **Leitura (do pedido)**: resultado de uma solicitação de leitura; pertence a um pedido e a uma versão do conjunto documental; é "atual" ou "desatualizada"; sempre marcada como simulada.
- **Leitura de documento**: resultado por documento; situação (lido, protegido, leitura vazia); motivo e orientação de reenvio quando não lido; lista de campos.
- **Campo extraído**: código do campo (ligado às regras), situação (identificado, não localizado, ilegível), valor (só quando identificado) e origem (arquivo, página, trecho) quando disponível.
- **Contrato de leitura**: fronteira entre o sistema e o componente de leitura; hoje simulado, trocável por leitura real sem mudar o restante.
- **Documento, Pedido, Versão do conjunto documental**: já existentes (RC-08, RC-09).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Para os documentos sintéticos de demonstração (PDF e fotografia), 100% dos campos usados pelas regras saem na situação e com o valor definidos de antemão para o caso, em todas as execuções.
- **SC-002**: Em 100% dos casos de teste de campo "não localizado" ou "ilegível", nenhum valor aparece no resultado.
- **SC-003**: Em 100% das leituras mal sucedidas (protegido, vazia, falha), a paciente recebe motivo e possibilidade de reenvio ou nova tentativa, e nenhuma resposta traz resultado de completude, pendência ou estado de pedido alterado.
- **SC-004**: 100% das respostas e dos registros desta história identificam a leitura como simulada.
- **SC-005**: Depois de remover ou substituir um documento, 100% das consultas passam a marcar a leitura anterior como desatualizada.
- **SC-006**: Em testes com solicitações repetidas e simultâneas sobre a mesma versão, o pedido nunca fica com mais de um resultado atual.
- **SC-007**: Em 100% dos testes de autorização, perfis indevidos e outras pacientes não obtêm dados dos documentos, e o "não encontrado" é idêntico para pedido inexistente e alheio.
- **SC-008**: Nenhum valor extraído, caminho de arquivo ou detalhe técnico aparece em registros ou mensagens de erro nos testes de higiene de registros.

## Assumptions

- A leitura é pedida pela própria paciente, por uma ação explícita; ela não dispara sozinha ao enviar um documento (o envio continua sem ler nem avaliar, como na RC-08).
- A leitura simulada reconhece os documentos sintéticos de demonstração pelo conteúdo do arquivo; qualquer outro arquivo cai em "leitura vazia" (nunca em campos inventados).
- Os campos extraídos são exatamente os que o catálogo de regras do protocolo `PROTOCOLO-ACADEMICO-FICTICIO v0.1` usa (RC-R01 a RC-R06); campos fora disso não são extraídos.
- A conferência da transcrição pela paciente (RC-11) e a execução das regras (RC-12) usarão estes dados; esta história apenas os produz e os guarda.
- O estado do pedido não muda por causa da leitura; os estados que dependem da leitura (processando, requer conferência) são tratados com a RC-11 e a RC-12.
- Valores e documentos são sintéticos; nenhum dado real, e a leitura simulada não é apresentada como integração real em lugar nenhum.
- O armazenamento dos resultados da leitura pode exigir uma mudança de esquema; isso é decidido no plano, respeitando o prefixo `RC_` e sem alterar tabelas de outros trabalhos.
