# API

Contrato proposto: [../../CONTRATO_API.md](../../CONTRATO_API.md). Nesta etapa existe apenas o health da fundacao antiga (`GET /actuator/health`); na stack nova a rota prevista e `GET /health`. Formato de documentacao: Postman Collection do health (`RegulaCheck.postman_collection.json`); Swagger/OpenAPI fica para quando houver endpoints de negocio; como nao ha Spring, a documentacao sera Postman ou OpenAPI escrito a mao.

Contratos a respeitar (ver [../arquitetura.md](../arquitetura.md)): isolamento por dona da solicitacao; upload ate 3 arquivos, 5 MB cada, PDF ate 5 paginas, JPEG/PNG, so material sintetico; resultado por regra (atende, pendente, nao verificavel, nao aplicavel); submissao idempotente e simulada; telemetria enviada em JSON por HTTP pelo simulador Java, com sessao, tipo, valor, unidade e data; resumo de IA e extracao mockados e identificados como simulados.
