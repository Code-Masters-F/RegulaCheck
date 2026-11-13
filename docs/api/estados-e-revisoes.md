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

**Leitura (RC-10):** ler os documentos (`POST /solicitacoes/{id}/extracoes`) **não** muda o estado do pedido, não cria revisão e não inicia análise; os estados que dependem da leitura (`PROCESSANDO`, `REQUER_CONFERENCIA`) pertencem à conferência da transcrição (RC-11) e às regras (RC-12). A leitura é presa à revisão em que foi feita e é `ATUAL` só enquanto `revisao` do pedido for igual a ela; enviar, remover ou substituir documento a torna `DESATUALIZADA`. Pedido em `SUBMETIDA_SIMULACAO` recusa nova leitura (409) e continua podendo ser consultado.

**Conferência (RC-11):** registrar decisão de conferência (`PUT …/extracoes/{extracaoId}/conferencia`) **não** muda o estado do pedido, não cria revisão e não inicia análise. A situação da conferência (`PENDENTE`, `REQUER_CONFERENCIA`, `CONFERIDA`, `DESATUALIZADA`) é calculada a partir da leitura e das decisões e **não é** o estado do pedido; `CONFERIDA` não libera submissão. As transições que dependem dela (`PROCESSANDO` → `REQUER_CONFERENCIA` e `REQUER_CONFERENCIA` → `PROCESSANDO`, tabela acima) continuam sendo da análise (RC-12/RC-13): `POST …/documentos` (RC-08) só aceita `RASCUNHO` e excluir (RC-09) só vale em `RASCUNHO`, então mover o pedido de estado a cada decisão o prenderia fora desses fluxos. Pedido em `SUBMETIDA_SIMULACAO` recusa decisão (409) e continua podendo ser consultado.

**Proibidas:** qualquer estado → `SUBMETIDA_SIMULACAO` exceto a partir de `SEM_PENDENCIAS_IDENTIFICADAS`; `SUBMETIDA_SIMULACAO` → qualquer outro (final); `PROCESSANDO` → `SUBMETIDA_SIMULACAO`; alterações durante `PROCESSANDO` não são rejeitadas: iniciam nova revisão e o resultado da antiga não é publicado (decisão, ver tabela). Não existe volta a `RASCUNHO` depois do primeiro processamento; isso mantém a coerência com `docs/arquitetura.md` ("após correção/reenvio volta a PROCESSANDO").

## Revisão
- Começa em 1 (criação do rascunho, motivo `CRIACAO`); incrementa quando arquivo é enviado, substituído ou removido, quando o serviço do rascunho é trocado (`PROTOCOLO_ALTERADO`, RC-07) ou quando dado relevante/conferência muda. Cada revisão fica em `RC_REVISAO` com número sequencial, sem salto nem repetição. Motivos aceitos pelo banco: `CRIACAO`, `DOCUMENTO_ENVIADO`, `DOCUMENTO_SUBSTITUIDO`, `DOCUMENTO_REMOVIDO`, `CONFERENCIA_ALTERADA`, `PROTOCOLO_ALTERADO` (este depende do script `06_` em bancos criados antes da RC-07).
- Incrementar a revisão invalida o resultado anterior (`vigente=false`) e preserva histórico, protocolo e versão usados.
- **Desvio justificado do princípio VI (RC-11):** registrar uma decisão de conferência **não** cria a revisão `CONFERENCIA_ALTERADA` (o motivo continua previsto no esquema). A decisão é anotação da leitura, não dado do pedido: criar uma revisão a cada decisão tornaria desatualizada a própria leitura conferida (a leitura é atual só se a revisão dela for a do pedido). **Compensação:** a resposta da conferência traz `atualizadaEm`, e a RC-12/RC-13 **devem** capturar a conferência (`atualizadaEm`) junto com a revisão ao publicar uma análise e invalidá-la se `atualizadaEm` mudar. Alternativa rejeitada: criar a revisão e redefinir "atual" da leitura (muda o cálculo da RC-10). Justificativa formal em [Complexity Tracking](../../specs/012-conferir-transcricao/plan.md).
- Um processamento guarda a revisão em que começou; só publica resultado `vigente` se a revisão atual for a mesma. Caso contrário, descarta o resultado como desatualizado.
- Retentativas (upload, análise, submissão) não duplicam efeitos: usar identificadores/constraints; repetir a submissão devolve a existente.
- Uma transição para qualquer estado "de resultado" só ocorre depois de persistir a operação.

## Alinhamento
Nomes iguais aos de `docs/arquitetura.md` e `docs/protocolo-academico.md` (resultado geral). `FALHA_ANALISE` e `RASCUNHO`/`PROCESSANDO`/`SUBMETIDA_SIMULACAO` são estados da solicitação, não resultados do catálogo.
