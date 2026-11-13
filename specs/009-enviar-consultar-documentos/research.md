# Research: Enviar e consultar documentos (RC-08)

Decisões que o plano precisa fechar. Nenhum `NEEDS CLARIFICATION` ficou aberto na spec; aqui estão as escolhas técnicas, o porquê e o que foi descartado. Itens marcados **(confirmar)** dependem de verificação na implementação e viram a primeira tarefa (T-spike).

## D1 — Onde ficam as rotas

- **Decisão**: `POST` e `GET /solicitacoes/{id}/documentos` e `GET /solicitacoes/{id}/documentos/{docId}`, como já publicado em `CONTRATO_API.md`. O `SolicitacaoController` (que detém `/solicitacoes/*`) reconhece o subcaminho `/{id}/documentos[/{docId}]` e **delega** ao `DocumentoController`; qualquer outro subcaminho continua 404 padrão.
- **Por quê**: `RotasPrivadas` recusa dois registros para o mesmo caminho, e o padrão de servlet não aceita curinga no meio (`/solicitacoes/*/documentos`). `docs/api/autorizacao.md` já prevê "estender o `SolicitacaoController` com os subcaminhos".
- **Descartado**: servlet separado em `/documentos/*` (quebra o contrato publicado); herança entre controllers (acopla sem necessidade).

## D2 — Receber o arquivo: multipart pela Servlet API

- **Decisão**: `multipart/form-data` com `request.getPart(...)`, configurado por `MultipartConfigElement` no servlet de `/solicitacoes/*`: `location` = a pasta de quarentena privada (ou temporária privada quando o armazenamento não está configurado), `maxFileSize` = `LimitesArquivo.MAXIMO_BYTES`, `maxRequestSize` = limite + folga de 64 KB para os campos, `fileSizeThreshold` = 0 (nada de arquivo inteiro na memória). `RotasPrivadas` ganha uma sobrecarga `registrar` que aceita essa configuração.
- **Por quê**: o contrato já diz "multipart com um arquivo por requisição", é o que o aplicativo Android envia sem esforço, e o parser da Servlet API já faz a leitura em fluxo com limite. O conteúdo da parte alimenta `ConferenciaDeArquivo.conferir(InputStream, nome)`, que reaplica o limite sem confiar em cabeçalho.
- **Erros**: estouro de `maxFileSize`/`maxRequestSize` vira 413 `ARQUIVO_MUITO_GRANDE`; corpo que não é `multipart/form-data` vira 415 `TIPO_NAO_SUPORTADO` (mesmo código que `CorpoJson` usa para tipo de conteúdo inválido); parte `arquivo` ausente, vazia, repetida ou com mais de um arquivo vira 400 `REQUISICAO_INVALIDA` (detalhe `arquivo`/`arquivos`); multipart malformado vira 400.
- **Confirmado em 13/11/2026** pelo `MultipartJettyTest` (T002, 5 testes verdes em Jetty 12.1.15 EE10): (a) `ServletHolder.getRegistration().setMultipartConfig(...)` vale para requisição roteada por `PathInfo`; (b) **os três erros chegam como `ServletException` com `BadMessageException` 400 "bad multipart"**, sem código próprio de 413; o que distingue é a causa-raiz `IllegalStateException`: `max file size exceeded: N` (arquivo), `max length exceeded: N` (requisição) e `Not multipart Content-Type` (corpo que não é multipart). O controller **não depende só da mensagem**: confere o `Content-Type` antes de `getPart` (415) e classifica o estouro de limite pela causa-raiz `IllegalStateException` com mensagem iniciada em `max ` (413); qualquer outra falha de multipart vira 400. `BadMessageException` está marcado como obsoleto no Jetty 12.1.6, então o código de produção **não** o referencia; (c) as partes em disco são apagadas ao fim da requisição, inclusive após estouro de limite. O teste fica como regressão contra mudança de versão do Jetty.
- ~~(confirmar)~~ plano original da verificação, em Jetty 12.1.15 EE10: (a) a configuração programática por `ServletHolder.getRegistration().setMultipartConfig(...)` vale para requisições roteadas pelo `PathInfo`; (b) o tipo exato de exceção no estouro de limite; (c) as partes em disco são apagadas ao fim da requisição, inclusive em erro. Se (c) falhar, o controller apaga as partes no `finally`.
- **Descartado**: corpo bruto com `Content-Type` do arquivo e metadados em cabeçalhos/consulta (contraria o contrato publicado); biblioteca de upload extra (dependência desnecessária).

## D3 — Entrada validada antes de qualquer gravação

- **Decisão**: campos do multipart: `arquivo` (parte de arquivo) e `tipoDocumental` (parte de texto até 40 caracteres). Cabeçalho opcional `X-Chave-Envio`: 8 a 64 caracteres de `[A-Za-z0-9_-]` (cabe um UUID). Qualquer outro campo é ignorado. Dono, estado, revisão e nome interno **não existem** na entrada.
- **Ordem de verificação** (segue `docs/api/erros.md`): 401 → 403 → 404 (pedido da dona) → 415 (não é multipart) / 413 / 400 (campos e arquivo, incluindo a conferência) → 409 (fora de rascunho; chave reutilizada) → 422 (limite de 3) → 503. A busca da dona vem antes de ler o corpo, para que um envio a pedido alheio responda 404 mesmo com corpo ruim.
- **Por quê**: mesma disciplina da RC-07; o tipo de erro por camada fica previsível e testável.

## D4 — Tipo documental

- **Decisão**: enum fechado `ENCAMINHAMENTO`, `IDENTIFICACAO`, `AUTORIZACAO_RESPONSAVEL`, `OUTRO`, guardado em `RC_DOCUMENTO.TIPO_DOCUMENTAL` com `CHECK`. Valor em branco/ausente/desconhecido: 400 `REQUISICAO_INVALIDA` com detalhe `tipoDocumental` e a lista dos valores aceitos na mensagem.
- **Por quê**: espelha a evidência exigida pelo protocolo acadêmico (RC-R01, R03, R06), sem avaliar nada agora. A obrigatoriedade é problema da análise, não do envio.
- **Descartado**: texto livre (impossibilita regra posterior); lista dependente do protocolo no banco (excesso para um único protocolo demonstrativo).

## D5 — Repetir envio sem duplicar (identificação de tentativa)

- **Decisão**: `X-Chave-Envio` opcional, válida **por pedido** (índice único funcional `(CASE WHEN CHAVE_ENVIO IS NOT NULL THEN SOLICITACAO_ID END, CHAVE_ENVIO)`; uma `UNIQUE (SOLICITACAO_ID, CHAVE_ENVIO)` comum rejeitaria dois envios sem chave, porque o Oracle só ignora chaves compostas totalmente nulas). Guarda-se também o `HASH_CONTEUDO` (SHA-256 em hexadecimal, calculado sobre o arquivo conferido) e o tipo documental. Comportamento:
  - chave nova → cria (201);
  - mesma chave, mesmo hash **e** mesmo tipo documental → devolve o documento existente (200), sem gravar arquivo, documento nem revisão;
  - mesma chave com hash ou tipo diferentes → 409 `CHAVE_ENVIO_REUTILIZADA`, sem efeitos.
- **Dois pontos de checagem**: uma leitura antecipada no Service (depois da conferência e antes de gravar o arquivo) evita I/O inútil na repetição comum; a decisão **final** é a da transação sob bloqueio, e a restrição única é a rede de segurança. Se a transação encontrar a chave já gravada por outro envio simultâneo, aplica a mesma regra (repetido ou conflito) e remove o arquivo recém-guardado.
- **Sem chave**: cada envio é novo (FR-014). O limite de 3 documentos é a única proteção contra duplicata nesse caso; isso é documentado, não escondido.
- **Por quê**: o aplicativo gera um UUID por tentativa e o reutiliza ao repetir; é o padrão de chave de idempotência. O hash impede que uma chave reaproveitada por engano devolva um documento diferente do enviado.
- **Descartado**: deduplicar só por hash (impediria reenviar o mesmo arquivo com outro tipo documental e esconderia duplicatas legítimas); chave obrigatória (quebraria clientes simples e testes manuais).

## D6 — Ordem de gravação e compensação

- **Decisão**: (1) conferir → quarentena; (2) calcular SHA-256 do arquivo em quarentena; (3) leitura antecipada da chave; (4) `ArmazenamentoPrivado.guardar` (atômico, descarta a quarentena); (5) transação do banco; (6) se o resultado não for `REGISTRADO`, ou se houver qualquer exceção, `ArmazenamentoPrivado.remover(id)`. A conexão do pool só é aberta no passo 5. Entre os passos 1 e 4, o estado `RASCUNHO` do pedido (já carregado na conferência da dona) é checado de novo para recusar com 409 antes de gravar o arquivo; a decisão final continua sendo a da transação.
- **Por quê**: não segura conexão nem bloqueio durante I/O de arquivo (pool de 3, banco compartilhado); a compensação já é o contrato do `ArmazenamentoPrivado` (`protecoes.md`, passo 2). Falha ao remover é registrada (sem nome) e deixa, no pior caso, um arquivo órfão ilegível na área privada, sem qualquer exposição.
- **Risco aceito e documentado**: queda do processo entre os passos 4 e 5 deixa um arquivo órfão; não há registro sem arquivo. Limpeza de órfãos fica fora do escopo (retenção não existe no MVP).

## D7 — Limite de 3 documentos sob concorrência

- **Decisão**: dentro da transação, depois do `SELECT … FOR UPDATE` do pedido, `SELECT COUNT(*) FROM RC_DOCUMENTO WHERE SOLICITACAO_ID = ? AND REVISAO_REMOCAO IS NULL`; com 3 ou mais, resultado `LIMITE` (422 `REGRA_DE_NEGOCIO`, detalhe `documentos`). Como todo envio do mesmo pedido passa pelo mesmo bloqueio, a contagem é consistente.
- **Por quê**: um `COUNT` sem bloqueio permitiria quatro documentos sob envios simultâneos (SC-004). O bloqueio da linha do pedido é o mesmo da RC-07, então envio e troca de serviço também se serializam.
- **Descartado**: restrição declarativa de contagem (Oracle não tem); gatilho de banco (fora do padrão do projeto).

## D8 — Revisão e motivo

- **Decisão**: cada envio concluído faz `REVISAO_ATUAL + 1`, `ATUALIZADA_EM = agora`, insere `RC_REVISAO` com motivo `DOCUMENTO_ENVIADO` (já aceito pela restrição `RC_REVISAO_CK_MOTIVO`; o script 06 não é necessário para isto) e grava o documento com `REVISAO_ENVIO` = nova revisão. Tudo na mesma transação. Repetição e recusas não mexem na revisão.
- **Por quê**: regra central do projeto (CLAUDE.md §10): alterar o conjunto documental incrementa a revisão e invalida resultado anterior. A "versão do conjunto documental" do critério de aceite é exatamente a revisão do pedido.

## D9 — Estado do envio e distinção de "análise"

- **Decisão**: o documento expõe `estadoEnvio` como enum fechado (hoje só `ENVIADO`), derivado de o registro existir e estar disponível — não é coluna. O pedido ganha `versaoConjuntoDocumental` (= `revisao`), `documentosDisponiveis` e `analise: { "situacao": "NAO_REALIZADA", "mensagem": "…" }`. Nesta história nada cria análise, então a situação é sempre `NAO_REALIZADA` para pedido em rascunho; a história de análise passa a calculá-la a partir de `RC_ANALISE`.
- **Por quê**: o critério pede distinguir "upload concluído" de "análise concluída" sem inventar processamento. Um enum fechado deixa as histórias seguintes acrescentarem estados sem mudar o significado do atual (FR-019).
- **Cuidado**: nenhuma resposta usa as palavras "conferido", "validado" ou "aprovado" para o documento.

## D10 — Consultar e baixar

- **Decisão**: `DocumentoRepository.buscarDoPedido(docId, solicitacaoId, pacienteId)` com junção até `RC_SOLICITACAO.PACIENTE_ID = ?` na mesma instrução e `AND d.REVISAO_REMOCAO IS NULL`, passado por `Propriedade.exigir`. Documento alheio, inexistente, de outro pedido da mesma paciente ou com id mal formado: o mesmo 404. A lista usa a mesma junção e ordena por `ENVIADO_EM, ID`. O download usa `ArmazenamentoPrivado.abrir` + `DownloadSeguro.enviar` (anexo, `nosniff`, `no-store`, tipo vindo do banco). `CAMINHO_INTERNO` e o hash **nunca** saem do repositório para o DTO.
- **Falhas**: armazenamento não configurado, arquivo ausente na área privada ou erro de leitura → 503 `DEPENDENCIA_INDISPONIVEL` genérico (a lista continua funcionando). Documentos da carga sintética (`02_`) apontam para arquivos que não existem no disco e respondem assim; isso é esperado e vai no quickstart.

## D11 — Resposta do envio

- **Decisão**: 201 com `Location: /solicitacoes/{id}/documentos/{docId}` e o `DocumentoResponse`; 200 sem `Location` novo na repetição. Campos: `id`, `solicitacaoId`, `nomeArquivo`, `tipoDocumental`, `formato`, `tamanhoBytes`, `paginas`, `estadoEnvio`, `revisaoEntrada`, `enviadoEm`, mais `versaoConjuntoDocumental` e `documentosDisponiveis` do pedido no momento. O identificador de documento é o número do banco (como o do pedido na RC-07); o exemplo `doc-0001` do contrato antigo é atualizado.

## D12 — Logs e higiene

- **Decisão**: log só de `solicitacaoId`, `documentoId`, revisão, tipo de arquivo, tamanho e código de resultado. Nunca nome do arquivo, `X-Chave-Envio`, hash, nome interno, caminho ou conteúdo. `HigieneDeLogSolicitacaoTest` serve de modelo para um teste equivalente do documento.

## D13 — Configuração e montagem

- **Decisão**: `Composicao` monta `ArmazenamentoPrivado` e `ConferenciaDeArquivo` a partir de `Configuracao.armazenamentoRaiz()` (o snippet do passo 0 de `protecoes.md`), e o `DocumentoRepository` entra em `Dependencias` (JDBC em produção, memória nos testes). Os testes de endpoint configuram uma raiz temporária. Sem `DOCUMENT_STORAGE_PATH`: a conferência funciona (quarentena temporária), `guardar` e `abrir` respondem 503, e a API sobe normalmente.

## D14 — Esquema do banco (script 07)

- **Decisão**: `ALTER TABLE RC_DOCUMENTO ADD` de `TIPO_DOCUMENTAL VARCHAR2(30 CHAR) DEFAULT 'OUTRO' NOT NULL`, `CHAVE_ENVIO VARCHAR2(64 CHAR)` e `HASH_CONTEUDO VARCHAR2(64 CHAR)`; restrições `RC_DOCUMENTO_CK_TIPO_DOC` (quatro valores), `RC_DOCUMENTO_CK_HASH` (nulo ou 64 caracteres hexadecimais) e `RC_DOCUMENTO_UK_CHAVE` (índice único funcional por pedido, ver data-model.md). O `DEFAULT 'OUTRO'` só existe para as linhas da carga sintética já gravadas; a aplicação sempre informa o valor. O `01_` recebe as mesmas colunas para instalação nova; o `04_` ganha casos de tipo inválido e de chave duplicada.
- **Por quê**: sem estas colunas não há onde guardar o tipo documental nem como distinguir repetição de reutilização. Aditivo: não apaga nem reescreve linha; o grupo executa na conta compartilhada (conforme combinado, esta sessão só aplica script com autorização específica).
- **Descartado**: reutilizar `NOME_ORIGINAL`/`CAMINHO_INTERNO` para guardar metadado (mistura de responsabilidades e fere a regra de nunca expor o caminho).

## D15 — Impacto sobre a RC-07

- **Decisão**: `Solicitacao` ganha `documentosDisponiveis`; `SQL_BUSCAR`/`SQL_LISTAR` ganham uma subconsulta de contagem; `SolicitacaoResponse` ganha `versaoConjuntoDocumental`, `documentosDisponiveis` e `analise`. Mudança **aditiva** nos contratos da RC-07 (nenhum campo some ou muda de significado); os testes da RC-07 que constroem `Solicitacao` são ajustados. Registrado como alteração de outra história no relatório final.
