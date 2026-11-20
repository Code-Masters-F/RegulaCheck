# Data Model: Documentar API e respostas HTTP (RC-28)

Nenhuma tabela, script SQL ou dado persistido muda. As "entidades" abaixo são estruturas em memória e documentos.

## Arquivo da documentação (em memória, imutável)

| Campo | Tipo | Regra |
|---|---|---|
| nome público | texto | chave exata da URL sob `/docs` (`""`, `"/"`, `"/index.html"`, `"/inicializar.js"`, `"/openapi.yaml"`, `"/swagger-ui.css"`, `"/swagger-ui-bundle.js"`, `"/index.css"`, `"/favicon-32x32.png"`, `"/favicon-16x16.png"`); qualquer outro → 404 |
| conteúdo | bytes | lido do classpath na construção do controller; ausência impede a subida |
| tipo de conteúdo | texto | `text/html;charset=utf-8`, `text/javascript;charset=utf-8`, `application/yaml;charset=utf-8`, `text/css;charset=utf-8` ou `image/png` |

Origem: página e inicializador em `documentacao/` (próprios); contrato em `documentacao/openapi.yaml` (copiado de `docs/api/openapi.yaml` no build); demais da webjar `META-INF/resources/webjars/swagger-ui/{versão}/`.

## Passo do roteiro (documento + teste)

| Campo | Regra |
|---|---|
| número e objetivo | ordem fixa: autenticar → serviços → rascunho → envio → leitura → conferência → análise → resultado → resumo → correção → reenvio (conferência) → conferência → reenvio → submissão → comprovante → exclusão de rascunho descartável → indicadores |
| rota e método | um par método + caminho do contrato |
| entrada | corpo JSON ou multipart com amostra de `samples/`; nunca senha copiada |
| status esperado | conferido no `RoteiroAvaliacaoTest` |
| campos a observar | por exemplo `estado`, `resultadoGeral`, `etapa`, `referencia` |
