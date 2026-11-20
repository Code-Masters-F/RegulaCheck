-- =============================================================================
-- RegulaCheck - rodar_tudo.sql
-- Finalidade : criar o banco do zero em UM comando: esquema, carga sintetica e
--              tabela de sessoes de login, na ordem correta.
-- Executa    : 01_criar_esquema.sql -> 02_carga_sintetica.sql -> 05_sessao_login.sql
-- Como usar  : conectado ao Oracle 19c com o seu usuario, rode ESTE arquivo como script
--              (SQL Developer: abra o arquivo e tecle F5; SQLcl/SQL*Plus: @rodar_tudo.sql
--              estando em database/scripts). O @@ resolve os caminhos a partir desta pasta.
-- Pre-requisito: usuario SEM tabelas RC_* (banco novo). Em banco ja criado, o primeiro CREATE falha
--              com ORA-00955 e o script PARA (WHENEVER SQLERROR EXIT); veja "Segunda execucao e recomeco" no README desta pasta.
-- Nao inclui : 03_ (consultas) e 04_ (testes de restricoes), que sao opcionais e nao mudam dados;
--              06_ a 11_ (o 01_ atual ja traz o que eles acrescentam); 90_ (destrutivo).
-- Dados      : sinteticos; protocolo ficticio.
-- =============================================================================
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT >>> 1/3 Criando o esquema (01_criar_esquema.sql)
@@01_criar_esquema.sql

PROMPT >>> 2/3 Inserindo a carga sintetica (02_carga_sintetica.sql)
@@02_carga_sintetica.sql

PROMPT >>> 3/3 Criando a tabela de sessoes de login (05_sessao_login.sql)
@@05_sessao_login.sql

PROMPT >>> Fim sem erros.
