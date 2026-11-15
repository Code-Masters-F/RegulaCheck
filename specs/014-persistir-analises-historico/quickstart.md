# Quickstart: verificar a RC-13

## Automatizado (sem banco)

A partir de `backend/` (o `mvnw` está no Git sem permissão de execução, por isso `sh ./mvnw`; no Windows, `.\mvnw.cmd`):

```bash
sh ./mvnw -q test -Dtest='br.com.regulacheck.analise.**'    # análises (serviço, rota, concorrência, prazo, falha)
sh ./mvnw clean verify                                       # tudo
```

Os testes usam a API real (Jetty em porta livre) sobre repositórios em memória e o leitor simulado com as amostras sintéticas. O `synchronized` dos dobros em memória faz o papel do `SELECT … FOR UPDATE`: os testes de concorrência provam a lógica do serviço, **não** o bloqueio de linha do Oracle.

## Oracle (opt-in)

Pré-requisitos: `01_`…`09_` aplicados e **`10_analise_historico.sql`** executado (bancos criados antes da RC-13). Depois:

```bash
export DB_URL=… DB_USERNAME=… DB_PASSWORD=… RC_TESTE_ORACLE=SIM
sh ./mvnw -q test -Dtest=AnaliseOracleTest
```

Sem `RC_TESTE_ORACLE=SIM` o teste é ignorado e deve ser registrado como **NÃO VERIFICADO**. Também rodar o `04_testes_restricoes.sql` (casos novos 28 a 31) no SQL Developer.

## Percurso manual (API)

1. `POST /auth/login` como `paciente.a@exemplo.invalid`.
2. `POST /solicitacoes` → `POST …/documentos` (encaminhamento e identificação sintéticos de `samples/`) → `POST …/extracoes`.
3. `POST …/analises` **sem conferir** → 201, `resultadoGeral: REQUER_CONFERENCIA`, pedido em `REQUER_CONFERENCIA`, pendências com motivo `TRANSCRICAO_NAO_CONFERIDA` (R06: `CONDICAO_INDETERMINADA`).
4. `POST …/analises` de novo → 200 com a mesma análise.
5. `PUT …/extracoes/{extracaoId}/conferencia` confirmando todos os campos identificados → `GET …/analises`: a análise anterior com `vigente: false` e sem `vigenteId`.
6. `POST …/analises` → 201, `SEM_PENDENCIAS_IDENTIFICADAS`; `GET …/analises` mostra as duas (mesma `revisao`, `conferenciaConsiderada` diferentes), só a nova vigente.
7. `PUT …/documentos/{docId}` (substituir) → `GET /solicitacoes/{id}`: `analise.situacao = NAO_REALIZADA`, estado mantido; `GET …/analises`: nenhuma vigente.

## Gabarito independente

| Cenário | Esperado |
|---|---|
| Amostras oficiais lidas, nada confirmado | 201, `REQUER_CONFERENCIA`, 6 resultados, 6 pendências `NAO_VERIFICAVEL` com responsável `PACIENTE`: motivo `TRANSCRICAO_NAO_CONFERIDA` em R01–R05 e `CONDICAO_INDETERMINADA` em R06 |
| Amostras oficiais lidas, tudo confirmado | 201, `SEM_PENDENCIAS_IDENTIFICADAS`, R06 `NAO_APLICAVEL`, 0 pendências |
| Repetir sem mudança | 200, mesma `id`, nenhuma análise nova |
| 10 pedidos simultâneos | 1 análise gravada; respostas 201/200 (mesma `id`) ou 409 `ANALISE_EM_ANDAMENTO` |
| Documento substituído durante a avaliação | 409 `CONFLITO_ESTADO`, 0 análises novas, estado = o da última análise concluída ou `RASCUNHO` |
| Decisão de conferência durante a avaliação | idem |
| Avaliação lança `ProtocoloNaoSuportadoException` | 503 `ANALISE_INDISPONIVEL`, `FALHA_ANALISE`, 0 análises; nova tentativa sem a falha → 201 |
| `PROCESSANDO` há 61 s | `GET …/analises` → `execucao.situacao = INTERROMPIDA`; `POST` → 201; o resultado tardio do anterior é descartado |
| `PROCESSANDO` há 10 s | `POST` → 409 `ANALISE_EM_ANDAMENTO` com `Retry-After: 50` |
| Pedido da paciente B | 404 idêntico ao de pedido inexistente (POST e GET) |
| Pedido `SUBMETIDA_SIMULACAO` | `POST` 409; `GET` 200 |
