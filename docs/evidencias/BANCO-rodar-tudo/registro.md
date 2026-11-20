# BANCO-rodar-tudo — Execução limpa do script SQL do início ao fim

Tipo de execução: Oracle real da faculdade, esquema do grupo (tabelas `RC_*`), executor JDBC mínimo que imita o SQL Developer (`@@`, `PROMPT`, `;` e `/`). Não é caso numerado do plano de testes.

| Item | Valor |
|---|---|
| Data | 2026-11-19 |
| Versão | `database/scripts/rodar_tudo.sql` (alteração local ainda não commitada) |
| Responsável | grupo CodeMasters · executado com apoio do Claude Code |
| Resultado bruto | `bruto/01-rodar_tudo.txt`, `bruto/02-04_testes_restricoes.txt`, `bruto/03-consultas-resultado.txt` |

## Passos
1. Antes: 18 tabelas `RC_*` no esquema. Limpeza com uma cópia temporária de `90_limpeza_DESTRUTIVA.sql` com a confirmação `SIM` (o arquivo do repositório continua com `NAO`): 18 tabelas removidas, 0 restantes.
2. `rodar_tudo.sql` (chama `01_`, `02_` e `05_`).
3. Contagem de linhas por tabela.
4. `04_testes_restricoes.sql` e `03_consultas_uso.sql`.
5. API reiniciada sobre o banco recriado: `/health` UP e login das 3 contas de demonstração (HTTP 200).

## Esperado × obtido
- ✔ `rodar_tudo.sql` do zero: 33 instruções executadas, **0 erros**, 18 tabelas `RC_*` criadas.
- ✔ Carga: 77 linhas, iguais às contagens do README de `database/scripts` (`RC_USUARIO` 3, `RC_PROTOCOLO` 1, `RC_REGRA` 6, `RC_SOLICITACAO` 4, `RC_REVISAO` 6, `RC_DOCUMENTO` 8, `RC_ANALISE` 5, `RC_RESULTADO_REGRA` 30, `RC_PENDENCIA` 8, `RC_SESSAO_CAPTURA` 1, `RC_LEITURA_SENSOR` 4, `RC_SUBMISSAO` 1).
- ✔ `04_testes_restricoes.sql`: 34 OK, 0 FALHOU, e o total volta a 77 linhas (sem sobras).
- ✔ `03_consultas_uso.sql`: as 6 consultas executam sem erro e os resultados reais (`bruto/03-consultas-resultado.txt`) coincidem com o "Esperado" de cada consulta no próprio script (3/1 pedidos por paciente; 5 análises; 1 pedido por estado; pendências por regra; 4 linhas de regras que mudaram da revisão 1 para a 2; 6 regras da última análise).
- ✔ API sobre o banco recriado: `/health` UP; login de `paciente.a`, `paciente.b` e `admin` com HTTP 200; `GET /solicitacoes` da paciente A devolve o pedido da carga.

## Observações
- A limpeza apagou os dados criados nas execuções de 19/11 (por exemplo o pedido 153 e a sessão de captura 66). As evidências em `docs/evidencias/T*/` e `IOT-simulacao/` guardam as respostas brutas daquela execução e continuam válidas como registro; os identificadores citados nelas não existem mais no banco.

## Status final
Aprovado — grupo CodeMasters (com apoio do Claude Code) — 2026-11-19.
