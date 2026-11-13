# Scripts SQL do RegulaCheck (RC-03)

Scripts para **Oracle 19c** (SQL Developer, SQLcl ou SQL*Plus). `;` encerra instrução SQL e `/` sozinho na linha encerra bloco PL/SQL. Tudo usa o prefixo **`RC_`** porque o esquema da faculdade é compartilhado com outros trabalhos. Modelo e dicionário: [docs/modelo-de-dados.md](../../docs/modelo-de-dados.md).

> Dados **sintéticos** e protocolo **fictício**. Credenciais do banco nunca ficam no repositório (use o seu cliente SQL com o seu usuário).

## Ordem de execução

| Ordem | Arquivo | O que faz | Muda dados? |
|---|---|---|---|
| 1 | [`01_criar_esquema.sql`](01_criar_esquema.sql) | Cria as 12 tabelas `RC_*`, restrições e índices | Só cria; falha com ORA-00955 se já existir |
| 2 | [`02_carga_sintetica.sql`](02_carga_sintetica.sql) | Insere 3 contas, o protocolo v0.1, as 6 regras e 4 pedidos de exemplo | Só insere; bloco único, tudo ou nada |
| 3 | [`03_consultas_uso.sql`](03_consultas_uso.sql) | 6 consultas com junção e agregação | Somente leitura |
| 4 | [`04_testes_restricoes.sql`](04_testes_restricoes.sql) | Tenta 17 violações e confere que o banco recusa (os casos 13 e 14, da RC-07, exigem o motivo `PROTOCOLO_ALTERADO`; os casos 15 a 17, da RC-08, exigem as colunas do `07_`) | Não deixa dados (`ROLLBACK`) |
| 5 | [`05_sessao_login.sql`](05_sessao_login.sql) | (RC-04) Cria `RC_SESSAO_LOGIN`, onde a API guarda as sessões de login (só o hash do token) | Só cria; exige o `01_`; falha com ORA-00955 se já existir |
| 6 | [`06_motivo_revisao_protocolo.sql`](06_motivo_revisao_protocolo.sql) | (RC-07) Recria `RC_REVISAO_CK_MOTIVO` aceitando `PROTOCOLO_ALTERADO` (troca de serviço do rascunho). **Só para banco criado antes da RC-07**; o `01_` atual já traz o motivo. Sem ele a troca de serviço falha ao registrar a revisão (ORA-02290) | Aditivo: nenhuma linha muda; contém `DROP CONSTRAINT` seguido de `ADD CONSTRAINT` (se o `ADD` falhar, rode-o de novo antes de usar a API) |
| 7 | [`07_documento_tipo_e_tentativa.sql`](07_documento_tipo_e_tentativa.sql) | (RC-08) Acrescenta a `RC_DOCUMENTO` as colunas `TIPO_DOCUMENTAL`, `CHAVE_ENVIO` e `HASH_CONTEUDO`, mais duas restrições `CHECK` e a restrição única `(SOLICITACAO_ID, CHAVE_ENVIO)`. **Só para banco criado antes da RC-08**; o `01_` atual já traz as colunas. Sem ele, só o **envio** de documento falha ao gravar (ORA-00904 no log); pedidos, rascunhos e a troca de serviço seguem funcionando | Aditivo: só `ALTER TABLE … ADD`; as linhas da carga sintética recebem `TIPO_DOCUMENTAL = 'OUTRO'` pelo `DEFAULT` |
| 90 | [`90_limpeza_DESTRUTIVA.sql`](90_limpeza_DESTRUTIVA.sql) | **Apaga** as tabelas `RC_*` (as 12 da RC-03 e `RC_SESSAO_LOGIN`) | **DESTRUTIVO**; só roda com confirmação explícita |

Nenhum script, exceto o `90_`, contém `DROP TABLE`, `TRUNCATE` ou `DELETE`; o `06_` tem apenas `DROP CONSTRAINT` de uma restrição que ele recria na instrução seguinte, e o `07_` só acrescenta colunas e restrições. O `90_` nunca é chamado pelos outros e, por padrão, falha sem apagar nada (ORA-20001): para permitir, troque `'NAO'` por `'SIM'` na linha `<<CONFIRMACAO>>`. Ele cita as 13 tabelas `RC_*` (12 da RC-03 e `RC_SESSAO_LOGIN` da RC-04) uma a uma e não toca em `PEQUENOVAREJO`, `VENDA` ou qualquer outro objeto.

Para ver as saídas do `04_` e da carga, habilite a saída do servidor (SQL Developer: Exibir > Saída DBMS; SQLcl/SQL*Plus: `SET SERVEROUTPUT ON`).

## Segunda execução e recomeço

- Rodar `01_` de novo: cada `CREATE` falha com **ORA-00955** e nada é alterado.
- Rodar `02_` de novo: falha com **ORA-00001** (e-mail já cadastrado) e o bloco inteiro é desfeito; nada é duplicado.
- Rodar `05_` de novo: ORA-00955 e nada é alterado.
- Para recomeçar em ambiente de desenvolvimento: `90_` (com confirmação) → `01_` → `02_` → `05_`.

## Contas e senhas de demonstração

| Conta | Perfil |
|---|---|
| `paciente.a@exemplo.invalid` | `PACIENTE` |
| `paciente.b@exemplo.invalid` | `PACIENTE` |
| `admin@exemplo.invalid` | `ADMINISTRADOR` |

O domínio `.invalid` é reservado e nunca coincide com conta real. A coluna `SENHA_HASH` guarda **somente hash BCrypt** (versão `2a`, **custo 12**, 60 caracteres, formato `$2a$12$…`). Os hashes foram gerados com a biblioteca `at.favre.lib:bcrypt` 0.10.2, a mesma candidata para a API (RC-04), por um programa descartável fora do repositório, a partir da **senha de demonstração fictícia** `RegulaCheck#Demo2026`, usada apenas neste ambiente acadêmico e que **não é senha de ninguém**. Cada conta tem um hash distinto (sal próprio). Não reutilize essa senha em nenhum outro lugar. A compatibilidade com a biblioteca de verificação da API foi conferida na RC-04 por teste automatizado (`HashesRc03CompatibilidadeTest`), que lê os três hashes deste script e verifica a senha de demonstração, e pelo login real das três contas (ver registro em [../README.md](../README.md#verificação-registrada)).

## Dados da carga (contagens por tabela)

`RC_USUARIO` 3 · `RC_PROTOCOLO` 1 · `RC_REGRA` 6 · `RC_SOLICITACAO` 4 · `RC_REVISAO` 6 · `RC_DOCUMENTO` 8 · `RC_ANALISE` 5 · `RC_RESULTADO_REGRA` 30 · `RC_PENDENCIA` 8 · `RC_SESSAO_CAPTURA` 1 · `RC_LEITURA_SENSOR` 4 · `RC_SUBMISSAO` 1 (total 77).

| Pedido | Paciente | Estado | Revisões | Resumo |
|---|---|---|---|---|
| 1 | A | `SUBMETIDA_SIMULACAO` | 2 | Rev. 1 com pendência (R03) e não verificável (R02); rev. 2 sem pendências; submissão simulada |
| 2 | A | `COM_PENDENCIAS` | 1 | R04 e R05 pendentes (responsável: unidade emissora) |
| 3 | B | `REQUER_CONFERENCIA` | 2 | Imagem substituída; ainda há R03 não verificável |
| 4 | A | `RASCUNHO` | 1 | Sessão de captura com 4 leituras dos sensores simulados |

## Resultados esperados das consultas (`03_`)

1. **Pedidos por paciente**: Paciente A = 3; Paciente B = 1 (o administrador não aparece).
2. **Análises por pedido**: 5 linhas; pedidos 1 e 3 com revisões 1 e 2 (a 2 é a vigente); pedido 2 com a revisão 1; o rascunho não tem análise.
3. **Pedidos por estado**: `COM_PENDENCIAS` 1, `RASCUNHO` 1, `REQUER_CONFERENCIA` 1, `SUBMETIDA_SIMULACAO` 1.
4. **Pendências por regra** (total 8): R02 `NAO_VERIFICAVEL` 2; R03 `NAO_VERIFICAVEL` 2 e `PENDENTE` 1; R04 `PENDENTE` 1; R05 `PENDENTE` 1; R06 `NAO_VERIFICAVEL` 1.
5. **Pedidos corrigidos entre revisões**: 4 linhas (pedido 1: R02 e R03; pedido 3: R02 e R06, da revisão 1 para a 2). A R03 do pedido 3 continua pendente e não aparece.
6. **Resultado por regra da análise vigente do pedido 1**: R01 a R05 `ATENDE`; R06 `NAO_APLICAVEL`.

## Testes de restrições (`04_`)

Cada caso deve falhar com o código esperado e imprimir `OK`: perfil inválido, estado inválido, resultado de regra inválido (ORA-02290); e-mail duplicado, código de regra duplicado na versão, segunda análise vigente no pedido, segunda submissão (ORA-00001); referência inexistente, pendência sem resultado, pendência de tipo diferente do resultado (ORA-02291); documento acima de 5 MB e unidade incoerente com o sensor (ORA-02290). O script ainda confere que não restou nenhuma linha (total 77).

## Verificação registrada

Execuções reais no Oracle da faculdade (inclusive a do `05_`) estão em [../README.md](../README.md#verificação-registrada).
