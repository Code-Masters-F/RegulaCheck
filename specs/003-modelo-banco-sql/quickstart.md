# Quickstart — verificar a entrega da RC-03

Pré-requisito: acesso ao Oracle da faculdade (`oracle.fiap.com.br:1521`, SID `orcl`) com usuário próprio, no SQL Developer, SQLcl ou outro cliente. Credenciais só no seu ambiente.

1. Conferir antes: `SELECT table_name FROM user_tables` — apenas tabelas de outros trabalhos (ex.: `PEQUENOVAREJO`, `VENDA`), nenhuma `RC_*`.
2. Executar `database/scripts/01_criar_esquema.sql` → 12 tabelas `RC_*` sem erro. Conferir com `SELECT table_name FROM user_tables WHERE table_name LIKE 'RC%'`.
3. Executar `02_carga_sintetica.sql` → contagens conforme `contracts/scripts-sql.md`. Verificar que `SENHA_HASH` começa com `$2a$12$` e que nenhuma senha em texto puro aparece.
4. Executar `03_consultas_uso.sql` → 6 consultas com resultado não vazio e igual ao esperado.
5. Executar `04_testes_restricoes.sql` → os 12 casos imprimem `OK`; nenhum `FALHOU`; nenhuma linha restante.
6. Repetir `01_` e `02_`: devem falhar de forma previsível (ORA-00955 e ORA-00001); a carga, por ser um único bloco, desfaz tudo e as contagens das tabelas `RC_*` não mudam.
7. Executar `90_limpeza_DESTRUTIVA.sql` apenas com confirmação explícita, em seguida refazer 01 → 02 para provar que o esquema é reproduzível; conferir que `PEQUENOVAREJO` e `VENDA` continuam intactas.
8. Varredura de segredos nos arquivos versionados (sem usuário, senha, senha em texto puro).
9. Conferir coerência com `docs/protocolo-academico.md`, `docs/api/estados-e-revisoes.md` e `docs/modelo-de-dados.md`.
