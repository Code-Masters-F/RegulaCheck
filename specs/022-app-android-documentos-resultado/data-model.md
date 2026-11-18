# Data model — RC-19 (app)

O app não tem banco. Modelos de contrato (`@Serializable`, `ignoreUnknownKeys`, `explicitNulls=false`) espelham `docs/api/openapi.yaml` (versão da `main`, com a RC-15). Campos exibidos são obrigatórios, salvo os opcionais do contrato; enums ficam como texto e são convertidos em rótulos por `ui/RotulosApi.kt` (valor desconhecido → rótulo neutro). Modelos da RC-18: [data-model 017](../017-app-android-login-pedidos/data-model.md).

## Modelos de contrato

| Modelo | Campos usados | Origem |
|---|---|---|
| `Protocolo` | `protocoloId`, `versao`, `localidade`, `especialidade`, `avisoFicticio` | `GET /protocolos` (`itens[]`) |
| `NovaSolicitacao` | `protocoloId` | corpo de `POST /solicitacoes` (201 → `Solicitacao`) |
| `Documento` | `id`, `nomeArquivo`, `tipoDocumental`, `formato`, `tamanhoBytes`, `paginas`, `estadoEnvio` | `GET …/documentos` (`documentos[]`, `total`, `maximo`) |
| `DocumentoEnviado` | `Documento` + `documentosDisponiveis` | 201/200 de `POST …/documentos` |
| `Extracao` | `situacao`, `simulada`, `aviso`, `documentos[]{documentoId, tipoDocumental, situacao, motivo?, orientacao?, campos[]{situacao}}` | `POST …/extracoes` |
| `HistoricoAnalises` | `estado`, `versaoConjuntoDocumental`, `execucao?{situacao, limiteEm}`, `vigenteId?`, `analises[]` | `GET …/analises` |
| `AnaliseGravada` | `id`, `revisao`, `vigente`, `executadaEm`, `protocolo{versao}`, `resultadoGeral`, `rotuloResultado?`, `aviso`, `documentos[]{documentoId, nomeArquivo}`, `resultados[]{codigoRegra, descricao, resultado}`, `pendencias[]`, `resumo?` | `POST`/`GET …/analises`, `POST …/resumo` |
| `PendenciaGravada` | `codigoRegra`, `descricaoRegra`, `tipo`, `explicacao`, `motivo?`, `responsavel`, `orientacao?`, `evidencias[]{documentoId, campo, situacao, origem?{pagina?, trecho?}}` | `pendencias[]` |
| `ResumoSimulado` | `texto`, `origem` (`ADAPTADOR_MOCKADO`/`PADRAO`) | `resumo` |

`ErroApi` ganha `ArquivoInacessivel` (arquivo escolhido não pôde ser aberto) e `Indisponivel` passa a guardar `codigo` (ex.: `LEITURA_INDISPONIVEL`).

## Estado local das telas

**E2.1 Novo pedido** — `servico: EstadoTela<Protocolo>` (Vazio = sem serviços) · `criacao`: `Ociosa` · `Criando` · `Falhou(erro)`. O serviço continua visível em `Criando`/`Falhou`.

**E2.2 Documentos** — `pedido: EstadoTela<{Solicitacao, documentos}>` · `envio: EnvioEmCurso?`:

| `EnvioEmCurso.situacao` | Quando | Interface |
|---|---|---|
| `ENVIANDO` | após escolher arquivo ou "Tentar de novo" | linha "Enviando…", ações bloqueadas |
| `NAO_ENVIADO(erro)` | rede, tempo, 5xx | linha "Não enviado" + faixa + "Tentar de novo" (mesma fonte, mesma chave) |
| `RECUSADO(mensagem)` | 400/413/415/422 | faixa com a mensagem da API + "Nada foi salvo." + "Escolher outro arquivo"; não listado |
| `ESCOLHER_DE_NOVO` | `ArquivoInacessivel` ou 409 `CHAVE_ENVIO_REUTILIZADA` | faixa "Escolha o arquivo outra vez." + "Escolher outro arquivo" |
| `PEDIDO_FECHADO` | 409 `CONFLITO_ESTADO` | faixa "Este pedido não pode mais receber documentos." |

`EnvioEmCurso = {tipoDocumental, fonte: FonteArquivo, chave}`; confirmado (201/200) → o documento entra na lista e `envio = null`.

**E3.1 Leitura** — `leitura`: `Lendo` · `Lida(extracao)` · `Falhou(erro)`; `analise`: `Ociosa` · `Confirmando` (diálogo) · `Analisando` · `ConsultandoHistorico` · `EmAndamento(retryAfter?)` · `Incerta` (GET falhou) · `Falhou(erro)` · `Concluida` → navega ao resultado.

**E3.3 Resultado** — `EstadoTela<ApresentacaoHistorico>` + `resumo`: `Ocioso` · `Pedindo` · `Falhou`:

| `ApresentacaoHistorico` | Condição | Conteúdo |
|---|---|---|
| `EmAndamento` | `execucao.situacao = EM_ANDAMENTO` | "Em verificação" + "Atualizar" |
| `Falha` | `estado = FALHA_ANALISE` e sem vigente | "A verificação não pôde ser feita." + "Tentar de novo" (leitura) |
| `SemAnalise` | `analises` vazia | texto próprio, sem conclusão |
| `SemVigente(anterior)` | sem `vigenteId` | texto próprio + acesso ao "Resultado anterior" |
| `Analise(a, vigente)` | `vigenteId` (ou `analiseId` pedido) | resultado completo; `vigente=false` → "Resultado anterior" |

**E3.4 Pendência** — recebe `DetalhePendencia{pedidoId, versaoProtocolo, vigente, pendencia, nomesArquivos}` da mesma análise; sem chamada própria.

## Rótulos (de `specs/015-fluxo-telas-app/data-model.md` §2)

Tipo documental, campo, situação do campo, resultado por regra, responsável (`PACIENTE` → Você · `UNIDADE_EMISSORA` → Unidade emissora) e motivo do não verificável usam os rótulos da spec 015. Situação da evidência (sem rótulo na 015, RC-19): `CONFIRMADO` confirmado · `NAO_LOCALIZADO` não localizado na leitura · `ILEGIVEL` ilegível · `NAO_CONFERIDO` leitura não conferida · `DIVERGENCIA_APONTADA` diferença apontada · `DOCUMENTO_NAO_LIDO` documento não lido.
