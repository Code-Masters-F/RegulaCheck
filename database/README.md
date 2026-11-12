# Banco de dados

**Oracle fornecido pela faculdade**, acessado por JDBC (decisão de 12/11/2026). O esquema será gerenciado por scripts SQL em `scripts/` (criação, carga sintética e consultas), com finalidade e ordem de execução explicadas em cada script (RC-03).

- Dados de conexão ainda pendentes. Não versione credenciais: use `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` exportadas no ambiente (ver [docs/ambiente.md](../docs/ambiente.md)).
- A API **não** exige o banco para iniciar nesta etapa (`GET /health` não consulta o banco).
- O `docker-compose.yml` e a configuração de PostgreSQL da fundação antiga foram removidos; não há banco local em contêiner.
