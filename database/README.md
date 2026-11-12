# Banco de dados

**Oracle fornecido pela faculdade**, acessado por JDBC (decisão de 12/11/2026). O esquema é gerenciado por scripts SQL numerados em [`scripts/`](scripts/README.md) (criação, carga sintética, consultas, testes de restrições e limpeza separada), com finalidade e ordem explicadas em cada script e no README da pasta (RC-03). Modelo e dicionário de dados: [docs/modelo-de-dados.md](../docs/modelo-de-dados.md).

- Conexão: `oracle.fiap.com.br:1521`, SID `orcl`; Oracle Database 19c Enterprise Edition, charset AL32UTF8, permissões CREATE TABLE e CREATE SEQUENCE, quota ilimitada (verificado em 12/11/2026). O esquema do usuário já tem tabelas de outros trabalhos (`PEQUENOVAREJO`, `VENDA`), por isso todas as tabelas do RegulaCheck usam o prefixo `RC_` e nenhum script toca nas demais. Não versione credenciais: use `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` exportadas no ambiente (ver [docs/ambiente.md](../docs/ambiente.md)).
- A API **não** exige o banco para iniciar nesta etapa (`GET /health` não consulta o banco); o acesso JDBC será implementado na história que ligar a API ao banco.
- O `docker-compose.yml` e a configuração de PostgreSQL da fundação antiga foram removidos; não há banco local em contêiner.
- As colunas de chave gerada (`IDENTITY`) criam sequências internas com nome automático `ISEQ$$_…`; elas pertencem às tabelas `RC_*` e são removidas junto com elas.

## Como executar

Ordem: `01_criar_esquema.sql` → `02_carga_sintetica.sql` → `03_consultas_uso.sql` → `04_testes_restricoes.sql` → `05_sessao_login.sql` (RC-04; só exige o `01_`). O `90_limpeza_DESTRUTIVA.sql` fica fora da sequência e exige confirmação explícita. Detalhes, contas de demonstração e resultados esperados: [scripts/README.md](scripts/README.md).

## Verificação registrada

Execução **real** no Oracle 19c da faculdade em **12/11/2026**, com um executor temporário em Java (JDBC `ojdbc11` 23.26.4.0.0, fora do repositório) que roda as instruções na ordem do arquivo. Isto **não** é o SQL Developer; a sintaxe é a do Oracle e o resultado é o mesmo.

| Verificação | Resultado |
|---|---|
| Estado inicial do esquema | só `PEQUENOVAREJO` e `VENDA` (0 linhas cada); nenhum objeto `RC_` |
| `01_criar_esquema.sql` | 21 instruções, 0 erros: 12 tabelas, 1 índice único funcional e 8 índices; 12 PK, 15 FK, 9 UK; maior nome de restrição com 28 caracteres |
| `02_carga_sintetica.sql` | 1 bloco, 0 erros; contagens: usuários 3, protocolo 1, regras 6, pedidos 4, revisões 6, documentos 8, análises 5, resultados 30, pendências 8, sessão 1, leituras 4, submissão 1; os 3 hashes começam com `$2a$12$` e têm 60 caracteres; nenhuma senha em texto puro |
| `03_consultas_uso.sql` | 6 consultas, 0 erros; saídas iguais aos resultados esperados documentados (2, 5, 4, 6, 4 e 6 linhas) |
| `04_testes_restricoes.sql` | 12 violações rejeitadas com o código ORA esperado e 1 conferência de que nenhuma linha restou: 13 `OK`, 0 `FALHOU` |
| `01_` e `02_` repetidos | `01_`: 21 erros ORA-00955, nada alterado; `02_`: ORA-00001 (e-mail), bloco desfeito; as contagens permaneceram iguais |
| `90_` sem confirmação | falhou com ORA-20001 e não apagou nada |
| `90_` com confirmação (a pedido do responsável) | as 12 tabelas `RC_*` removidas; restaram só `PEQUENOVAREJO` e `VENDA` (0 linhas cada) e 0 objetos `RC_` |
| Recriação (`01_` → `02_` → `03_` → `04_`) | todos sem erro, com as mesmas contagens e os mesmos resultados; 13 `OK` no `04_` |

### RC-04 (`05_sessao_login.sql` e login contra o Oracle real, 12/11/2026)

| Verificação | Resultado |
|---|---|
| Estado antes | 12 tabelas `RC_*`, 77 linhas nelas, `RC_SESSAO_LOGIN` inexistente, `PEQUENOVAREJO` e `VENDA` com 0 linhas |
| `05_sessao_login.sql` | 2 instruções, 0 erros: `RC_SESSAO_LOGIN` com PK, FK, UK do hash do token, 2 CHECK (formato do hash e prazo) e 1 índice; maior nome de restrição com 26 caracteres |
| `05_` repetido | 2 erros ORA-00955, nada alterado |
| Estado depois | 13 tabelas `RC_*`; as 12 da RC-03 seguem com 77 linhas; `PEQUENOVAREJO` e `VENDA` com 0 linhas |
| Testes automatizados contra o Oracle (`AuthOracleTest`, 6 testes) | verdes: 3 contas entram, 401 idêntico para senha errada e e-mail inexistente, logout invalida o token e preserva outra sessão da conta, expiração no prazo, simulação de reinício, o token em claro nunca é o valor gravado |
| Percurso manual com `java -jar` | login das 3 contas (200), sessão (200), logout (204), reuso (401), 401 idêntico nos dois tipos de falha; sem `DB_*` o login dá 503 e `/health` segue 200 |
| Limpeza | os testes removeram só as sessões que criaram e as 3 linhas do percurso manual foram removidas por ID; `RC_SESSAO_LOGIN` terminou com 0 linhas |

Não verificado (RC-04): tempo do login ≤ 1 s no primeiro acesso (o primeiro login levou ~1,6 s por abrir a conexão com o Oracle e o seguinte ~0,3 s); execução no SQL Developer do `05_`.

Não verificado: execução no SQL Developer/SQLcl (o grupo deve repetir a ordem acima, incluindo o `05_`, uma vez no seu cliente); desempenho (fora do escopo, volume de demonstração).
