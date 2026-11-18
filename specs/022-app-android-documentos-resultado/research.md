# Research — RC-19

Decisões que a #40/#42 e a [research da RC-18](../017-app-android-login-pedidos/research.md) (D1–D10) não fixam. As já fixadas (versões, cliente sem retentativa, conexão nova por chamada, 401 central, `ErroApi`, navegação própria) continuam valendo e não são repetidas.

## D1 — Envio multipart em streaming
- **Decision**: o `ApiClient` aceita um corpo multipart pronto; o arquivo entra como `RequestBody` que abre o `InputStream` a cada tentativa (`FonteArquivo.abrir()`), com o tamanho quando conhecido. Falha ao abrir o arquivo (inclusive `SecurityException`) vira `ArquivoInacessivelException` (uma `IOException`) e o erro `ErroApi.ArquivoInacessivel`.
- **Rationale**: não carrega 5 MB (ou um arquivo maior, que a API recusa com 413) na memória; reabrir a cada tentativa permite "Tentar de novo" com o mesmo arquivo; uma `RuntimeException` dentro do OkHttp derrubaria o processo.
- **Alternatives**: ler o arquivo para `ByteArray` (simples, mas memória e sem limite); recusar no app acima de 5 MB (a #42 pede a mensagem da API).

## D2 — Chave de envio
- **Decision**: `X-Chave-Envio` = UUID aleatório sem hífens (32 caracteres hexadecimais), gerado quando a paciente escolhe o arquivo e guardado no estado do envio em curso; toda tentativa daquele arquivo reutiliza a chave. Nova escolha gera nova chave.
- **Rationale**: atende ao padrão `^[A-Za-z0-9_-]{8,64}$`; a repetição devolve 200 com o documento existente (sem duplicar).

## D3 — Seletor do sistema e metadados
- **Decision**: `ActivityResultContracts.OpenDocument` com `application/pdf`, `image/jpeg`, `image/png`; nome e tamanho por `OpenableColumns`, tipo por `ContentResolver.getType`. Sem permissão de armazenamento e sem `takePersistableUriPermission`.
- **Rationale**: o seletor concede leitura temporária ao app; se ela se perder (processo encerrado), a próxima abertura falha e o app pede nova escolha (#42). O formato real é decidido pela API pelo conteúdo.
- **Alternatives**: `GetContent` (aceita só um tipo MIME base); "Fotografar" (fora: depende da E4.1/sensores).

## D4 — Telas e navegação
- **Decision**: `Tela` ganha `NovoPedido`, `Documentos(id)`, `Leitura(id)`, `Resultado(id, analiseId?)` e `Pendencia(pedidoId, detalhe)`. `Navegador.substituir` troca o topo (novo pedido → documentos; leitura → resultado) para o "voltar" não reabrir uma operação concluída. Pedido e Documentos reconsultam a API ao voltar a aparecer, como "Meus pedidos".
- **Rationale**: sem biblioteca de navegação (#40). O detalhe da pendência recebe os dados da mesma análise já carregada (nunca mistura análises; sem nova chamada).

## D5 — Ler e analisar
- **Decision**: a tela de leitura faz `POST …/extracoes` ao abrir (a paciente acabou de tocar "Ler documentos"; repetir sobre a mesma versão devolve 200), mostra a situação de cada documento e só então oferece "Verificar critérios" com o diálogo E3.8. Ambas as chamadas usam `leituraLonga` (70 s).
- **Rationale**: a conferência (E3.2) está fora do épico; o diálogo deixa claro que, depois de verificar, não é possível anexar novos documentos.

## D6 — Tempo esgotado ou queda durante a análise
- **Decision**: função pura `decidirAposFalhaDaAnalise(historico)`: `vigenteId` presente → abrir o resultado; `execucao.situacao = EM_ANDAMENTO` → "ainda não terminou" + "Consultar resultado"; senão → "Tentar de novo" (novo `POST`). Se o próprio `GET` falhar, a tela oferece "Consultar de novo" (repete só o `GET`), nunca o `POST`.
- **Rationale**: exigência da #40/#42; o `POST` não é repetido sem saber se a análise anterior foi gravada.

## D7 — Criação de rascunho sem deduplicação
- **Decision**: em tempo esgotado no `POST /solicitacoes`, o app mostra o texto E4.3 ("Não foi possível confirmar a operação…") e orienta conferir "Meus pedidos" antes de repetir; "Tentar de novo" continua disponível.
- **Rationale**: a API cria um pedido por chamada (sem chave de idempotência) e o backend não pode ser alterado nesta história; o rascunho duplicado é inofensivo, mas a paciente precisa saber.

## D8 — Resumo simulado
- **Decision**: após `POST …/analises` com sucesso e sem `resumo`, o app faz **um** `POST …/resumo` (primeira tentativa) antes de abrir o resultado; falha é ignorada nesse momento. Na tela de resultado, resumo ausente ou `PADRAO` oferece "Tentar de novo"; 409 reconsulta `GET …/analises`.
- **Rationale**: sem isso o resumo nunca apareceria no percurso normal; a regra "sem retentativa automática" continua valendo (só a paciente repete).

## D9 — Seleção e apresentação do resultado
- **Decision**: função pura `apresentarHistorico(historico, analiseId?)` devolve um estado fechado: `EmAndamento`, `SemAnalise`, `Falha` (pedido em `FALHA_ANALISE` sem vigente), `SemVigente(anterior?)` ou `Analise(analise, vigente)`. Só a análise com `id == vigenteId && vigente` é atual; outra só como "Resultado anterior".
- **Rationale**: contrato da spec 019; testável sem interface.

## D10 — Textos novos e textos do desenho que pressupõem rotas fora da #42
- **Decision**: textos novos ficam em `ui/Textos.kt` marcados "RC-19" e são registrados em `mobile/design/textos.md`. Textos do desenho que oferecem ações ausentes nesta versão (substituir, remover, conferir, "Depois você confere…") são ajustados sem prometer a ação; os botões correspondentes não aparecem.
- **Rationale**: CLAUDE.md proíbe simular funcionalidade e a spec 019 proíbe botão inoperante.

## D11 — Selo SIMULADO e cabeçalho
- **Decision**: `SeloSimulado` com os tokens `rc-simulated-fg/-bg` (`#8D3C0C`/`#FFF1E6`, 6,82:1) e o estilo `Selo` (Nunito 800, 12, caixa-alta); `CabecalhoVoltar` aceita o título "Pedido {n}" e o selo à direita, como nos quadros E3.x.

## D12 — Rótulo do resultado sem o campo da RC-15
- **Decision**: `rotuloResultado` é opcional no modelo; ausente, o app usa o rótulo local equivalente (`rotuloResultadoGeral`, RC-18). `orientacao` ausente é omitida.
- **Rationale**: o backend em execução pode ser anterior ao commit `65cbadf`; a tela não deve falhar por isso.

## D13 — Integração com a `main` depois da RC-16 e da RC-17 (17/11/2026)
- **Decision**: a feature foi renumerada para **022** (a `main` passou a ter `020-reenviar-versao-corrigida` e `021-submeter-simulacao`) e a branch integrou a `main`. Como a RC-16 passou a aceitar documento depois da análise (`POST …/documentos` só recusa em `SUBMETIDA_SIMULACAO`), os textos que diziam ser impossível anexar depois da verificação foram trocados por "ainda não disponível neste app"; o app continua sem reenvio, comparação (`comparacao`) e conclusão da simulação (fora da #42).
- **Rationale**: nenhum texto pode afirmar uma limitação que a API não tem; implementar o reenvio no app é escopo novo.
