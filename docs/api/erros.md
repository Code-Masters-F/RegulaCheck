# Contrato de erros

Toda resposta de erro tem o mesmo corpo, com `Content-Type: application/json`:

```json
{
  "codigo": "ARQUIVO_MUITO_GRANDE",
  "mensagem": "O arquivo excede 5 MB.",
  "detalhes": [ { "campo": "arquivo", "mensagem": "Tamanho máximo: 5 MB" } ],
  "idRastreamento": "8f1c2a7e"
}
```
- `codigo` (string, obrigatório): estável, em MAIÚSCULAS com sublinhado.
- `mensagem` (string, obrigatório): compreensível para a pessoa, sem termos técnicos.
- `detalhes` (lista, opcional): erros por campo de entrada.
- `idRastreamento` (string, opcional): identificador para achar o registro técnico no log.

**Nunca** aparecem: stack trace, texto de SQL, caminhos de arquivos, chaves, tokens, corpo de erro de provedor externo, conteúdo de documentos.

## Catálogo

| HTTP | Código | Quando |
|---|---|---|
| 400 | `REQUISICAO_INVALIDA` | corpo ou parâmetro inválido (detalhes por campo). RC-09: `DELETE /solicitacoes/{id}` sem o cabeçalho `X-Confirmar-Exclusao: EXCLUIR` (ausente, em branco ou com outro valor) → detalhe `confirmacao`, e nada é apagado |
| 401 | `CREDENCIAIS_INVALIDAS` | login com senha errada, e-mail inexistente, conta desativada ou senha acima de 72 bytes: sempre a mesma resposta (RC-04) |
| 401 | `NAO_AUTENTICADO` | token ausente, malformado, desconhecido, expirado ou encerrado, ou conta desativada: sempre a mesma resposta (RC-04) |
| 403 | `ACESSO_NEGADO` | identidade válida, mas o perfil não pode usar a rota (ex.: administrador em recurso individual de paciente, paciente em rota administrativa, chave do simulador fora da telemetria, conta na telemetria). Mensagem fixa: "Voce nao tem permissao para esta operacao." (RC-05) |
| 404 | `NAO_ENCONTRADO` | recurso inexistente **ou** de outra paciente, sessão de captura inexistente ou encerrada, identificador ilegível, rota inexistente. Corpo idêntico em todos os casos; mensagem fixa: "Recurso nao encontrado." (RC-05) |
| 405 | `METODO_NAO_PERMITIDO` | método HTTP não suportado pela rota |
| 409 | `CONFLITO_ESTADO` | operação incompatível com o estado ou a revisão atual (RC-07: `PUT /solicitacoes/{id}` em pedido que já saiu de `RASCUNHO`; mensagem "Este pedido não pode mais ser alterado." RC-08/RC-16: `POST …/documentos` em pedido `SUBMETIDA_SIMULACAO` (desde a RC-16 a inclusão vale nos demais estados), mensagem "Este pedido já foi submetido e só pode ser consultado." RC-09: remover, substituir ou excluir pedido em `SUBMETIDA_SIMULACAO`; mensagem "Este pedido já foi submetido e só pode ser consultado."; excluir fora de `RASCUNHO`: "Somente rascunhos podem ser excluídos."; excluir quando algo ainda ligado ao pedido impede: "Este pedido não pode ser excluído.", sem apagar nada. RC-11: `PUT …/conferencia` com a leitura desatualizada, "Os documentos mudaram; leia os documentos de novo.", ou com o pedido submetido, "Este pedido já foi submetido e só pode ser consultado."; nada é gravado. RC-13: `POST …/analises` em pedido submetido; sem leitura da versão atual, "Leia os documentos antes de pedir a análise."; documentos ou conferência mudaram durante a análise, "Os documentos ou a conferência mudaram durante a análise. Confira e peça a análise de novo." (nada gravado, o pedido volta ao estado da última análise concluída ou a `RASCUNHO`); análise que passou do limite e foi assumida por outra, "A análise passou do tempo limite e não foi publicada. Consulte as análises ou peça de novo." RC-21: `POST …/sessoes-captura` em pedido submetido, "Este pedido já foi submetido e só pode ser consultado.", sem criar sessão. RC-16: `POST …/reenvio` em pedido submetido (sem `detalhes`) ou, com `detalhes` de etapa, quando os documentos mudaram durante a leitura (`LEITURA`) ou documentos/conferência mudaram ou o limite passou durante a análise (`ANALISE`). RC-17: `POST …/submissoes-simuladas` em pedido não pronto — análise com pendência ou não verificável, sem análise vigente da versão atual, análise em andamento ou pedido já só para consulta; nada é registrado e as recusas de completude dizem que a restrição é do simulador acadêmico e não impede atendimento) |
| 409 | `ANALISE_EM_ANDAMENTO` | RC-13: `POST …/analises` enquanto outra análise do mesmo pedido está em andamento dentro do limite de 1 minuto; cabeçalho `Retry-After` com os segundos restantes; nada é iniciado. RC-16: também em `POST …/reenvio`, com `detalhes` de etapa `ANALISE` |
| 409 | `CHAVE_ENVIO_REUTILIZADA` | RC-08: o cabeçalho `X-Chave-Envio` já foi usado, no mesmo pedido, com outro arquivo ou outro tipo documental; nada é criado nem alterado. Repetir a **mesma** tentativa não é erro: responde 200 com o documento existente |
| 413 | `ARQUIVO_MUITO_GRANDE` | arquivo acima de 5 MB (a leitura para no limite, sem confiar no tamanho declarado). Corpo JSON grande demais **não** usa 413: segue 400 (RC-04) |
| 415 | `TIPO_NAO_SUPORTADO` | três usos: corpo de rota JSON que não é `application/json` (RC-04), arquivo cujo conteúdo real não é PDF/JPEG/PNG (RC-06) e, no envio de documento, corpo que não é `multipart/form-data` (RC-08); a mensagem diferencia |
| 415 | `EXTENSAO_INCOMPATIVEL` | extensão do arquivo ausente, não permitida ou diferente do conteúdo real (RC-06) |
| 422 | `REGRA_DE_NEGOCIO` | violação de regra de negócio com a entrada já bem formada. Em uso desde a RC-07: serviço (`protocoloId`) inexistente ou inativo em `POST/PUT /solicitacoes`, com `detalhes[].campo = "protocoloId"` e a mesma resposta para os dois casos. RC-08: pedido que já tem 3 documentos disponíveis em `POST …/documentos`, com `detalhes[].campo = "documentos"`. (A previsão antiga de usar 422 para submeter com pendências foi substituída por 409 na RC-17.) |
| 422 | `SEM_DOCUMENTOS` | RC-10: `POST /solicitacoes/{id}/extracoes` em pedido sem documento disponível; nada é criado. RC-16: também em `POST …/reenvio` (etapa `LEITURA`) |
| 503 | `ANALISE_INDISPONIVEL` | RC-13: a avaliação falhou (versão do protocolo não aplicável ou erro inesperado); nada é gravado, o pedido vai a `FALHA_ANALISE` e a paciente pode pedir a análise de novo |
| 503 | `LEITURA_INDISPONIVEL` | RC-10: o leitor (simulado hoje) falhou ou respondeu fora do contrato; sem detalhe técnico; nada é gravado e a paciente pode tentar de novo |
| 422 | `DECISAO_NAO_APLICAVEL` | RC-11: `PUT …/conferencia` com decisão que não cabe à situação do campo (qualquer decisão em campo ilegível; confirmar campo não localizado; motivo errado para a situação; campo inexistente no documento); `detalhes` aponta o campo; **nada** do lote é gravado |
| 500 | `ERRO_INTERNO` | falha inesperada; mensagem genérica, detalhe só no log |
| 502/503 | `DEPENDENCIA_INDISPONIVEL` | banco ou adaptador indisponível |

### Etapa da verificação da versão corrigida (RC-16)
`POST /solicitacoes/{id}/reenvio` repassa a falha da etapa com o **mesmo** `status` e `codigo` (`SEM_DOCUMENTOS`, `LEITURA_INDISPONIVEL`, `CONFLITO_ESTADO`, `ANALISE_EM_ANDAMENTO`, `ANALISE_INDISPONIVEL`, `DEPENDENCIA_INDISPONIVEL`), troca a `mensagem` pelo que foi preservado e pelo que a paciente precisa repetir e identifica a etapa em `detalhes`: `[{ "campo": "etapa", "mensagem": "LEITURA" | "CONFERENCIA" | "ANALISE" }]`. Aqui `detalhes` não aponta campo de entrada, e sim a etapa do processamento. O 404 continua idêntico e sem detalhe. Falha ao gravar o resumo depois da análise **não** é erro: a resposta é 201/200 com `etapa: "RESUMO"`. Contrato: [specs/020-reenviar-versao-corrigida/contracts/api-reenvio.md](../../specs/020-reenviar-versao-corrigida/contracts/api-reenvio.md).

Rotas inexistentes devolvem 404 `NAO_ENCONTRADO` no mesmo formato, inclusive qualquer caminho fora da lista fixa da documentação `/docs/*`, onde métodos diferentes de GET recebem 405 (RC-28). **Nenhuma rota usa 202**: leitura, análise, resumo, reenvio e submissão são síncronos e só respondem depois de gravar. Falha da IA ou da extração simuladas não é erro HTTP: preserva o resultado determinístico e devolve resumo padrão identificado.

## Ordem de verificação (RC-05)

Vale para toda rota privada, sempre nesta ordem:

1. **Autenticação** → sem identidade válida: `401 NAO_AUTENTICADO` (mesma resposta para qualquer motivo).
2. **Perfil permitido na rota** → `403 ACESSO_NEGADO`, antes de qualquer consulta ao recurso (por isso o administrador pedindo um recurso individual recebe 403 também quando o identificador não existe).
3. **Dona do recurso** → `404 NAO_ENCONTRADO` se o recurso não existe ou é de outra paciente; a busca já vem filtrada pela dona.
4. **Validação do pedido** → `400 REQUISICAO_INVALIDA` (e 413/415 quando couber). Fica depois da dona para que um corpo ruim não permita descobrir se um recurso alheio existe.
5. Regra de negócio e demais erros (409, 503…).

Na rota do simulador (`POST /sessoes-captura/{id}/leituras`, RC-21) a ordem é a mesma: 401/403 pela chave `X-Chave-Simulador` → 404 se a sessão de captura não existe ou está encerrada (conferido **antes** de ler o corpo) → 415/400 de formato do corpo → 400 de conteúdo, com `detalhes[].campo` em `sessaoId`, `tipo`, `unidade`, `valor`, `medidaEm` ou `corpo`. Valor fora da faixa demonstrativa **não** é erro: só muda a orientação na consulta.

### Recusas de arquivo (RC-06)
PDF com mais de 5 páginas, PDF protegido por senha, arquivo vazio/truncado/corrompido, imagem grande demais e mais de 3 arquivos respondem `400 REQUISICAO_INVALIDA` com `detalhes` (`arquivo` ou `arquivos`) e uma orientação clara; nada é gravado. A lista completa de motivos e mensagens está em [protecoes.md](protecoes.md). Toda resposta de erro, inclusive a 500, leva os cabeçalhos de segurança (`nosniff`, `no-store`…).

Método HTTP não previsto numa rota privada responde 405 somente depois de 401/403. As mensagens de 403 e 404 não citam dona, perfil exigido, identificador interno, SQL nem nome de arquivo.
