# Quickstart: verificar a RC-28

A partir de `backend/` (o `mvnw` está versionado sem o bit de execução; use `sh ./mvnw`):

```bash
sh ./mvnw clean verify                     # suíte completa (inclui DocumentacaoEndpointTest, ContratoDaApiTest, RoteiroAvaliacaoTest)
sh ./mvnw -q -Dtest='Documentacao*,ContratoDaApi*,RoteiroAvaliacao*' test   # só os testes da RC-28
sh ./mvnw clean package -DskipTests && java -jar target/regulacheck-backend.jar
```

Com a API no ar:

1. Abra `http://localhost:8080/docs`: as rotas aparecem agrupadas e a descrição identifica dados sintéticos e partes simuladas.
2. `curl -s http://localhost:8080/docs/openapi.yaml | diff - ../docs/api/openapi.yaml` não mostra diferenças.
3. `curl -si -X POST http://localhost:8080/docs` → 405; `curl -si --path-as-is http://localhost:8080/docs/../pom.xml` → 404 (sem `--path-as-is`, o `curl` normaliza o `..` antes de enviar e o teste não exercita a travessia).
4. `curl -s -D - -o /dev/null http://localhost:8080/health | grep -i content-security` (GET; `-I` faria HEAD, que responde 405) continua `default-src 'none'; frame-ancestors 'none'`.
5. Com banco e carga sintética, siga [docs/api/roteiro-avaliacao.md](../../docs/api/roteiro-avaliacao.md) pela página (Authorize → Try it out) ou por `curl`, do login até o comprovante `SIM-…`. Sem banco, as rotas de negócio respondem 503 `DEPENDENCIA_INDISPONIVEL`.

Verificações fora do repositório (descartáveis, registradas em `docs/evidencias/`):

- Chrome headless em `/docs`: a página monta as operações, sem violação de política de conteúdo nem requisição externa.
- `npx @redocly/cli lint` sobre `docs/api/openapi.yaml`: 0 erros, inclusive exemplos compatíveis com os esquemas.
