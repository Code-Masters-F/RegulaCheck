# Research: Executar as regras do protocolo (RC-12)

Decisões tomadas no plano, com base no código existente (RC-03 a RC-11) e na spec. Nenhum item ficou como NEEDS CLARIFICATION.

## D1. Onde ficam as regras e quem decide como avaliá-las

- **Decision**: os **metadados** de cada regra (código, descrição, evidência exigida, natureza, condição, mensagem, responsável, ordem) são lidos de `RC_REGRA` pela versão do protocolo do pedido (`RC_SOLICITACAO.PROTOCOLO_ID`), **sem** filtrar por `ATIVO` (um pedido continua usando a versão a que está ligado). A **lógica** de cada regra é uma implementação Java (Strategy) indexada pelo código (`RC-R01` a `RC-R06`). O motor só aceita a versão se toda regra cadastrada tiver implementação com a **mesma natureza**; caso contrário, falha por inteiro (`ProtocoloNaoSuportadoException`).
- **Rationale**: o banco já guarda as regras versionadas desde a RC-03 (com `CHECK`s de natureza e responsável); texto e responsável mudam por versão sem recompilar. A lógica de avaliação não cabe em SQL nem em texto livre de "condição" sem um interpretador (que seria complexidade sem necessidade). Falhar por inteiro evita "sem pendências" por regra ignorada (FR-006).
- **Alternatives considered**: (a) regras inteiramente no código — rejeitada: perde o versionamento já modelado e duplica os textos; (b) linguagem de condição interpretada a partir de `CONDICAO_APLICACAO` — rejeitada: o campo é texto para pessoas e só existe uma regra condicional; (c) pular regra desconhecida — rejeitada: produziria resultado incompleto apresentado como completo.

## D2. O que conta como evidência (leitura + conferência)

- **Decision**: cada campo de cada documento da leitura atual vira uma **evidência de campo** com uma situação:

| Leitura (RC-10) | Decisão vigente aplicável (RC-11) | Situação da evidência | Valor usado |
|---|---|---|---|
| documento `PROTEGIDO` ou `LEITURA_VAZIA` | — | `DOCUMENTO_NAO_LIDO` (para os 6 campos) | não |
| `ILEGIVEL` | — (RC-11 recusa decisão) | `ILEGIVEL` | não |
| `NAO_LOCALIZADO` | nenhuma | `NAO_LOCALIZADO` | não |
| `NAO_LOCALIZADO` | `DIVERGENTE` / `CAMPO_EXISTE_E_NAO_FOI_LIDO` | `DIVERGENCIA_APONTADA` | não |
| `IDENTIFICADO` | nenhuma | `NAO_CONFERIDO` | não |
| `IDENTIFICADO` | `CONFIRMADA` | `CONFIRMADO` | **sim** |
| `IDENTIFICADO` | `DIVERGENTE` / `VALOR_DIFERENTE_DO_DOCUMENTO` | `DIVERGENCIA_APONTADA` | não |

  "Aplicável" usa `SituacaoConferencia.aplicavel(...)` da RC-11 (decisão incoerente com a situação do campo é ignorada, como lá). A origem (página, trecho) acompanha a evidência quando a leitura a tiver.
- **Rationale**: a RC-11 existe para que nenhuma regra use leitura não conferida (spec RC-11: "antes de qualquer regra olhar para esses dados, a paciente precisa poder conferir"); a tabela de estados prevê `REQUER_CONFERENCIA` → nova análise "quando a conferência é confirmada". Não localizado não exige decisão (como na situação da RC-11, em que eles "não pesam").
- **Alternatives considered**: usar valor identificado mesmo sem confirmação — rejeitada: permitiria cobrar/liberar com base em erro de transcrição; exigir a situação `CONFERIDA` para montar a avaliação — rejeitada: o motor já devolve `REQUER_CONFERENCIA` com o motivo por regra, o que é mais informativo e mantém "todas as regras avaliadas".

## D3. Tipo do documento

- **Decision**: o tipo do documento é o valor **confirmado** de `TIPO_DOCUMENTO`, interpretado pelos nomes de `TipoDocumental` (`ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL`, `OUTRO`). `NAO_LOCALIZADO` (documento lido sem título de tipo) conta como tipo conhecido "sem tipo exigido" (`OUTRO`). Qualquer outra situação, ou valor fora da lista, deixa o tipo **desconhecido** (não verificável). O `tipoDocumental` escolhido pela paciente no envio **não** é usado.
- **Rationale**: FR-010; a RC-10 registrou que o tipo informado "não é reconciliado com o campo lido" e deixou a decisão para a RC-12. A escolha da paciente não é prova.

## D4. Interpretação das regras v0.1

Implementa FR-012 a FR-017. Notação: *confirmado* = evidência `CONFIRMADO` com valor interpretável; *não verificável* = `ILEGIVEL`, `NAO_CONFERIDO`, `DIVERGENCIA_APONTADA`, `DOCUMENTO_NAO_LIDO` ou confirmado com valor não interpretável. Em toda regra: **divergência comprovada ou ausência em documento legível (PENDENTE) prevalece** sobre não verificável quando não depende do dado não verificável.

| Regra | ATENDE | PENDENTE | NAO_VERIFICAVEL (motivo) | NAO_APLICAVEL |
|---|---|---|---|---|
| R01 | algum documento de tipo `ENCAMINHAMENTO` | todos os documentos com tipo conhecido e nenhum encaminhamento (inclui pedido sem documentos) | algum documento com tipo desconhecido (motivo do tipo) | — |
| R02 | ≥ 2 documentos com nome **e** data de nascimento confirmados, todos iguais, e nenhum nome/data não verificável nem documento não lido | nomes ou datas confirmados diferentes entre documentos | documento não lido ou nome/data não verificável (motivo do campo); < 2 documentos comparáveis (`DOCUMENTOS_INSUFICIENTES`) | — |
| R03 | algum documento `IDENTIFICACAO` com nome e data de nascimento confirmados | nenhum documento de identificação (todos com tipo conhecido) ou identificação sem nome/data (não localizados) | documento de tipo desconhecido, ou identificação com nome/data não verificável | — |
| R04 | todos os encaminhamentos com motivo confirmado (conteúdo não avaliado) | algum encaminhamento com motivo `NAO_LOCALIZADO` | motivo não verificável; nenhum encaminhamento confirmado (motivo do documento de tipo não verificável, se houver; senão `DEPENDE_DE_DOCUMENTO_AUSENTE`) | — |
| R05 | todos os encaminhamentos com emissão confirmada entre 0 e 180 dias antes da data de referência | emissão `NAO_LOCALIZADO`, a mais de 180 dias, ou posterior à data de referência | emissão não verificável / não interpretável; nenhum encaminhamento confirmado (como R04) | — |
| R06 | menor de 18 e algum documento com `AUTORIZACAO_ASSINADA` confirmada `SIM` | menor de 18 e todos os documentos lidos sem autorização (`NAO_LOCALIZADO` ou `NAO`) | condição indeterminada (`CONDICAO_INDETERMINADA`: sem identificação com data confirmada, data não verificável ou datas diferentes entre identificações); menor de 18 com documento não lido ou autorização não verificável e nenhum `SIM` | 18 anos ou mais na data de referência |

- **Comparação de nomes (R02)**: normalização NFD sem marcas de acento, minúsculas (`Locale.ROOT`), espaços colapsados e aparados. **Datas**: `LocalDate.parse` estrito (`AAAA-MM-DD`); falha = `VALOR_NAO_INTERPRETAVEL`. **Idade (R06)**: `Period.between(nascimento, referencia).getYears() < 18`. **Validade (R05)**: `ChronoUnit.DAYS.between(emissao, referencia)` em `[0, 180]`.
- **Rationale**: tabela de resultados do protocolo ("não localizada em documento legível ou divergente → PENDENTE; ilegível → NAO_VERIFICAVEL"; "ilegível nunca equivale a ausente"); casos 1 a 5 conferidos à mão contra a tabela (ver `quickstart.md`).
- **Alternatives considered**: R04/R05 `PENDENTE` sem encaminhamento — rejeitada: a mensagem ("peça à unidade emissora que complete o documento") e o responsável (unidade emissora) seriam falsos quando o problema é a falta do encaminhamento, que R01 já cobra da paciente; R06 com data de nascimento de qualquer documento — rejeitada: o protocolo fixa a do documento de identificação.

## D5. Data de referência

- **Decision**: maior `ENVIADO_EM` entre os documentos avaliados, convertido para data em `America/Bahia` (horário de Salvador, sem horário de verão). O motor recebe a data pronta: não lê relógio.
- **Rationale**: o protocolo conta validade e idade "até a data do envio"; usar o envio torna a avaliação reprodutível para a mesma revisão (FR-018, FR-026) e independente de quando a RC-13 rodar. **Alternative**: data da análise (relógio) — fica como alternativa para a RC-13 se o grupo preferir (registrado em Assumptions).

## D6. Montagem e consistência sem bloqueio

- **Decision**: `AvaliacaoService.avaliar(sessao, solicitacaoId)` lê, sem transação longa e sem bloqueio: pedido da paciente (`SolicitacaoRepository.buscarDaPaciente`, 404 idêntico via `Propriedade`), leitura mais recente (`ExtracaoRepository.buscarMaisRecente`), decisões vigentes (`ConferenciaRepository.buscar`), documentos disponíveis (`DocumentoRepository.listarDoPedido`) e regras da versão (`ProtocoloRepository.listarRegras`). Recusa com `409 CONFLITO_ESTADO` se não há leitura ou se ela não é a atual. Como são leituras separadas, confere a coerência do retrato: revisão da leitura = revisão do pedido, leitura ainda atual na consulta da conferência e documentos da leitura = documentos disponíveis; se algo mudou no meio, recusa como desatualizada (nada parcial).
- **Rationale**: esta história não grava nada (FR-025), então não precisa de `FOR UPDATE`; quem publicar resultado (RC-13) faz isso sob bloqueio e confere revisão e `atualizadaEm` (FR-005, princípio VI). Repositórios usados diretamente (com a dona aplicada na própria consulta) evitam montar quatro serviços só para ler.
- **Alternatives considered**: uma consulta SQL nova que traga tudo de uma vez — rejeitada: duplicaria SQL já verificado no Oracle (RC-07 a RC-11); chamar `ExtracaoService.consultar` — rejeitada: ela responde 404 quando nunca houve leitura, o que não permite a recusa específica de FR-023.

## D7. Sem rota e sem ligação na composição

- **Decision**: nenhuma rota, nenhum DTO público, nenhuma mudança em `Composicao`/`Dependencias`. A RC-13 instancia `AvaliacaoService` em `Composicao` junto com `POST/GET /solicitacoes/{id}/analises`. `ProtocoloRepository` ganha `listarRegras` (a interface já está em `Dependencias`).
- **Rationale**: decisão de escopo do responsável (13/11/2026); instanciar um serviço que nenhuma rota usa seria código morto em produção.

## D8. Explicação, motivo e mensagens

- **Decision**: `PENDENTE` → mensagem e responsável da regra na versão (`RC_REGRA.MENSAGEM`/`RESPONSAVEL`). `NAO_VERIFICAVEL` → mensagem padrão do protocolo v0.1 (a mesma constante `c_msg_nv` da carga `02_`) + `MotivoNaoVerificavel` (lista fechada de 8) + responsável `PACIENTE`. Nenhum texto do documento entra em mensagem.
- **Rationale**: FR-021; protocolo, seção "Mensagem padrão para não verificável". O motivo fechado permite à RC-14/RC-15 orientar sem texto livre.

## D9. Logs e dados

- **Decision**: `AvaliacaoService` registra um `INFO` com `solicitacaoId`, `revisao`, `resultadoGeral` e quantidades por resultado. `CampoAvaliado` e `Evidencia` sobrescrevem `toString` sem valor nem trecho (padrão de `CampoLido`). `ProtocoloNaoSuportadoException` traz só motivo e código da regra.
- **Rationale**: FR-028; princípio IV; mesmo padrão das RC-10/RC-11 (testes de higiene de log).
