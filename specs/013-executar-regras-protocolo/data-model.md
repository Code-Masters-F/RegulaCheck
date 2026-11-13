# Data Model: Executar as regras do protocolo (RC-12)

**Sem mudança de esquema.** A história lê tabelas existentes e produz um resultado **em memória** (não gravado; a gravação em `RC_ANALISE`, `RC_RESULTADO_REGRA` e `RC_PENDENCIA` é da RC-13).

## Tabelas lidas

| Tabela | Uso | Origem |
|---|---|---|
| `RC_REGRA` | regras da versão do protocolo do pedido (`WHERE PROTOCOLO_ID = ? ORDER BY ORDEM, CODIGO`) — **nova consulta** | RC-03 (carga v0.1 no `02_`) |
| `RC_SOLICITACAO` + `RC_PROTOCOLO` | pedido da paciente, versão do protocolo, revisão atual | RC-07 (`buscarDaPaciente`) |
| `RC_EXTRACAO*` | leitura mais recente e campos | RC-10 (`buscarMaisRecente`) |
| `RC_CONFERENCIA_CAMPO` | decisões vigentes | RC-11 (`ConferenciaRepository.buscar`) |
| `RC_DOCUMENTO` | documentos disponíveis e `ENVIADO_EM` | RC-08 (`listarDoPedido`) |

## Tipos novos

### `protocolo.entity`

**`RegraDoProtocolo`** (record) — linha de `RC_REGRA`.

| Campo | Tipo | Regra |
|---|---|---|
| `codigo` | String | `RC-R01`…`RC-R06` na v0.1; único na versão |
| `descricao` | String | texto da versão |
| `evidenciaExigida` | String | texto da versão |
| `natureza` | `NaturezaRegra` | `OBRIGATORIA` / `CONDICIONAL` |
| `condicaoAplicacao` | String | `null` se obrigatória (CHECK do banco) |
| `mensagem` | String | mensagem à paciente quando `PENDENTE` |
| `responsavel` | `ResponsavelCorrecao` | `PACIENTE` / `UNIDADE_EMISSORA` |
| `ordem` | int | ordem de avaliação e de apresentação |

**`NaturezaRegra`** (enum): `OBRIGATORIA`, `CONDICIONAL`. **`ResponsavelCorrecao`** (enum): `PACIENTE`, `UNIDADE_EMISSORA`.

### `validacao.model` (puro: sem HTTP, JDBC ou IA)

| Tipo | Conteúdo |
|---|---|
| `Resultado` (enum) | `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL` |
| `ResultadoGeral` (enum) | `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS`; `de(resultados)` aplica a precedência do protocolo |
| `MotivoNaoVerificavel` (enum) | `CAMPO_ILEGIVEL`, `DOCUMENTO_NAO_LIDO`, `TRANSCRICAO_NAO_CONFERIDA`, `DIVERGENCIA_APONTADA`, `VALOR_NAO_INTERPRETAVEL`, `CONDICAO_INDETERMINADA`, `DOCUMENTOS_INSUFICIENTES`, `DEPENDE_DE_DOCUMENTO_AUSENTE` |
| `SituacaoEvidencia` (enum) | `CONFIRMADO`, `NAO_LOCALIZADO`, `ILEGIVEL`, `NAO_CONFERIDO`, `DIVERGENCIA_APONTADA`, `DOCUMENTO_NAO_LIDO` |
| `CampoAvaliado` (record) | `codigo` (`CodigoCampo`), `situacao`, `valor` (só em `CONFIRMADO`), `pagina`, `trecho`; `toString` sem valor/trecho |
| `DocumentoAvaliado` (record) | `documentoId`, `campos` (os 6 códigos, um cada) |
| `EntradaAvaliacao` (record) | `documentos`, `dataReferencia` (`LocalDate`, Salvador) |
| `Evidencia` (record) | `documentoId`, `codigo`, `situacao`, `pagina`, `trecho`; `toString` sem trecho |
| `Veredito` (record) | `resultado`, `motivo` (só em `NAO_VERIFICAVEL`), `evidencias` |
| `ResultadoDeRegra` (record) | `regra` (`RegraDoProtocolo`), `veredito` |
| `Pendencia` (record) | `codigoRegra`, `descricaoRegra`, `tipo` (`PENDENTE`/`NAO_VERIFICAVEL`), `explicacao`, `motivo`, `evidencias`, `responsavel` |
| `AvaliacaoDeRegras` (record) | `resultados` (ordem da versão), `pendencias` (mesma ordem), `resultadoGeral` |

Invariantes: `Veredito` com motivo ⇔ `NAO_VERIFICAVEL`; `CampoAvaliado.valor != null` ⇔ `CONFIRMADO`; listas imutáveis (cópia defensiva); uma pendência por regra `PENDENTE`/`NAO_VERIFICAVEL` e nenhuma para as demais.

### `validacao.regra`

`RegraDocumental` (interface Strategy): `codigo()`, `natureza()`, `Veredito avaliar(EntradaAvaliacao)`. Implementações: `RegraEncaminhamentoPresente` (R01), `RegraIdentificacaoCoerente` (R02), `RegraDocumentoDeIdentificacao` (R03), `RegraMotivoInformado` (R04), `RegraValidadeDoEncaminhamento` (R05), `RegraAutorizacaoDoResponsavel` (R06). Apoio: `LeituraDosCampos` (interpretação de tipo, data, sim/não, nome normalizado; tipo de cada documento).

### `validacao.motor`

`MotorDeRegras`: `avaliar(List<RegraDoProtocolo>, EntradaAvaliacao) → AvaliacaoDeRegras`; `doProtocoloAcademico()` com as 6 implementações; constante `MENSAGEM_NAO_VERIFICAVEL`. `ProtocoloNaoSuportadoException` (motivo: `SEM_REGRAS`, `REGRA_DESCONHECIDA`, `NATUREZA_DIVERGENTE`, `REGRA_REPETIDA`; código da regra).

### `analise.service`

| Tipo | Conteúdo |
|---|---|
| `EvidenciasDaLeitura` | `montar(Extracao, List<DecisaoConferencia>) → List<DocumentoAvaliado>` (tabela D2 do research) |
| `AvaliacaoDoPedido` (record) | `solicitacaoId`, `revisao`, `protocolo` (`Protocolo`), `extracaoId`, `conferenciaAtualizadaEm` (`Instant`), `dataReferencia`, `avaliacao` (`AvaliacaoDeRegras`), `leituraSimulada` (sempre `true`) |
| `AvaliacaoService` | `avaliar(SessaoAutenticada, long) → AvaliacaoDoPedido` |

## Estados

Nenhuma transição. O estado e a revisão do pedido são lidos, nunca escritos. A RC-13 usará `resultadoGeral` para a transição `PROCESSANDO → COM_PENDENCIAS | REQUER_CONFERENCIA | SEM_PENDENCIAS_IDENTIFICADAS` e `revisao` + `conferenciaAtualizadaEm` para saber se o resultado ficou velho.
