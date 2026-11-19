# Inventário documental (feature 025)

Gerado de `git ls-files` em 19/11/2026, excluindo `specs/`, `.specify/`, `.agents/`, `.claude/` e código-fonte (protegidos, tratados só quanto a links). Destino segue o critério D2 de [research.md](research.md).

## Temas e fonte de verdade (resultado da leitura)

| Tema | Fonte de verdade | Observação |
|---|---|---|
| Rotas, códigos HTTP, formatos | `docs/api/openapi.yaml`, `contratos-dados.md`, `erros.md` | `CONTRATO_API.md` virou mapa de rotas e perfis |
| Situação e verificação por história | `docs/api/README.md` (Situação das rotas) | removido o parágrafo duplicado de `CONTRATO_API.md` |
| Perfil por rota | `CONTRATO_API.md` (tabela) e `docs/api/autorizacao.md` | tabela mantida: é o contrato das próximas rotas |
| Cenários de teste | `docs/testes/plano-de-testes.md` | `CONTRATO_TESTES.md` mantido como origem (issue #50) |
| Resultados de teste | `docs/testes/resultado-da-rodada.md`, `docs/evidencias/` | preservados |
| Ambiente e comandos | `docs/ambiente.md` | sem cópia divergente encontrada |
| Decisões | `docs/decisoes.md` | decisão da feature registrada |
| Modelo de dados e SQL | `docs/modelo-de-dados.md`, `database/scripts/README.md` | sem sobreposição relevante encontrada |
| Design e telas | `mobile/design/*`, `design/README.md` | caminho pessoal removido de `design/README.md` |

## Divergências encontradas (T008)

- `CONTRATO_API.md` citava `POST /solicitacoes/{id}/analises/{analiseId}/resumo`; a rota real e o `openapi.yaml` usam `POST /solicitacoes/{id}/resumo`. Corrigido.
- `CONTRATO_API.md` não listava `POST /solicitacoes/{id}/reenvio` (RC-16), presente no `openapi.yaml` e em `ReenvioController`. Corrigido.
- `docs/api/README.md` dizia que "todas as demais rotas" eram planejadas; só `GET /admin/indicadores` não está no `openapi.yaml`. Corrigido.
- Não conferido linha a linha contra o código: demais códigos HTTP e limites (assumidos conforme `openapi.yaml`, que não foi alterado).

## Documentos

| Caminho | Tipo | Protegido | Destino | Estado |
|---|---|---|---|---|
| `AGENTS.md` | instrução de agente | não | manter (sem desvio detectado) | verificado |
| `CLAUDE.md` | instrução de agente | não | manter (sem desvio detectado) | verificado |
| `CONTRATO_API.md` | referência | não | consolidado (mapa de rotas; situação por RC removida, aponta para docs/api/README.md) | verificado |
| `CONTRATO_TESTES.md` | referência | não | normalizado (H1 e propósito; mantido como origem T01–T16, conforme D2/I2) | verificado |
| `README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `backend/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `database/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `database/scripts/01_criar_esquema.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/02_carga_sintetica.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/03_consultas_uso.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/04_testes_restricoes.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/05_sessao_login.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/06_motivo_revisao_protocolo.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/07_documento_tipo_e_tentativa.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/08_extracao.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/09_conferencia.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/10_analise_historico.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/11_iot_sessao_captura.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/90_limpeza_DESTRUTIVA.sql` | SQL | sim | manter (protegido; só links) | verificado |
| `database/scripts/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `design/README.md` | índice/guia | não | normalizado (caminho pessoal removido) | verificado |
| `design/verificacao-2026-11-13.md` | design | não | manter (sem desvio detectado) | verificado |
| `docs/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `docs/ambiente.md` | referência | não | manter (sem desvio detectado) | verificado |
| `docs/api/README.md` | índice/guia | não | normalizado (rota planejada e rótulo corrigidos) | verificado |
| `docs/api/RegulaCheck.postman_collection.json` | contrato | não | manter (sem desvio detectado) | verificado |
| `docs/api/autorizacao.md` | contrato | não | manter (sem desvio detectado) | verificado |
| `docs/api/contratos-dados.md` | contrato | não | manter (sem desvio detectado) | verificado |
| `docs/api/erros.md` | contrato | não | manter (sem desvio detectado) | verificado |
| `docs/api/estados-e-revisoes.md` | contrato | não | manter (sem desvio detectado) | verificado |
| `docs/api/openapi.yaml` | contrato | não | manter (sem desvio detectado) | verificado |
| `docs/api/protecoes.md` | contrato | não | manter (sem desvio detectado) | verificado |
| `docs/arquitetura.md` | referência | não | manter (sem desvio detectado) | verificado |
| `docs/decisoes.md` | referência | não | decisão da feature registrada | verificado |
| `docs/escopo-e-limitacoes.md` | referência | não | manter (sem desvio detectado) | verificado |
| `docs/evidencias/2026-11-17_rc18-app-android.md` | evidência | sim | manter (protegido; só links) | verificado |
| `docs/evidencias/2026-11-17_rc18-roteiro-manual.md` | evidência | sim | manter (protegido; só links) | verificado |
| `docs/evidencias/2026-11-17_rc19-app-documentos-resultado.md` | evidência | sim | manter (protegido; só links) | verificado |
| `docs/evidencias/README.md` | evidência | sim | manter (protegido; só links) | verificado |
| `docs/modelo-de-dados.md` | referência | não | manter (sem desvio detectado) | verificado |
| `docs/protocolo-academico.md` | referência | não | manter (sem desvio detectado) | verificado |
| `docs/testes/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `docs/testes/plano-de-testes.md` | teste | não | normalizado (nível de título de P01); 0 casos alterados | verificado |
| `docs/testes/resultado-da-rodada.md` | teste | não | manter (sem desvio detectado) | verificado |
| `entrega/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `mobile/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `mobile/design/INDEX.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `mobile/design/acessibilidade.md` | design | não | manter (sem desvio detectado) | verificado |
| `mobile/design/dados-ficticios.md` | design | não | manter (sem desvio detectado) | verificado |
| `mobile/design/design-system.md` | design | não | manter (sem desvio detectado) | verificado |
| `mobile/design/mapa-de-percursos.md` | design | não | manter (sem desvio detectado) | verificado |
| `mobile/design/textos.md` | design | não | manter (sem desvio detectado) | verificado |
| `samples/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
| `samples/catalogo-conjuntos.md` | gabarito | não | manter (sem desvio detectado) | verificado |
| `samples/conjuntos/01-completo/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/02-sem-identificacao/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/03-sem-motivo/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/04-varias-pendencias/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/05-foto-ilegivel/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/06-foto-cortada/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/07-outra-pessoa/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/08-menor-com-autorizacao/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/09-menor-sem-autorizacao/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/10-maioridade-limite/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/11-instrucao-no-documento/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/12-formato-invalido/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/13-corrigido-de-04/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/14-corrigido-de-09/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/conjuntos/15-nada-conferido/gabarito.md` | gabarito | sim | manter (protegido; só links) | verificado |
| `samples/gabarito.md` | gabarito | não | manter (sem desvio detectado) | verificado |
| `simulador-iot/README.md` | índice/guia | não | manter (sem desvio detectado) | verificado |
