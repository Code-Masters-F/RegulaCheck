# Pesquisa e decisões — RC-03

## D1. Ambiente
- **Fato verificado (12/11/2026):** Oracle Database 19c Enterprise Edition, AL32UTF8, usuário individual com CREATE TABLE e CREATE SEQUENCE e quota ilimitada; já existem `PEQUENOVAREJO` e `VENDA` de outros trabalhos.
- **Decisão:** esquema próprio com prefixo `RC_`; nenhum script referencia objetos sem esse prefixo. **Não** usar `DROP` fora do script `90_`.

## D2. Chaves e tipos
- **Decisão:** chaves primárias numéricas geradas com `NUMBER GENERATED ALWAYS AS IDENTITY` (19c), `VARCHAR2(n CHAR)` para texto, `TIMESTAMP WITH TIME ZONE` guardado em UTC (`SYSTIMESTAMP AT TIME ZONE 'UTC'`), `NUMBER(1)` com `CHECK (… IN (0,1))` para booleanos (Oracle 19c não tem BOOLEAN em SQL), `NUMBER(12,3)` para valores de sensor.
- **Rationale:** identity evita sequências avulsas; o 19c suporta identity e CHECK; tipos simples e portáveis para a JDBC futura.
- **Alternativas:** sequence + trigger (mais código, rejeitada); UUID `RAW(16)` (opaco para consultas de estudo).

## D3. Valores permitidos por CHECK
- Perfil: `PACIENTE`, `ADMINISTRADOR`. Estado: os 7 de `docs/api/estados-e-revisoes.md`. Resultado de regra: `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL`. Resultado geral: `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS`. Natureza da regra: `OBRIGATORIA`, `CONDICIONAL`. Responsável: `PACIENTE`, `UNIDADE_EMISSORA`. Sensor: `LUMINOSIDADE` (unidade `LUX`) e `DISTANCIA` (unidade `CM`). Tipo de documento: `PDF`, `JPEG`, `PNG`. Origem do resumo: `ADAPTADOR_MOCKADO`, `PADRAO`.
- **Rationale:** CHECK é simples, visível no dicionário e não exige tabela de domínio; mudar um conjunto exige novo script (aditivo).
- **Alternativas:** tabelas de domínio com FK (mais flexível, mais complexo; adiada).

## D4. Revisão como entidade
- **Decisão:** `RC_REVISAO(SOLICITACAO_ID, NUMERO)` com chave composta; `RC_SOLICITACAO.REVISAO_ATUAL` guarda o número vigente; `RC_ANALISE` referencia `(SOLICITACAO_ID, REVISAO)` por chave estrangeira composta, e `RC_DOCUMENTO` referencia a revisão em que foi enviado.
- **Histórico por revisão:** documento tem `REVISAO_ENVIO` e `REVISAO_REMOCAO` (nula enquanto ativo). O conjunto de documentos da revisão *n* é o de `REVISAO_ENVIO <= n` e (`REVISAO_REMOCAO IS NULL` ou `> n`). Assim substituir ou remover não apaga o histórico.
- **Rationale:** responde "o que mudou entre a revisão 1 e a 2" sem duplicar linhas.

## D5. Análise, resultado por regra e pendência
- **Decisão:** `RC_ANALISE` (uma por pedido e revisão, `UNIQUE(SOLICITACAO_ID, REVISAO)`), `RC_RESULTADO_REGRA(ANALISE_ID, REGRA_ID)` com chave composta, e `RC_PENDENCIA` apenas para resultados `PENDENTE` ou `NAO_VERIFICAVEL`, ligada por chave estrangeira composta ao resultado da mesma regra (impede pendência sem resultado correspondente).
- **Vigência única:** `VIGENTE NUMBER(1)` e índice único funcional `CREATE UNIQUE INDEX RC_ANALISE_VIGENTE_UK ON RC_ANALISE (CASE WHEN VIGENTE = 1 THEN SOLICITACAO_ID END)`; no máximo uma análise vigente por pedido.
- **Resumo:** colunas `RESUMO_TEXTO`, `RESUMO_ORIGEM` e `SIMULADO` (sempre 1 neste MVP) na própria análise; repetir o resumo atualiza a coluna, sem criar outra análise.
- **Consistência pendência × resultado (garantida no banco):** `RC_RESULTADO_REGRA` tem uma chave única extra (`ANALISE_ID`, `REGRA_ID`, `RESULTADO`); `RC_PENDENCIA` referencia essa chave por FK composta usando (`ANALISE_ID`, `REGRA_ID`, `TIPO`), com `CHECK (TIPO IN ('PENDENTE','NAO_VERIFICAVEL'))`. Assim a pendência só existe se o resultado da mesma regra na mesma análise for exatamente do mesmo tipo; pendência para resultado `ATENDE` ou `NAO_APLICAVEL`, ou sem resultado, é rejeitada. O teste verifica as duas violações.

## D6. Sensores
- **Decisão:** `RC_SESSAO_CAPTURA` (ligada ao pedido) e `RC_LEITURA_SENSOR` (tipo, valor, unidade, `MEDIDA_EM`, `RECEBIDA_EM`). CHECK amarra a unidade ao tipo. Limites demonstrativos e classificação de "antiga" ficam na API.

## D7. Submissão simulada
- **Decisão:** `RC_SUBMISSAO` com `UNIQUE(SOLICITACAO_ID)` e `UNIQUE(REFERENCIA)`, ligada à análise vigente usada; `SIMULADA` sempre 1.

## D8. Senhas
- **Decisão:** BCrypt com custo 12, hash de 60 caracteres no formato `$2a$12$…`, coluna `SENHA_HASH VARCHAR2(100)`. Biblioteca candidata da API: `at.favre.lib:bcrypt` 0.10.2 (consolidada, sem dependências pesadas). Os hashes da carga são gerados com a mesma biblioteca num programa descartável fora do repositório; a senha de demonstração (fictícia) é documentada no README dos scripts apenas para uso em ambiente acadêmico e **não** é uma senha real de ninguém.
- **Alternativas:** Argon2 (também aceito pela constituição; exige parâmetros de memória e dependência nativa); mantido como possibilidade para a RC-04 se o grupo preferir, bastando `VARCHAR2(100)` comportar o formato PHC.

## D9. Carga e idempotência
- **Decisão:** a carga é **um único bloco PL/SQL** (variáveis guardam os IDs gerados por identity para ligar filhos aos pais, `COMMIT` só no final). Rodar duas vezes **falha** na primeira unicidade (e-mail) e o bloco inteiro sofre `ROLLBACK`, sem duplicar nem corromper nada, mesmo em clientes que continuariam após erro. Esse é o comportamento previsível documentado (SC-006). Dados de exemplo usam e-mails fictícios `paciente.a@exemplo.invalid`, `paciente.b@exemplo.invalid`, `admin@exemplo.invalid` (domínio `.invalid` reservado).

## D10. Consultas de uso (6)
1. Pedidos por paciente (JOIN usuário × solicitação, `COUNT`).
2. Análises por pedido (JOIN solicitação × análise × protocolo, ordem de revisão).
3. Pedidos por estado (`GROUP BY ESTADO`).
4. Pendências por regra (JOIN pendência × regra, `COUNT`, `GROUP BY`).
5. Pedidos corrigidos entre revisões (pendência na revisão *n* sem pendência da mesma regra na revisão seguinte, com `NOT EXISTS`).
6. (extra) Resultado por regra de uma análise vigente com seu responsável, útil para a tela de resultado.

## D11. Execução e verificação
- **Decisão:** executar os scripts em ordem 01 → 02 → 03 → 04 no Oracle da faculdade usando um executor temporário em Java (fora do repositório) que separa instruções por `;` e blocos PL/SQL por `/`, registrando a saída. Em seguida, `90_` para provar que remove só `RC_` e recria sem perda (não toca em `PEQUENOVAREJO`/`VENDA`, conferidos antes e depois).
- **Rationale:** é o mesmo que o grupo faria no SQL Developer; a saída real vira evidência.
- **Risco:** banco compartilhado. Mitigação: só objetos `RC_`, usuário individual, conferência prévia da lista de tabelas.

## D12. Convenção de nomes
- **Decisão:** `RC_NOME_DA_TABELA` em maiúsculas com sublinhado (Oracle é case-insensitive sem aspas; nada de identificadores entre aspas). Restrições: `RC_<TABELA>_PK`, `_FK_<REF>`, `_UK`, `_CK_<CAMPO>`; índices `RC_<TABELA>_IX_<CAMPO>`. Abreviar para respeitar 30 caracteres quando necessário.
