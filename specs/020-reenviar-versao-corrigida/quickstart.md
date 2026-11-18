# Quickstart: Reenviar e conferir a versão corrigida (RC-16)

Verificação local, sem banco, com repositórios em memória e leitor simulado (mesma base dos testes da RC-13/RC-14).

## Build completo

A partir de `backend/` (o `mvnw` está versionado sem bit de execução; `sh ./mvnw` funciona em qualquer caso):

```bash
sh ./mvnw clean verify
```

Referência antes da RC-16 (17/11/2026): 695 testes, 0 falhas, 31 ignorados (Oracle opt-in), `BUILD SUCCESS`.

## Testes focados da RC-16

```bash
sh ./mvnw test -Dtest='ComparacaoDeAnalisesTest,ComparacaoEndpointTest,ReenvioEndpointTest,FalhasDoReenvioTest,InclusaoAposAnaliseTest,EnviarDocumentoEndpointTest,PedidoSubmetidoSomenteConsultaTest,MatrizPerfisReenvioTest,HigieneDeLogReenvioTest'
```

## Percurso manual (API em execução com Oracle e `DOCUMENT_STORAGE_PATH`)

1. `POST /solicitacoes` → rascunho; enviar `samples/` encaminhamento sem motivo + identificação (`POST …/documentos`).
2. `POST …/extracoes`, confirmar todos os campos (`PUT …/extracoes/{id}/conferencia`), `POST …/analises` → `COM_PENDENCIAS` (RC-R04), sem `comparacao`.
3. `PUT …/documentos/{docId}` com o encaminhamento completo → revisão + 1; `GET …/analises` mostra a análise como não vigente e **nenhum** item `RESOLVIDA`.
4. `POST …/reenvio` → 200 `etapa: CONFERENCIA` (leitura nova, campos sem decisão).
5. Confirmar os campos da nova leitura; `POST …/reenvio` → 201 `etapa: CONCLUIDA`, `analise.resultadoGeral = SEM_PENDENCIAS_IDENTIFICADAS`, resumo `SIMULADO`, `comparacao.itens = [RC-R04 RESOLVIDA]`.
6. `POST …/reenvio` de novo → 200, mesma análise.
7. Documento ausente: pedido só com encaminhamento → análise com RC-R03 pendente → `POST …/documentos` com a identificação (aceito após a análise) → passos 4–5 → RC-R03 `RESOLVIDA`.

## Oracle (opt-in)

Sem script SQL novo. A inclusão após a análise muda o caminho JDBC (`DocumentoRepositoryJdbc.registrar` passa a invalidar a análise vigente): coberto por teste opt-in (`RC_TESTE_ORACLE=SIM` + `DB_*`), executado **somente com autorização** para usar a conta compartilhada do grupo. Executado em 17/11/2026 com autorização: `ReenvioOracleTest` e `AnaliseOracleTest`, 3 testes verdes (ver `tasks.md`).
