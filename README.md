# RegulaCheck

Verificador de completude documental de encaminhamentos (MVP acadêmico, **dados sintéticos**, submissão **simulada**). O sistema não diagnostica, não define urgência e não autoriza atendimento.

## Decisões de tecnologia (12/11/2026)
- Backend em **Java puro** (Jakarta Servlet em servidor embutido), **sem Spring e sem JPA** em produção; Spring Boot só em testes automatizados.
- Banco **Oracle fornecido pela faculdade**, acesso por JDBC.
- **2 sensores simulados por script Java**, enviando leituras em JSON por HTTP.
- **IA e extração/OCR mockadas**, sempre identificadas como simuladas.
- Mobile: Android nativo com Kotlin (decidido, não criado).
- Desenvolvimento conduzido com o GitHub Spec Kit (ver `CLAUDE.md` e `.specify/`).

Detalhes e pendências em [docs/decisoes.md](docs/decisoes.md). Recorte e regras do demonstrador em [docs/protocolo-academico.md](docs/protocolo-academico.md); tecnologias e limitações em [docs/escopo-e-limitacoes.md](docs/escopo-e-limitacoes.md).

## Estado real
A base técnica de `backend/` já está em Java puro (Jetty + Servlet, sem Spring/JPA) e expõe `GET /health`, a autenticação (`/auth/*`), o catálogo de serviços (`GET /protocolos`), o rascunho da solicitação (`/solicitacoes`, RC-07), o envio, a lista e o download dos documentos do pedido (`/solicitacoes/{id}/documentos`, RC-08) e, na RC-09, remover e substituir documento (`DELETE`/`PUT …/documentos/{docId}`) e excluir o rascunho com confirmação (`DELETE /solicitacoes/{id}` com o cabeçalho `X-Confirmar-Exclusao: EXCLUIR`); a RC-09 não tem script SQL novo e seu teste contra o Oracle (`AlteracaoDocumentoOracleTest`) é opt-in e **ainda não foi executado**. Dentro do código estão implementados e verificados com repositórios em memória; a RC-07 também foi verificada em 13/11/2026 contra o Oracle da faculdade, na conta usada pelo grupo, com o script `database/scripts/06_motivo_revisao_protocolo.sql` aplicado (ver [docs/decisoes.md](docs/decisoes.md)). Desde a RC-12 existe o **motor de regras** (seis critérios do protocolo acadêmico, resultado por regra, pendências e resultado geral) e a montagem da avaliação a partir da leitura e da conferência. Na RC-13, `POST /solicitacoes/{id}/analises` faz a análise (síncrona; o pedido fica em `PROCESSANDO` só enquanto ela roda, com limite de 1 minuto), **grava** resultados, pendências e evidências, muda o estado para o resultado geral e devolve a análise; `GET /solicitacoes/{id}/analises` mostra o histórico com a vigente marcada. A análise é simulada (leitura simulada, protocolo fictício) e "sem pendências identificadas" não é aprovação do SUS. A RC-13 exige o script `database/scripts/10_analise_historico.sql` em banco criado antes dela e foi verificada contra o Oracle da faculdade em 14/11/2026 (script aplicado, `04_` com 32 OK e `AnaliseOracleTest` com 2 testes verdes). Na RC-21 + RC-22 (Épico 7) existe a **estação de captura simulada**: a paciente abre a sessão de captura do pedido (`POST /solicitacoes/{id}/sessoes-captura`), o simulador Java [`simulador-iot/SimuladorSensores.java`](simulador-iot/README.md) envia leituras de luminosidade (lux) e distância (cm) com a chave `IOT_API_KEY` (`POST /sessoes-captura/{id}/leituras`) e a paciente vê a orientação de cada sensor em `GET /solicitacoes/{id}/leituras`; os limites são demonstrativos, sem calibração, as leituras não comprovam a legibilidade do arquivo e nunca bloqueiam o envio. Exige o script `database/scripts/11_iot_sessao_captura.sql` em banco criado antes dela; foi verificada sem banco (o `CapturaOracleTest` é opt-in e ainda não foi executado). A RC-14 acrescenta resumo de pendências **simulado** pela rota `POST /solicitacoes/{id}/resumo`, com fallback recuperável e consulta pelo histórico; ver [contrato](specs/018-resumo-pendencias-simulado/contracts/api-resumo.md). A RC-16 fecha o ciclo de correção: o documento que faltava pode ser **incluído** depois da análise (exceto em pedido submetido), `POST /solicitacoes/{id}/reenvio` verifica a versão corrigida (leitura → conferência → análise → resumo, parando enquanto a paciente precisa conferir) e toda análise traz a `comparacao` com a anterior (resolvida, continua, nova), sem nunca marcar pendência como resolvida pelo simples envio; sem script SQL novo, verificada sem banco e, em 17/11/2026, no Oracle da faculdade (`ReenvioOracleTest` e `AnaliseOracleTest` verdes); ver [contrato](specs/020-reenviar-versao-corrigida/contracts/api-reenvio.md). A RC-17 conclui a **submissão simulada**: `POST /solicitacoes/{id}/submissoes-simuladas` aceita só pedido com análise vigente sem pendências nem critérios não verificáveis, registra a submissão e devolve o comprovante (referência `SIM-…`, data, serviço, versão analisada e o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS"); repetir devolve o mesmo comprovante e `GET` na mesma rota o recupera; nada é enviado ao SUS e não há fila real nem protocolo oficial; ver [contrato](specs/021-submeter-simulacao/contracts/api-submissao.md). O app mobile ainda não está implementado. A RC-08 ainda **não foi verificada contra o Oracle real**: depende do script `database/scripts/07_documento_tipo_e_tentativa.sql` e do `DocumentoOracleTest`. Os contratos de dados, erros e estados estão em [docs/api](docs/api/README.md). Desde a RC-10, `POST/GET /solicitacoes/{id}/extracoes` fazem a **leitura simulada** (dados sintéticos predefinidos, sem leitura real) dos documentos do pedido. Na RC-11, `GET/PUT /solicitacoes/{id}/extracoes/{extracaoId}/conferencia` mostram o que foi lido e deixam a paciente **só confirmar ou marcar divergência**, campo a campo (nenhum valor digitado é aceito); a conferência não muda o estado nem a revisão do pedido e nunca libera submissão. A RC-11 exige o script `database/scripts/09_conferencia.sql` e foi verificada contra o Oracle da faculdade em 13/11/2026 (script aplicado e `ConferenciaOracleTest` com 3 testes verdes).

## Executar (verificado em 12/11/2026)
Pré-requisito: JDK 21+. A partir de `backend/` (sem banco e sem variáveis):
- Windows: `.\mvnw.cmd clean verify` e depois `java -jar target\regulacheck-backend.jar`
- Linux/macOS: `./mvnw clean verify` e depois `java -jar target/regulacheck-backend.jar`
- Conferir: `curl http://localhost:8080/health` -> `{"status":"UP"}` (UP não prova que o MVP está completo nem que o banco está acessível)
- Documentação da API: `http://localhost:8080/docs` (ver [Documentação da API (Swagger)](#documentação-da-api-swagger))

Detalhes, acesso do emulador (`10.0.2.2`) e problemas comuns em [docs/ambiente.md](docs/ambiente.md).

## Documentação da API (Swagger)
Com a API no ar, abra **`http://localhost:8080/docs`** no navegador. É o Swagger UI servido pela própria API (RC-28): público, somente leitura, sem CDN e sem rede externa. Ele mostra todas as rotas agrupadas por assunto, com perfil exigido, parâmetros, códigos de resposta e exemplos de entrada e saída. Os exemplos usam só dados fictícios.

| Endereço | Conteúdo |
|---|---|
| `GET /docs` | Página do Swagger UI (também `/docs/` e `/docs/index.html`) |
| `GET /docs/openapi.yaml` | O contrato OpenAPI 3.0, idêntico a [docs/api/openapi.yaml](docs/api/openapi.yaml) |

**Como executar as rotas pela página**
1. Em `POST /auth/login`, clique em **Try it out**, informe uma conta de demonstração (`paciente.a@exemplo.invalid` ou `admin@exemplo.invalid`) e a senha de demonstração de [database/scripts/README.md](database/scripts/README.md#contas-e-senhas-de-demonstração), e clique em **Execute**.
2. Copie o `token` da resposta, clique em **Authorize** (`bearerAuth`), cole o token e confirme.
3. Em qualquer rota, use **Try it out** → **Execute**. Mantenha o servidor `/`, que é a mesma origem da página.

Siga o percurso completo (autenticar, criar rascunho, enviar arquivo, analisar, consultar, corrigir e submeter em simulação) em [docs/api/roteiro-avaliacao.md](docs/api/roteiro-avaliacao.md), com as amostras de `samples/`.

**Limites**
- As rotas de negócio precisam do banco Oracle com a carga sintética. Sem `DB_*`, a página abre normalmente, mas essas rotas respondem 503 `DEPENDENCIA_INDISPONIVEL`.
- A execução só funciona pela própria API, porque não há CORS. Um Swagger Editor externo serve apenas para ler o contrato.
- O token fica só na memória da aba e some quando ela é fechada; a sessão expira em 30 minutos.
- Leitura, resumo e submissão são **simulados**: nada é enviado ao SUS, e nenhum resultado é aprovação ou diagnóstico.

**Manutenção**: o contrato é escrito à mão em `docs/api/openapi.yaml`, a única fonte. O build o copia para o jar, então depois de editá-lo basta gerar o jar de novo (`./mvnw clean package`). O `ContratoDaApiTest` confere se o mapa de rotas de [CONTRATO_API.md](CONTRATO_API.md) e o contrato listam as mesmas rotas. Os arquivos do Swagger UI vêm da dependência `org.webjars:swagger-ui` (versão fixa no `backend/pom.xml`). A alternativa à página é a coleção [Postman](docs/api/RegulaCheck.postman_collection.json); o índice completo está em [docs/api/README.md](docs/api/README.md).

## Estrutura
`backend/` API | `mobile/` | `simulador-iot/` | `database/` | `docs/` | `samples/` | `entrega/`. Arquitetura em [docs/arquitetura.md](docs/arquitetura.md).
