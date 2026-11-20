# RC-28 — Documentação da API e respostas HTTP (19/11/2026)

História RC-28 (#55) · spec [`specs/028-documentar-api-swagger/`](../../specs/028-documentar-api-swagger/) · branch `55-rc-28-documentar-api-e-respostas-http` (a partir da `main` em `a90f1a1`). Só texto: nenhuma captura de tela, relatório de teste ou token versionado.

Ambiente: Linux, OpenJDK 25.0.5.1 (`maven.compiler.release=21`), Maven Wrapper (`sh ./mvnw`), Chrome 158 headless, Node 24. Sem Oracle: as rotas de negócio foram exercitadas com repositórios em memória.

## Verificação automática (executada)

| Comando (a partir de `backend/`) | Resultado |
|---|---|
| `sh ./mvnw -q clean verify` antes da mudança (linha de base) | 786 testes, 0 falhas, 35 ignorados (Oracle opt-in), 65 s |
| `sh ./mvnw -q clean verify` depois da mudança | 799 testes, 0 falhas, 35 ignorados, 75 s |
| — `DocumentacaoEndpointTest` | 8/8: `/docs`, `/docs/` e `/docs/index.html` 200 HTML sem sessão; `/docs/openapi.yaml` byte a byte igual a `docs/api/openapi.yaml`; JS, CSS e PNG com o tipo correto e `nosniff`/`no-store`/`no-referrer`; inicializador sem endereço externo, com `validatorUrl: null` e `persistAuthorization: false`; caminhos fora da lista (inclusive arquivos da webjar não listados) 404 `NAO_ENCONTRADO` com a política estrita; travessia de pasta sem entregar arquivo; POST/PUT/DELETE 405 com `Allow: GET`; `/health` e a rota coringa com a política estrita |
| — `ContratoDaApiTest` | 3/3: `CONTRATO_API.md` e `openapi.yaml` com os mesmos 31 pares método + caminho; todas as operações com `tags`; 202 declarado como não usado e servidor `/` primeiro |
| — `RoteiroAvaliacaoTest` | 2/2: o percurso de [roteiro-avaliacao.md](../api/roteiro-avaliacao.md) pelas rotas reais, com o leitor simulado de produção e as amostras de `samples/`: login → serviço → rascunho 201 → envio 201 → leitura 201 → conferência `CONFERIDA` → análise 201 `COM_PENDENCIAS` (RC-R03) → histórico → resumo 201 simulado → inclusão da identificação 201 → reenvio 200 `CONFERENCIA` → reenvio 201 `CONCLUIDA` `SEM_PENDENCIAS_IDENTIFICADAS` (RC-R03 `RESOLVIDA`) → submissão 201 `SIM-…` → comprovante 200; exclusão de rascunho descartável 204 e depois 404; indicadores 200 para o administrador e 403 para a paciente |
| — `VarreduraDeSegredosTest`, `CabecalhosSegurosTest`, `RotasPrivadasTest`, `ArquiteturaTest` | verdes. A varredura acusou a variável de shell `TOKEN=` no roteiro, que foi renomeada; a regra não foi alterada |

## Verificação com a API empacotada (executada, sem banco)

`java -jar target/regulacheck-backend.jar` com `SERVER_PORT=18080`, sem `DB_*`:

- `curl /docs/openapi.yaml | diff - ../docs/api/openapi.yaml` → idêntico; `GET /docs` → 200 `text/html;charset=utf-8`; `POST /docs` → 405; `GET --path-as-is /docs/../pom.xml` → 404; `GET /docs/swagger-ui.js` → 404; `HEAD /docs` → 405.
- CSP de `GET /health`: `default-src 'none'; frame-ancestors 'none'` (sem mudança). CSP de `GET /docs`: `default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`.
- Chrome headless pelo protocolo DevTools (script descartável fora do repositório): título "RegulaCheck API (acadêmica, dados sintéticos) 1.0.0", **31 operações em 14 grupos**, **0 requisições a endereços externos**, **0 violações de CSP**, 0 exceções de script; "Try it out" pela página: `GET /health` → 200 `{"status":"UP"}` e `POST /auth/login` → 503 `DEPENDENCIA_INDISPONIVEL` (sem banco, como documentado), ambos em `http://127.0.0.1:18080` (mesma origem); `localStorage` e `sessionStorage` vazios.
- Antes de o servidor `/` entrar no contrato, o "Try it out" mandava a requisição para `http://localhost:8080` (outra origem). Isso confirmou a decisão D4/FR-004.

## Validação do contrato (executada)

`npx @redocly/cli@2.11.1 lint` no scratchpad (fora do repositório), com `struct`, `no-invalid-media-type-examples` (`allowAdditionalProperties: false`) e `no-invalid-schema-examples` como erro: **0 erros**. A primeira execução achou 7 erros **anteriores** a esta história: descrições sem aspas, com vírgula, dentro de `{ … }`, que viravam chaves soltas e cortavam o texto. Foram corrigidas pondo aspas, sem mudar o significado. Avisos restantes: `operationId` ausente, servidor `localhost` e o esquema `TextoSimulado` sem uso.

Os exemplos de resposta do contrato vêm de uma execução real do percurso em memória (registro temporário no scratchpad, removido do teste depois). O exemplo `PADRAO` do resumo usa o texto fixo do `ResumoService`, com as mesmas pendências, e o 503 veio do jar sem banco.

## Saídas brutas (19/11/2026, sem token nem senha)

Resumo do Surefire no `clean verify` depois do merge da `main` (soma dos `target/surefire-reports/*.txt`):

```text
testes=799 falhas=0 erros=0 ignorados=35
rc=0 tempo=73s
```

Quickstart com o jar em `http://127.0.0.1:18080`, sem `DB_*`:

```text
diff contrato: identico
GET /docs: 200 text/html;charset=utf-8
POST /docs: 405
GET /docs/../pom.xml: 404
GET /docs/swagger-ui.js: 404
CSP /health: Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
CSP /docs: Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
CSP GET /docs: Content-Security-Policy: default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'
HEAD /docs: 405 (so GET; HEAD = 405 com politica estrita)
```

Chrome headless (script CDP descartável): "Try it out" e verificações da página:

```text
GET /health -> {"status":"200","url":"http://127.0.0.1:18080/health","corpo":"{ \"status\": \"UP\" }"}
POST /auth/login -> {"status":"503","url":"http://127.0.0.1:18080/auth/login","corpo":"{ \"codigo\": \"DEPENDENCIA_INDISPONIVEL\", \"mensagem\": \"Servico temporariamente indisponivel. Tente novamente em instantes.\" }"}
storage -> {"local":0,"sessao":0}
externas -> []
csp -> []
```

Redocly (`npx @redocly/cli@2.11.1 lint`, regras `struct`, `no-invalid-media-type-examples` e `no-invalid-schema-examples` como erro):

```text
validated in 272ms
Woohoo! Your API description is valid. 🎉
```

## Coleção Postman (executada depois da revisão do PR #86)

`npx newman@6.2.1 run` (descartável, no scratchpad) sobre uma cópia da coleção, com as partes `arquivo` apontando para `samples/encaminhamento-sintetico.pdf` e `samples/identificacao-sintetica.png`, contra a API com repositórios em memória (servidor de teste temporário, não versionado) e a senha do cenário de teste:

```text
requisicoes 46
200 RC-16 Verificar a versao corrigida ... {'etapa': 'CONFERENCIA'}
200 RC-16 Consultar a conferencia da nova leitura ...
200 RC-16 Confirmar os campos lidos da nova leitura ...
201 RC-16 Verificar a versao corrigida de novo ... {'etapa': 'CONCLUIDA'} resultado=SEM_PENDENCIAS_IDENTIFICADAS
201 RC-17 Concluir a simulacao ... referencia SIM-2026-…
200 RC-17 Consultar o comprovante da submissao simulada ...
200 RC-09 Remover documento (rascunho descartavel)
400 RC-09 Excluir rascunho SEM confirmacao
204 RC-09 Excluir rascunho com confirmacao (rascunho descartavel)
204 RC-20 Logout do administrador
204 Logout da paciente
401 Sessao com chave do simulador / Simulador envia leitura (chaveSimulador vazia e servidor sem chave)
```

## Não executado

- Roteiro com o **Oracle real** (opt-in e manual; não autorizado nesta história) e o tempo de 15 minutos do SC-002, que só pode ser medido com uma pessoa.
- No navegador, login com conta válida → Authorize → rota da paciente contra a API com repositórios em memória (previsto no T011). O percurso pela API está coberto pelo `RoteiroAvaliacaoTest`, e o "Try it out" na mesma origem foi conferido com `GET /health` e `POST /auth/login`.
- Os comandos `curl` do roteiro não foram executados como script; os mesmos passos foram executados pelo `RoteiroAvaliacaoTest`.
- A coleção Postman contra o Oracle real. Ela foi executada em ordem só contra a API com repositórios em memória (ver acima).
