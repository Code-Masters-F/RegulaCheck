# Contrato: documentação da API (RC-28)

Rotas **públicas** (sem sessão), somente leitura, que não consultam banco e não expõem dado de paciente, segredo nem configuração. Fonte do conteúdo: [docs/api/openapi.yaml](../../../docs/api/openapi.yaml).

| Método | Caminho | Sucesso | Corpo |
|---|---|---|---|
| GET | `/docs` (também `/docs/` e `/docs/index.html`) | 200 `text/html;charset=utf-8` | página do Swagger UI que carrega `/docs/openapi.yaml` |
| GET | `/docs/openapi.yaml` | 200 `application/yaml;charset=utf-8` | contrato, byte a byte igual a `docs/api/openapi.yaml` da versão empacotada |
| GET | `/docs/inicializar.js`, `/docs/swagger-ui-bundle.js` | 200 `text/javascript;charset=utf-8` | scripts da página |
| GET | `/docs/swagger-ui.css`, `/docs/index.css` | 200 `text/css;charset=utf-8` | estilos |
| GET | `/docs/favicon-32x32.png`, `/docs/favicon-16x16.png` | 200 `image/png` | ícones |

## Erros

| Situação | Resposta |
|---|---|
| Qualquer outro caminho sob `/docs` (inclusive `..`, `%2e%2e`, nomes da webjar fora da lista) | 404 `NAO_ENCONTRADO`, corpo idêntico ao da rota coringa |
| Método diferente de GET | 405 `METODO_NAO_PERMITIDO`, cabeçalho `Allow: GET` |

Os erros mantêm a política de conteúdo estrita (`default-src 'none'; frame-ancestors 'none'`).

## Cabeçalhos das respostas 200

- `Content-Security-Policy: default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`
- `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`, `Referrer-Policy: no-referrer` e `Strict-Transport-Security` quando `API_PUBLIC_URL` é https (vindos do filtro global, sem mudança).

## Comportamento da página

- Executa as rotas na **mesma origem** (servidor `/` do contrato); os servidores `http://localhost:8080` e `http://10.0.2.2:8080` continuam listados para referência.
- Não envia o contrato a validador externo, não carrega nada de terceiros e não guarda a autorização depois que a aba é fechada.
- A autenticação é feita pelo botão **Authorize** (`bearerAuth`), com o token devolvido por `POST /auth/login`. A chave do simulador (`chaveSimulador`) só vale em `POST /sessoes-captura/{id}/leituras`.

## Rotas existentes

Nenhuma rota existente muda. O contrato de cada uma continua em [docs/api/openapi.yaml](../../../docs/api/openapi.yaml), agora com grupos (tags), exemplos fictícios e a declaração de que 202 não é usado.
