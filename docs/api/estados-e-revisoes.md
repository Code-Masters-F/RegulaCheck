# Estados, transições e revisões da solicitação

## Estados

| Estado | Significado |
|---|---|
| `RASCUNHO` | pedido criado, documentos ainda sendo enviados; nada processado |
| `PROCESSANDO` | extração/análise em andamento na revisão atual; **não** equivale a concluído |
| `REQUER_CONFERENCIA` | a paciente precisa agir: conferir a transcrição ou reenviar por haver regra `NAO_VERIFICAVEL` |
| `COM_PENDENCIAS` | há regra `PENDENTE`; mostrar o que corrigir |
| `SEM_PENDENCIAS_IDENTIFICADAS` | todas as regras aplicáveis atendidas e nenhuma não verificável; **não** é aprovação do SUS |
| `FALHA_ANALISE` | erro de processamento; permite tentar de novo; não gera resultado de completude |
| `SUBMETIDA_SIMULACAO` | submissão concluída apenas no simulador; estado final |

## Transições permitidas

| De | Evento | Para |
|---|---|---|
| (nenhum) | criar solicitação | `RASCUNHO` |
| `RASCUNHO` | solicitar análise | `PROCESSANDO` |
| `PROCESSANDO` | análise conclui com regra pendente | `COM_PENDENCIAS` |
| `PROCESSANDO` | análise conclui com regra não verificável e sem pendente | `REQUER_CONFERENCIA` |
| `PROCESSANDO` | análise conclui sem pendência | `SEM_PENDENCIAS_IDENTIFICADAS` |
| `PROCESSANDO` | falha de processamento | `FALHA_ANALISE` |
| `RASCUNHO` | enviar, substituir ou remover documento (RC-08, RC-09) | `RASCUNHO` (revisão + 1; motivos `DOCUMENTO_ENVIADO`, `DOCUMENTO_SUBSTITUIDO` e `DOCUMENTO_REMOVIDO`) |
| `RASCUNHO` | excluir o rascunho com confirmação (`DELETE /solicitacoes/{id}`, RC-09) | (pedido deixa de existir; revisões, documentos e arquivos saem junto) |
| `RASCUNHO` | trocar o serviço (protocolo) por outro ativo (`PUT /solicitacoes/{id}`, RC-07) | `RASCUNHO` (revisão + 1, motivo `PROTOCOLO_ALTERADO`); salvar com o mesmo serviço não muda nada |
| `COM_PENDENCIAS`, `REQUER_CONFERENCIA`, `SEM_PENDENCIAS_IDENTIFICADAS` | reenviar documento corrigido ou alterar dado relevante (nova revisão) | `PROCESSANDO` (revisão + 1; resultado anterior deixa de ser vigente) |
| `FALHA_ANALISE` | tentar de novo ou reenviar documento | `PROCESSANDO` |
| `PROCESSANDO` | alterar documento ou dado relevante durante o processamento | `PROCESSANDO` (revisão + 1; o resultado da revisão antiga é descartado e não publicado) |
| `REQUER_CONFERENCIA` | conferência confirmada | `PROCESSANDO` |
| `SEM_PENDENCIAS_IDENTIFICADAS` | submeter em simulação | `SUBMETIDA_SIMULACAO` |

**Decisão da RC-09 (aberta para RC-13 e RC-16):** a linha acima que leva `COM_PENDENCIAS`, `REQUER_CONFERENCIA` e `SEM_PENDENCIAS_IDENTIFICADAS` a `PROCESSANDO` ao reenviar um documento corrigido descreve o fluxo de **análise e reenvio**, que ainda não existe. Remover ou substituir documento (RC-09) em pedido já analisado **aceita a mudança, cria a revisão seguinte, torna a análise vigente não vigente e mantém o estado**: nada é processado, então o pedido não vai a `PROCESSANDO`. Só `SUBMETIDA_SIMULACAO` recusa (409). Quem dispara a nova análise e a transição de estado é a história de análise/reenvio. Excluir só vale em `RASCUNHO`; pedidos já analisados não podem ser excluídos (409), para não perder o histórico de análise.

**Proibidas:** qualquer estado → `SUBMETIDA_SIMULACAO` exceto a partir de `SEM_PENDENCIAS_IDENTIFICADAS`; `SUBMETIDA_SIMULACAO` → qualquer outro (final); `PROCESSANDO` → `SUBMETIDA_SIMULACAO`; alterações durante `PROCESSANDO` não são rejeitadas: iniciam nova revisão e o resultado da antiga não é publicado (decisão, ver tabela). Não existe volta a `RASCUNHO` depois do primeiro processamento; isso mantém a coerência com `docs/arquitetura.md` ("após correção/reenvio volta a PROCESSANDO").

## Revisão
- Começa em 1 (criação do rascunho, motivo `CRIACAO`); incrementa quando arquivo é enviado, substituído ou removido, quando o serviço do rascunho é trocado (`PROTOCOLO_ALTERADO`, RC-07) ou quando dado relevante/conferência muda. Cada revisão fica em `RC_REVISAO` com número sequencial, sem salto nem repetição. Motivos aceitos pelo banco: `CRIACAO`, `DOCUMENTO_ENVIADO`, `DOCUMENTO_SUBSTITUIDO`, `DOCUMENTO_REMOVIDO`, `CONFERENCIA_ALTERADA`, `PROTOCOLO_ALTERADO` (este depende do script `06_` em bancos criados antes da RC-07).
- Incrementar a revisão invalida o resultado anterior (`vigente=false`) e preserva histórico, protocolo e versão usados.
- Um processamento guarda a revisão em que começou; só publica resultado `vigente` se a revisão atual for a mesma. Caso contrário, descarta o resultado como desatualizado.
- Retentativas (upload, análise, submissão) não duplicam efeitos: usar identificadores/constraints; repetir a submissão devolve a existente.
- Uma transição para qualquer estado "de resultado" só ocorre depois de persistir a operação.

## Alinhamento
Nomes iguais aos de `docs/arquitetura.md` e `docs/protocolo-academico.md` (resultado geral). `FALHA_ANALISE` e `RASCUNHO`/`PROCESSANDO`/`SUBMETIDA_SIMULACAO` são estados da solicitação, não resultados do catálogo.
