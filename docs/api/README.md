# API

Contrato proposto: [../../CONTRATO_API.md](../../CONTRATO_API.md). Nesta etapa existe apenas `GET /actuator/health`. Formato de documentacao: Postman Collection do health (`RegulaCheck.postman_collection.json`); Swagger/OpenAPI fica para quando houver endpoints de negocio e a biblioteca for autorizada.

Contratos a respeitar (ver [../arquitetura.md](../arquitetura.md)): isolamento por dona da solicitacao; upload ate 3 arquivos, 5 MB cada, PDF ate 5 paginas, JPEG/PNG, so material sintetico; resultado por regra (atende, pendente, nao verificavel, nao aplicavel); submissao idempotente e simulada; telemetria com sessao, tipo, valor, unidade e data.
