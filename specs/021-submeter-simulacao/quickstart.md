# Quickstart: Submeter solicitação em simulação (RC-17)

## Build completo

A partir de `backend/` (o `mvnw` está versionado sem bit de execução; use `sh ./mvnw`):

```bash
sh ./mvnw clean verify
```

Referência antes da RC-17 (17/11/2026, branch `39-rc-17-…` sobre a `main` com a RC-16): 731 testes, 0 falhas, 32 ignorados (Oracle opt-in), `BUILD SUCCESS`.

## Testes focados

```bash
sh ./mvnw test -Dtest='ReferenciaSimuladaTest,SubmeterEndpointTest,RecusasDaSubmissaoTest,MatrizPerfisSubmissaoTest,HigieneDeLogSubmissaoTest'
```

## Percurso manual (API com Oracle e `DOCUMENT_STORAGE_PATH`)

1. Criar o rascunho, enviar as amostras oficiais de `samples/`, ler (`POST …/extracoes`), confirmar todos os campos e analisar (`POST …/analises`) → `SEM_PENDENCIAS_IDENTIFICADAS`.
2. `POST …/submissoes-simuladas` → 201, comprovante `SIM-2026-…` com o destaque; o pedido vai a `SUBMETIDA_SIMULACAO`.
3. `POST …/submissoes-simuladas` de novo → 200, o mesmo comprovante.
4. `GET …/submissoes-simuladas` → 200, o mesmo comprovante.
5. `POST …/documentos` ou `POST …/reenvio` → 409 "Este pedido já foi submetido e só pode ser consultado."
6. Num pedido com pendência (encaminhamento sem motivo), `POST …/submissoes-simuladas` → 409 com a mensagem de restrição do simulador.

## Oracle (opt-in)

Sem script SQL novo (`RC_SUBMISSAO` existe desde a RC-03). `SubmissaoOracleTest` exige `RC_TESTE_ORACLE=SIM` e `DB_*`, e só deve rodar com autorização para usar a conta compartilhada (executado em 17/11/2026 com autorização: 3 testes verdes, ver `tasks.md`):

```bash
(set -a; . ./.env; set +a; cd backend && RC_TESTE_ORACLE=SIM sh ./mvnw test -Dtest=SubmissaoOracleTest)
```
