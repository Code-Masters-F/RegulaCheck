# Scripts SQL do RegulaCheck (RC-03)

Scripts para **Oracle 19c** (SQL Developer, SQLcl ou SQL*Plus). `;` encerra instrução SQL e `/` sozinho na linha encerra bloco PL/SQL. Execute sempre o **arquivo inteiro como script**: no SQL Developer, **F5** ("Executar script"); no DBeaver, **Alt+X** ("Executar script SQL"). **Ctrl+Enter** executa uma instrução só: com o arquivo inteiro selecionado, o Oracle recebe vários comandos de uma vez e recusa já o primeiro (ex.: ORA-01735 no `10_`, sem aplicar nada). Tudo usa o prefixo **`RC_`** porque o esquema da faculdade é compartilhado com outros trabalhos. Modelo e dicionário: [docs/modelo-de-dados.md](../../docs/modelo-de-dados.md).

> Dados **sintéticos** e protocolo **fictício**. Credenciais do banco nunca ficam no repositório (use o seu cliente SQL com o seu usuário).

## Ordem de execução

| Ordem | Arquivo | O que faz | Muda dados? |
|---|---|---|---|
| 1 | [`01_criar_esquema.sql`](01_criar_esquema.sql) | Cria as 17 tabelas `RC_*` (12 da RC-03, as 3 de extração da RC-10, `RC_CONFERENCIA_CAMPO` da RC-11 e `RC_PENDENCIA_EVIDENCIA` da RC-13), restrições e índices, já com as colunas da RC-13 | Só cria; falha com ORA-00955 se já existir |
| 2 | [`02_carga_sintetica.sql`](02_carga_sintetica.sql) | Insere 3 contas, o protocolo v0.1, as 6 regras e 4 pedidos de exemplo | Só insere; bloco único, tudo ou nada |
| 3 | [`03_consultas_uso.sql`](03_consultas_uso.sql) | 6 consultas com junção e agregação | Somente leitura |
| 4 | [`04_testes_restricoes.sql`](04_testes_restricoes.sql) | Tenta 33 casos (31 violações e 2 da RC-21) e confere que o banco recusa (os casos 13 e 14, da RC-07, exigem o motivo `PROTOCOLO_ALTERADO`; os casos 15 a 17, da RC-08, exigem as colunas do `07_`; os casos 18 a 22, da RC-10, exigem as tabelas do `08_`; os casos 23 a 27, da RC-11, exigem a tabela do `09_`; os casos 28 a 31, da RC-13, e a contagem final exigem o `10_`; os casos 32 e 33, da RC-21, exigem o índice do `11_`) | Não deixa dados (`ROLLBACK`) |
| 5 | [`05_sessao_login.sql`](05_sessao_login.sql) | (RC-04) Cria `RC_SESSAO_LOGIN`, onde a API guarda as sessões de login (só o hash do token) | Só cria; exige o `01_`; falha com ORA-00955 se já existir |
| 6 | [`06_motivo_revisao_protocolo.sql`](06_motivo_revisao_protocolo.sql) | (RC-07) Recria `RC_REVISAO_CK_MOTIVO` aceitando `PROTOCOLO_ALTERADO` (troca de serviço do rascunho). **Só para banco criado antes da RC-07**; o `01_` atual já traz o motivo. Sem ele a troca de serviço falha ao registrar a revisão (ORA-02290) | Aditivo: nenhuma linha muda; contém `DROP CONSTRAINT` seguido de `ADD CONSTRAINT` (se o `ADD` falhar, rode-o de novo antes de usar a API) |
| 7 | [`07_documento_tipo_e_tentativa.sql`](07_documento_tipo_e_tentativa.sql) | (RC-08) Acrescenta a `RC_DOCUMENTO` as colunas `TIPO_DOCUMENTAL`, `CHAVE_ENVIO` e `HASH_CONTEUDO`, mais duas restrições `CHECK` e a restrição única `(SOLICITACAO_ID, CHAVE_ENVIO)`. **Só para banco criado antes da RC-08**; o `01_` atual já traz as colunas. Sem ele, só o **envio** de documento falha ao gravar (ORA-00904 no log); pedidos, rascunhos e a troca de serviço seguem funcionando | Aditivo: só `ALTER TABLE … ADD`; as linhas da carga sintética recebem `TIPO_DOCUMENTAL = 'OUTRO'` pelo `DEFAULT` |
| 8 | [`08_extracao.sql`](08_extracao.sql) | (RC-10) Cria `RC_EXTRACAO`, `RC_EXTRACAO_DOCUMENTO` e `RC_EXTRACAO_CAMPO`, onde a API guarda a leitura **simulada** dos documentos de um pedido. **Só para banco criado antes da RC-10**; o `01_` atual já cria as três. Sem ele, só a rota de extração responde 503 (ORA-00942 no log); o resto da API, inclusive excluir rascunho, segue funcionando | Aditivo: só `CREATE TABLE`/`CREATE INDEX`; nenhuma tabela existente é alterada; falha com ORA-00955 se já existir |
| 9 | [`09_conferencia.sql`](09_conferencia.sql) | (RC-11) Cria `RC_CONFERENCIA_CAMPO`, onde a API guarda a decisão vigente da paciente sobre cada campo lido (confirmar ou apontar divergência; **sem coluna de valor digitado**). **Só para banco criado antes da RC-11**; o `01_` atual já cria a tabela. Sem ele, só a rota de conferência responde 503 (ORA-00942 no log); o resto da API, inclusive a leitura e excluir rascunho, segue funcionando | Aditivo: só `CREATE TABLE`; nenhuma tabela existente é alterada; falha com ORA-00955 se já existir |
| 10 | [`10_analise_historico.sql`](10_analise_historico.sql) | (RC-13) Prepara a gravação das análises pedidas pela paciente: `RC_SOLICITACAO.ANALISE_INICIADA_EM` (início da análise em andamento, só em `PROCESSANDO`), `RC_ANALISE.CONFERENCIA_EM` e `DATA_REFERENCIA`, `RC_PENDENCIA.MOTIVO`, a tabela `RC_PENDENCIA_EVIDENCIA` e a troca de `RC_ANALISE_UK_REVISAO` por `RC_ANALISE_UK_CONFERENCIA` (mais de uma análise por revisão quando a conferência muda). **Só para banco criado antes da RC-13**; o `01_` atual já traz tudo. Sem ele, só as rotas de análise respondem 503 (ORA-00904/ORA-00942 no log); o resto da API segue funcionando | Aditivo: nenhuma linha muda; contém `DROP CONSTRAINT RC_ANALISE_UK_REVISAO` seguido do `ADD` da restrição nova (se o `ADD` falhar, rode-o de novo antes de usar a API) |
| 11 | [`11_iot_sessao_captura.sql`](11_iot_sessao_captura.sql) | (RC-21) Cria o índice único `RC_SESSAO_CAPTURA_UK_ABERTA`: no máximo uma sessão de captura aberta por pedido (sessões encerradas ficam fora). **Só para banco criado antes da RC-21**; o `01_` atual já cria o índice. Sem ele, a API continua funcionando (a abertura já é serializada pelo bloqueio do pedido); só falta a garantia no banco | Aditivo: só `CREATE UNIQUE INDEX`; falha com ORA-00955 se já existir e com ORA-01452 se algum pedido tiver duas sessões abertas |
| 90 | [`90_limpeza_DESTRUTIVA.sql`](90_limpeza_DESTRUTIVA.sql) | **Apaga** as tabelas `RC_*` (as 12 da RC-03, `RC_SESSAO_LOGIN`, as 3 `RC_EXTRACAO*`, `RC_CONFERENCIA_CAMPO` e `RC_PENDENCIA_EVIDENCIA`) | **DESTRUTIVO**; só roda com confirmação explícita |

O `08_` e o `09_` só criam tabelas novas; o `10_` acrescenta colunas, restrições e uma tabela, e troca uma restrição única por outra mais permissiva; o `11_` só cria um índice único. Nenhum script, exceto o `90_`, contém `DROP TABLE`, `TRUNCATE` ou `DELETE`; o `06_` tem apenas `DROP CONSTRAINT` de uma restrição que ele recria na instrução seguinte, e o `07_` só acrescenta colunas e restrições. O `90_` nunca é chamado pelos outros e, por padrão, falha sem apagar nada (ORA-20001): para permitir, troque `'NAO'` por `'SIM'` na linha `<<CONFIRMACAO>>`. Ele cita as 18 tabelas `RC_*` (12 da RC-03, `RC_SESSAO_LOGIN` da RC-04, as 3 `RC_EXTRACAO*` da RC-10, `RC_CONFERENCIA_CAMPO` da RC-11 e `RC_PENDENCIA_EVIDENCIA` da RC-13) uma a uma e não toca em `PEQUENOVAREJO`, `VENDA` ou qualquer outro objeto.

Para ver as saídas do `04_` e da carga, habilite a saída do servidor (SQL Developer: Exibir > Saída DBMS; SQLcl/SQL*Plus: `SET SERVEROUTPUT ON`).

## Segunda execução e recomeço

- Rodar `01_` de novo: cada `CREATE` falha com **ORA-00955** e nada é alterado.
- Rodar `02_` de novo: falha com **ORA-00001** (e-mail já cadastrado) e o bloco inteiro é desfeito; nada é duplicado.
- Rodar `05_` de novo: ORA-00955 e nada é alterado.
- Para recomeçar em ambiente de desenvolvimento: `90_` (com confirmação) → `01_` → `02_` → `05_` (o `01_` atual já traz o que os `06_` a `11_` acrescentam) e, se quiser conferir as restrições, `04_`.
- **Banco já existente** (criado antes de alguma história): aplique primeiro os scripts aditivos que faltarem, na ordem (`06_` a `11_`), e só **depois** rode o `04_`, porque os casos dele usam as colunas e tabelas desses scripts.

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

Cada caso deve falhar com o código esperado e imprimir `OK`: perfil inválido, estado inválido, resultado de regra inválido (ORA-02290); e-mail duplicado, código de regra duplicado na versão, segunda análise vigente no pedido, segunda submissão (ORA-00001); referência inexistente, pendência sem resultado, pendência de tipo diferente do resultado (ORA-02291); documento acima de 5 MB e unidade incoerente com o sensor (ORA-02290). Desde a RC-13: `PROCESSANDO` sem início da análise (ORA-02290), duas análises da mesma revisão e conferência (ORA-00001), pendência `PENDENTE` com motivo e evidência com situação fora da lista (ORA-02290). O script ainda confere que não restou nenhuma linha (total 77).

## Verificação registrada

Execuções reais no Oracle da faculdade (inclusive a do `05_`) estão em [../README.md](../README.md#verificação-registrada).
