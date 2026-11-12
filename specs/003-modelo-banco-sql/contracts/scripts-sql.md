# Contrato dos scripts SQL (RC-03)

Todos em `database/scripts/`, para Oracle 19c, com cabeçalho em comentário (finalidade, ordem, efeito, pré-requisitos). Instruções SQL terminam em `;`; blocos PL/SQL terminam em `/` sozinho na linha. Nenhum script contém credenciais, `DROP`, `TRUNCATE` ou `DELETE` — exceto o `90_`.

| Ordem | Arquivo | Efeito | Seguro? |
|---|---|---|---|
| 1 | `01_criar_esquema.sql` | Cria tabelas `RC_*`, restrições e índices. Roda em esquema sem objetos `RC_`; se já existirem, falha com ORA-00955 sem alterar nada | Aditivo |
| 2 | `02_carga_sintetica.sql` | Insere contas, protocolo, regras e dados de exemplo em **um único bloco PL/SQL**, com `COMMIT` só no fim. Segunda execução falha por unicidade (ORA-00001) e o bloco inteiro sofre `ROLLBACK`: nada é duplicado | Aditivo |
| 3 | `03_consultas_uso.sql` | 6 `SELECT`s com JOIN e agregação | Somente leitura |
| 4 | `04_testes_restricoes.sql` | Tenta inserções inválidas dentro de blocos PL/SQL; cada violação é capturada e o bloco imprime `OK` ou `FALHOU`; termina com `ROLLBACK` | Não deixa dados |
| 90 | `90_limpeza_DESTRUTIVA.sql` | Remove somente objetos `RC_*`; só executa se a variável de confirmação for `SIM` | **Destrutivo** |

## Garantias de segurança
- Só toca objetos com prefixo `RC_`; nunca `PEQUENOVAREJO`, `VENDA` ou objetos de outros projetos.
- `90_` não é chamado por nenhum outro script e exige confirmação explícita no próprio arquivo/ao executar.
- Dados fictícios: e-mails `*.invalid`, nomes genéricos, documentos sintéticos.

## Carga mínima esperada (resultado de contagem após `02_`)
RC_USUARIO 3 · RC_PROTOCOLO 1 · RC_REGRA 6 · RC_SOLICITACAO 4 · RC_REVISAO 6 · RC_DOCUMENTO 8 · RC_ANALISE 5 · RC_RESULTADO_REGRA 30 · RC_PENDENCIA 8 · RC_SESSAO_CAPTURA 1 · RC_LEITURA_SENSOR 4 · RC_SUBMISSAO 1 (valores definitivos conferidos em execução real e documentados em `database/scripts/README.md`).

## Consultas (03) e resultado esperado
1. **Pedidos por paciente**: lista cada paciente e a quantidade de pedidos (A e B com contagens diferentes; administrador não aparece).
2. **Análises por pedido**: por pedido, revisão, resultado geral, versão do protocolo e vigência, em ordem de revisão.
3. **Pedidos por estado**: contagem por estado presente na carga.
4. **Pendências por regra**: contagem por código de regra e responsável.
5. **Pedidos corrigidos entre revisões**: pedido, regra e revisões em que a pendência existia numa revisão e não na seguinte.
6. **Resultado por regra da análise vigente**: regra, resultado e responsável para a análise vigente de um pedido.

Os valores exatos esperados são fixados na carga final e registrados em `database/scripts/README.md` e no registro de verificação.

## Testes de restrições (04)
Cada caso deve falhar com o código ORA indicado: perfil inválido (ORA-02290), estado inválido (ORA-02290), resultado de regra inválido (ORA-02290), e-mail duplicado (ORA-00001), código de regra duplicado na mesma versão (ORA-00001), referência a solicitação inexistente (ORA-02291), duas análises vigentes no mesmo pedido (ORA-00001), segunda submissão do mesmo pedido (ORA-00001), documento acima de 5 MB (ORA-02290), unidade incoerente com o tipo de sensor (ORA-02290), pendência sem resultado correspondente (ORA-02291) e pendência cujo tipo não coincide com o resultado da regra, por exemplo pendência para resultado `ATENDE` (ORA-02291). Total: 12 casos.
