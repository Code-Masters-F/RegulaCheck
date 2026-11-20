# Feature Specification: Documentar API e respostas HTTP (RC-28)

**Feature Branch**: `55-rc-28-documentar-api-e-respostas-http` (branch do grupo; diretório da feature `specs/028-documentar-api-swagger`)

**Created**: 2026-11-19

**Status**: Implementada e verificada sem banco em 19/11/2026 (799 testes, 0 falhas; Chrome headless; Redocly 0 erros). Roteiro com o Oracle real pendente (opt-in). Registro: [tasks.md](tasks.md#registro-de-execução)

**Input**: User description: "RC-28 (issue #55) — Documentar API e respostas HTTP. Como avaliador, quero executar os endpoints e compreender seus contratos. Critérios: documentação em Swagger com todos os endpoints; pelo menos 6 endpoints funcionais cobrindo GET, POST, PUT e DELETE, com exemplos de entrada e saída de dados fictícios; 200/201/202/204 quando usados e os erros aplicáveis 400, 401, 403, 404, 409, 413, 415 e falha de dependência; um procedimento que permita autenticar, criar rascunho, enviar arquivo, analisar, consultar resultado, corrigir e submeter em simulação. Decisão do usuário (19/11/2026): Swagger UI servido pela própria API em /docs, com o contrato escrito à mão como fonte única."

**Rastreabilidade**: RC-28 = issue [#55](https://github.com/Code-Masters-F/RegulaCheck/issues/55) (P0, 2 h, prazo do backlog 17/11, dependências RC-04 a RC-22). Fontes atuais do contrato: [docs/api/openapi.yaml](../../docs/api/openapi.yaml), [CONTRATO_API.md](../../CONTRATO_API.md), [docs/api/erros.md](../../docs/api/erros.md), [docs/api/README.md](../../docs/api/README.md) e [docs/api/RegulaCheck.postman_collection.json](../../docs/api/RegulaCheck.postman_collection.json). Conjuntos sintéticos: [samples/catalogo-conjuntos.md](../../samples/catalogo-conjuntos.md) (RC-23). Numeração: a última feature em `specs/` é a 027; a 026 não existe neste repositório e não é reaproveitada.

**Decisão do usuário registrada (19/11/2026)**: o Swagger UI é servido pela própria API, embora as funcionalidades estejam congeladas desde 17/11. O usuário escolheu esta opção, e não a de manter só documentos estáticos, sabendo que ela traz uma dependência nova de arquivos estáticos, uma rota pública e uma política de conteúdo mais permissiva restrita a essa rota.

## Contexto

O contrato da API já existe em OpenAPI escrito à mão e cobre as 29 rotas implementadas. Ainda assim, um avaliador não consegue **executar** as rotas a partir dele sem ferramenta externa, porque a API não serve nenhuma visualização e não libera CORS. O contrato também tem poucos exemplos completos e uma descrição geral desatualizada (cita só as rotas até a RC-11). Além disso, o mapa de rotas e o índice da API ainda dizem que `GET /admin/indicadores` está "planejada", embora a RC-20 tenha sido implementada e verificada em 18/11/2026. A coleção Postman não tem essa rota, e nela a exclusão do rascunho aparece antes da leitura, o que interrompe o percurso de quem a executa em ordem. Não há um roteiro único que leve o avaliador do login até o comprovante da submissão simulada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar e executar a API pela documentação interativa (Priority: P1)

O avaliador inicia a API, abre o endereço de documentação no navegador e vê todas as rotas agrupadas por assunto. Cada rota mostra o que faz, quem pode usá-la, o que recebe, o que devolve, os códigos de resposta e exemplos fictícios. Ele faz login, informa o token recebido e executa as rotas ali mesmo, vendo o status e o corpo reais.

**Why this priority**: É o primeiro critério do backlog ("documentação em Swagger com todos os endpoints") e o objetivo declarado da história ("executar os endpoints e compreender seus contratos").

**Independent Test**: Com a API em execução, abrir `/docs` num navegador, conferir que todas as rotas do mapa aparecem e executar `GET /health` e `POST /auth/login` pela própria página.

**Acceptance Scenarios**:

1. **Given** a API em execução, com ou sem banco configurado, **When** o avaliador abre `/docs`, **Then** a documentação interativa aparece sem login, lista todas as rotas implementadas agrupadas por assunto e se identifica como API acadêmica com dados sintéticos, leitura e resumo simulados.
2. **Given** a documentação aberta e o banco configurado com a carga sintética, **When** o avaliador executa o login com uma conta de demonstração e informa o token no botão de autorização, **Then** consegue executar as rotas da paciente pela página, na mesma origem da API, e vê o status e o corpo reais.
3. **Given** qualquer rota da documentação, **When** o avaliador abre seus detalhes, **Then** vê os códigos de sucesso realmente usados (200, 201 ou 204), os erros aplicáveis com o código estável do catálogo e exemplos fictícios de entrada (quando há corpo) e de saída.
4. **Given** a documentação aberta, **When** o avaliador baixa o contrato pelo endereço indicado na página, **Then** recebe exatamente o mesmo conteúdo do contrato versionado no repositório.
5. **Given** a documentação aberta, **When** a página carrega, **Then** nenhum recurso é buscado fora da própria API e o token informado não fica guardado no navegador depois que a página é fechada.

---

### User Story 2 - Seguir um roteiro ponta a ponta até a submissão simulada (Priority: P1)

O avaliador segue um roteiro único, passo a passo. Ele autentica, cria um rascunho, envia um arquivo sintético, pede a leitura (simulada), confere a transcrição, pede a análise e consulta o resultado com pendência. Depois corrige, incluindo o documento que faltava, verifica a versão corrigida e submete em simulação, recebendo o comprovante. Cada passo indica a rota, o arquivo de amostra, o status esperado e os campos a observar.

**Why this priority**: É o quarto critério do backlog e prova que os contratos documentados funcionam juntos. Sem ele, a documentação mostra rotas isoladas, sem mostrar o percurso da paciente.

**Independent Test**: Seguir o roteiro do primeiro ao último passo com a API e a carga sintética e chegar ao comprovante `SIM-…`; o mesmo percurso é executado automaticamente pelas rotas reais, sem banco real, e o resultado é registrado.

**Acceptance Scenarios**:

1. **Given** a API com banco e carga sintética e as amostras de `samples/`, **When** o avaliador segue o roteiro, **Then** passa por autenticar → criar rascunho → enviar arquivo → ler → conferir → analisar → consultar resultado com pendência → corrigir → verificar a versão corrigida → submeter em simulação → consultar o comprovante, e cada resposta tem o status previsto no roteiro.
2. **Given** o roteiro, **When** ele é contado, **Then** usa pelo menos 6 rotas diferentes e cobre os métodos GET, POST, PUT e DELETE.
3. **Given** a conta de administrador de demonstração, **When** o avaliador segue o passo administrativo, **Then** consulta os indicadores agregados e vê que a conta de paciente recebe 403 nessa rota.
4. **Given** o roteiro escrito, **When** o mesmo percurso é executado automaticamente pelas rotas reais com repositórios em memória, **Then** todos os status e campos previstos conferem.

---

### User Story 3 - Materiais de execução e índices coerentes (Priority: P2)

Quem prefere a coleção Postman ou a linha de comando encontra as mesmas rotas, na mesma situação, em todos os documentos. A coleção tem todas as rotas, e executá-la em ordem não apaga o pedido do percurso. O mapa de rotas, o índice da API e o contrato concordam entre si e com o código.

**Why this priority**: Evita que o avaliador encontre documentos contraditórios. Depende do contrato completo da US1.

**Independent Test**: Comparar automaticamente as rotas e métodos do mapa de rotas com os do contrato (0 divergência) e importar a coleção Postman para conferir que `GET /admin/indicadores` existe e que a exclusão do rascunho usa um pedido próprio, fora do percurso principal.

**Acceptance Scenarios**:

1. **Given** o mapa de rotas e o contrato, **When** são comparados automaticamente, **Then** listam o mesmo conjunto de pares método + caminho.
2. **Given** a coleção Postman, **When** é executada em ordem, **Then** inclui `GET /admin/indicadores` e só exclui um rascunho criado para esse fim, sem apagar o pedido usado no percurso até a submissão.
3. **Given** o índice da API e o mapa de rotas, **When** são lidos, **Then** `GET /admin/indicadores` aparece como implementada (RC-20), e a documentação interativa e o roteiro aparecem com seus endereços.

---

### Edge Cases

- **API sem banco**: `/docs` abre normalmente; as rotas de negócio executadas pela página respondem 503 `DEPENDENCIA_INDISPONIVEL`, e o contrato e o roteiro dizem isso.
- **Caminho inexistente sob `/docs`** (por exemplo, nome de arquivo inventado ou tentativa de subir de pasta): 404 `NAO_ENCONTRADO` no formato padrão; nenhum arquivo fora da lista fixa é lido.
- **Método diferente de GET em `/docs`**: 405 `METODO_NAO_PERMITIDO`.
- **Token expirado ou encerrado durante a avaliação**: a rota responde 401 `NAO_AUTENTICADO`, documentado; o roteiro orienta fazer login de novo.
- **Página aberta por outro endereço** (IP da rede, `127.0.0.1`): a execução usa a origem da própria página, e não um endereço fixo.
- **API publicada por HTTPS**: a página mantém os mesmos cabeçalhos de segurança, mais o HSTS.
- **Demais rotas**: continuam com a política de conteúdo estrita atual; a exceção não se espalha.
- **Pacote sem os arquivos da documentação**: o defeito é detectado na verificação automatizada e na subida da API, nunca silenciosamente.
- **Upload pela página acima de 5 MB ou com tipo errado**: respostas 413 e 415 reais, documentadas.
- **Código 202**: não é usado por nenhuma rota (processamentos síncronos); o contrato diz isso explicitamente, sem inventar um 202.
- **Falha do adaptador simulado de resumo**: não é erro HTTP; a resposta é 201 com resumo `PADRAO`, e o contrato mostra esse caso.
- **Senha de demonstração**: o roteiro e os exemplos não a copiam; apontam para o documento de origem.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A API MUST servir em `GET /docs` uma documentação interativa (Swagger UI) que lista todas as rotas implementadas, sem exigir sessão.
- **FR-002**: O contrato MUST ser servido em `GET /docs/openapi.yaml` com conteúdo idêntico ao do arquivo versionado `docs/api/openapi.yaml`, que continua sendo a fonte única; nenhuma cópia do contrato é mantida à mão em outro lugar.
- **FR-003**: A página e seus recursos MUST ser servidos pela própria API, em versão fixa, sem buscar nada fora dela. A página MUST NOT enviar o contrato a validadores externos e MUST NOT guardar o token informado depois que é fechada.
- **FR-004**: A execução de rotas pela página MUST usar a mesma origem da página, sem depender de CORS; os endereços alternativos já documentados (computador e emulador) permanecem listados.
- **FR-005**: As rotas de documentação MUST aceitar somente GET (outros métodos: 405 `METODO_NAO_PERMITIDO`) e servir somente uma lista fixa de arquivos; qualquer outro caminho sob `/docs` MUST responder 404 `NAO_ENCONTRADO` no formato padrão de erro.
- **FR-006**: A política de conteúdo mais permissiva necessária à página (scripts e estilos próprios, imagens embutidas e conexões só com a mesma origem) MUST valer somente nas respostas das rotas de documentação. Todas as demais respostas MUST manter a política atual (`default-src 'none'; frame-ancestors 'none'`). As rotas de documentação MUST manter os demais cabeçalhos de segurança (`nosniff`, `no-store`, `no-referrer`, proibição de quadros e HSTS quando houver HTTPS).
- **FR-007**: O contrato MUST descrever cada rota implementada com resumo, grupo de assunto, perfil exigido, parâmetros, corpo e os códigos de sucesso realmente usados (200, 201 ou 204). MUST declarar explicitamente que 202 não é usado, porque os processamentos são síncronos.
- **FR-008**: O contrato MUST documentar, por rota, os erros aplicáveis entre 400, 401, 403, 404, 405, 409, 413, 415, 422 e 503, com o código estável do catálogo de erros. Isso inclui as falhas de dependência (banco: `DEPENDENCIA_INDISPONIVEL`; leitor simulado: `LEITURA_INDISPONIVEL`; avaliação: `ANALISE_INDISPONIVEL`) e a queda do resumo simulado para o resumo `PADRAO` (201, não erro).
- **FR-009**: Os exemplos MUST usar somente dados fictícios (contas `.invalid`, nomes sintéticos, protocolo acadêmico fictício) e MUST NOT conter senha, token ou chave reais. MUST existir exemplo de entrada (quando há corpo) e de saída para cada rota do roteiro e para o formato de erro.
- **FR-010**: Os exemplos MUST ser compatíveis com os esquemas do próprio contrato e MUST corresponder a respostas obtidas da API em execução, não a respostas imaginadas.
- **FR-011**: MUST existir um roteiro de avaliação com o percurso autenticar → criar rascunho → enviar arquivo → ler → conferir → analisar → consultar resultado → corrigir → verificar a versão corrigida → submeter em simulação → consultar comprovante, mais um passo de exclusão (DELETE) e um passo administrativo. Cada passo MUST indicar a rota, o status esperado e os campos a observar, e MUST ser executável pela documentação interativa e pela linha de comando.
- **FR-012**: O roteiro MUST usar somente as amostras sintéticas de `samples/` e as contas de demonstração, e MUST indicar onde está a senha de demonstração, sem copiá-la.
- **FR-013**: O percurso do roteiro MUST ser verificado automaticamente pelas rotas reais da API, com repositórios em memória, e o resultado MUST ser registrado. A verificação contra o Oracle real é opt-in e só é feita com autorização.
- **FR-014**: A coleção Postman MUST incluir todas as rotas de negócio e técnicas implementadas, inclusive `GET /admin/indicadores`; as rotas de documentação ficam fora dela, porque são abertas no navegador. Executada em ordem, MUST NOT excluir o pedido usado no percurso antes da submissão simulada.
- **FR-015**: O mapa de rotas (`CONTRATO_API.md`) e o contrato MUST listar o mesmo conjunto de pares método + caminho, verificado automaticamente. A situação de `GET /admin/indicadores` MUST aparecer como implementada no mapa e no índice da API.
- **FR-016**: Nenhuma rota existente pode ter comportamento, status, formato de resposta ou regra de autorização alterados; só as rotas de documentação são novas.
- **FR-017**: README, `docs/ambiente.md`, `docs/decisoes.md`, `docs/api/protecoes.md` e `docs/api/autorizacao.md` MUST registrar a nova rota pública, a exceção na política de conteúdo, a nova dependência e como abrir a documentação.
- **FR-018**: A solução MUST NOT usar Spring nem gerar o contrato automaticamente. A única dependência nova é de arquivos estáticos, com versão fixa (sem SNAPSHOT nem intervalo).

### Key Entities

- **Contrato da API**: descrição OpenAPI escrita à mão, com rotas, grupos, esquemas, respostas e exemplos. Tem uma única fonte versionada e é servido pela API sem alteração.
- **Documentação interativa**: página servida pela API que exibe o contrato e executa as rotas na mesma origem.
- **Roteiro de avaliação**: sequência ordenada de passos (rota, amostra, status esperado, campos a observar) do login ao comprovante.
- **Mapa de rotas**: tabela resumida de rotas e perfis, que deve concordar com o contrato.
- **Coleção Postman**: requisições prontas, na ordem do percurso, com variáveis para token e identificadores.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das rotas implementadas (as 29 atuais e as 2 de documentação) aparecem na documentação interativa, com 0 divergência entre o mapa de rotas e o contrato.
- **SC-002**: Um avaliador que siga só o roteiro vai do login ao comprovante da submissão simulada em até 15 minutos, sem editar nenhum arquivo.
- **SC-003**: 100% das rotas do roteiro têm exemplo de saída e, quando recebem corpo, exemplo de entrada; 0 exemplo contém dado real, senha, token ou chave.
- **SC-004**: Pelo menos 6 rotas executáveis pelo roteiro cobrem os métodos GET, POST, PUT e DELETE.
- **SC-005**: A página de documentação abre com 0 requisições a endereços externos e 0 violações da política de conteúdo registradas pelo navegador.
- **SC-006**: As demais rotas têm 0 mudança nos cabeçalhos de segurança, e a suíte de testes existente tem 0 falhas.
- **SC-007**: O contrato tem 0 erros de validação segundo a especificação OpenAPI 3.0, e 0 exemplos incompatíveis com seus esquemas.

## Assumptions

- O avaliador usa um navegador atual no computador onde a API roda (`http://localhost:8080`) ou num endereço da mesma rede.
- Executar as rotas de negócio exige o Oracle configurado com os scripts e a carga sintética ([docs/ambiente.md](../../docs/ambiente.md)). Sem banco, a documentação abre e essas rotas respondem 503.
- A versão do Swagger UI é fixada numa versão estável publicada há algumas semanas. A versão publicada no próprio dia da entrega não é adotada.
- Sem CORS, a execução pela página só funciona na mesma origem da API; um editor externo pode apenas ler o contrato.
- `/docs` é pública porque expõe só o contrato, que já é público no repositório; não expõe dado de paciente, segredo nem configuração.
- O erro 500 `ERRO_INTERNO` continua documentado de forma genérica no catálogo de erros, e não rota a rota.
- O percurso principal do roteiro usa as duas amostras oficiais da raiz de `samples/`, que são os arquivos do conjunto 01. Primeiro vai só o encaminhamento: como no conjunto 02, falta a identificação, e o resultado é `COM_PENDENCIAS`. A correção inclui a identificação, e o resultado passa a `SEM_PENDENCIAS_IDENTIFICADAS`. Depois vem a submissão simulada.
- O resumo simulado, a leitura simulada e a submissão simulada continuam identificados como simulados em toda a documentação.
