# Research: Proteger entradas e processamento (RC-06)

Decisões do plano. Formato: decisão, motivo, alternativas.

## D1 — Tipo real por assinatura (bytes iniciais), extensão como segunda trava
- **Decisão**: ler os primeiros bytes e aceitar apenas `%PDF-` (PDF), `FF D8 FF` (JPEG) e `89 50 4E 47 0D 0A 1A 0A` (PNG). A extensão (`pdf`, `jpg`/`jpeg`, `png`, sem diferenciar caixa) precisa **corresponder** ao tipo real; o `Content-Type` declarado pelo cliente é ignorado.
- **Motivo**: nome e `Content-Type` são controlados por quem envia. Assinatura barata e sem biblioteca; a validação estrutural fica a cargo do PDFBox (PDF) e do `ImageIO` (imagens).
- **Alternativas**: `Files.probeContentType` (depende do SO e da extensão, rejeitada); Apache Tika (dependência grande, desnecessária para 3 tipos).

## D2 — PDFBox 3.0.x para páginas e proteção
- **Decisão**: `Loader.loadPDF(RandomAccessReadBufferedFile)` sobre o arquivo em quarentena; `getNumberOfPages()`; `InvalidPasswordException` (ou `isEncrypted()` sem senha) → `PDF_PROTEGIDO`; qualquer outra `IOException` → `ARQUIVO_ILEGIVEL`. Versão estável fixa no `pom.xml`, confirmada no Maven Central; nenhum recurso de renderização, texto ou execução é usado (FR-007). Documentação consultada no Context7 (PDFBox 3.0: `Loader`, `RandomAccessReadBufferedFile`, cache de fluxo).
- **Motivo**: contagem de páginas e detecção de criptografia exigem analisar a árvore de objetos; contar `/Type /Page` no texto é falsificável.
- **Alternativas**: parser próprio (alto risco, fora de escopo); iText (licença AGPL); sem validação de páginas (descumpre FR-005).

## D3 — Limite rígido em fluxo, arquivo em quarentena
- **Decisão**: copiar o fluxo para um arquivo temporário com buffer de 8 KB, contando bytes; ao passar de 5 MB (5.242.880) a cópia **para** e o resultado é `TAMANHO_EXCEDIDO`, sem confiar em `Content-Length`. A quarentena fica em `<raiz>/.quarentena/` quando `DOCUMENT_STORAGE_PATH` está definida e, **sem a variável**, em um diretório temporário privado criado pela própria conferência (assim a conferência funciona mesmo sem armazenamento configurado). Só `guardar` exige a raiz: ele copia o arquivo aceito para um temporário **dentro da raiz** e então faz `ATOMIC_MOVE` para o nome interno (rename entre volumes diferentes não é atômico, por isso não se move direto da quarentena do sistema). Recusa ou falha remove o temporário (`finally`). Arquivo vazio → `ARQUIVO_ILEGIVEL`.
- **Motivo**: evita memória proporcional ao envio, deixa o PDFBox ler de disco e garante "nada gravado" nas recusas (FR-006, FR-014).
- **Alternativas**: carregar em `byte[]` (até 5 MB por arquivo × concorrência; rejeitada por margem); conferir antes de qualquer cópia (impossível para PDF sem arquivo).

## D4 — Imagens: cabeçalho via ImageIO, dimensões limitadas
- **Decisão**: `ImageIO.getImageReaders(stream)` + `reader.getWidth/Height(0)` **sem decodificar** os pixels; recusar se o formato do leitor não bate com o tipo pela assinatura, ou se largura × altura > 25 milhões de pixels (limite demonstrativo, constante central). Falha ao ler cabeçalho → `ARQUIVO_ILEGIVEL`.
- **Motivo**: bomba de descompressão em imagens (poucos KB que expandem para GB) é o risco clássico; ler só o cabeçalho evita o custo.
- **Alternativas**: decodificar a imagem inteira (risco de memória); sem limite (descumpre o caso de borda da spec).

## D5 — Identificador interno e travessia de diretório por construção
- **Decisão**: `IdInterno` = 16 bytes de `SecureRandom` em 32 caracteres hexadecimais minúsculos; construtor valida `[0-9a-f]{32}`. O caminho é sempre `raiz.resolve(id.valor())`, conferido por `toRealPath()`/`startsWith(raizReal)` e recusando link simbólico (`NOFOLLOW_LINKS`). O nome enviado **nunca** participa do caminho (FR-009/FR-010).
- **Motivo**: se o único texto que chega ao sistema de arquivos tem formato fixo, `../`, `\`, `:`, nomes reservados do Windows e Unicode estranho não têm como entrar.
- **Alternativas**: sanear o nome enviado e usá-lo no caminho (superfície grande, rejeitada); UUID com hífens (equivalente; hex puro simplifica o regex).

## D6 — Nome de exibição saneado
- **Decisão**: `NomeExibicao.de(String)`: pegar só o último segmento (após `/` ou `\`), normalizar (NFC), remover caracteres de controle e de formatação bidirecional, trocar `< > : " | ? * ` por `_`, cortar em 100 caracteres preservando a extensão; vazio ou só pontos → `documento`. Usado apenas como texto (resposta JSON, `Content-Disposition` com `filename*` codificado, sem CR/LF).
- **Motivo**: o `Content-Disposition` e a interface exibem esse nome; injeção de cabeçalho (CR/LF) e truque bidirecional (`RLO`) são riscos reais.

## D7 — Download seguro e autorização pela RC-05
- **Decisão**: `DownloadSeguro.enviar(resposta, ArquivoGuardado, Path)` define `Content-Type` a partir do `TipoArquivo` armazenado (nunca do nome), `Content-Disposition: attachment; filename*=UTF-8''...`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`, `Content-Length`. Copia em buffer de 8 KB. Arquivo ausente → `ExcecaoNegocio` 500 padrão `ERRO_INTERNO` com log técnico (sem caminho na resposta; FR-015).
- A rota de download de negócio (RC futura) é declarada via `RotasPrivadas.registrar(caminho, servlet, PACIENTE)`; a busca filtrada pela dona + `Propriedade.exigir` já dão 404 idêntico; administrador e simulador recebem 403 pelo filtro de perfil. Nesta história a prova usa uma rota de exemplo em `src/test`.
- **Alternativas**: servir a pasta como recurso estático (proibido: arquivo público); redirecionar para URL assinada (fora de escopo).

## D8 — Texto puro em JSON e cabeçalhos
- **Decisão**: (a) `Json` passa a escapar `<`, `>`, `&` e `'` como `<`, `>`, `&`, `'` (`CharacterEscapes` do Jackson); o cliente JSON decodifica normalmente, mas a resposta nunca contém marcação literal; (b) `CabecalhosSegurosFiltro` em `/*` adiciona `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: no-referrer`; é filtro de **cabeçalho apenas**, não altera status nem 404 (diferente do guarda global rejeitado na RC-05, D3). `Strict-Transport-Security` só quando `urlPublica` é https.
- **Motivo**: o consumidor é o app e o contrato é JSON; escapar na origem e impedir o sniffing cobre o critério "sem HTML executável" sem inventar um sanitizador de HTML (que seria superfície de bug).
- **Alternativas**: biblioteca de sanitização HTML (desnecessária: não devolvemos HTML); escapar só no app (o servidor não pode depender do cliente).
- **Campo declarativo**: DTOs de texto simulado carregam `formato: "texto_puro"` e `simulado: true` (CLAUDE.md §11).

## D9 — Conteúdo de documento como dado: tipo, delimitação e contrato de saída
- **Decisão**:
  - `TextoNaoConfiavel` (record) guarda o texto; `toString()` devolve só `[texto nao confiavel, N caracteres]`; `delimitar(idOrigem)` produz `<<DADO_NAO_CONFIAVEL origem=... >> ... <<FIM_DADO_NAO_CONFIAVEL>>` removendo/escapando qualquer ocorrência dos próprios delimitadores no texto. Esse é o único caminho para incluir texto de documento em mensagem a outro componente.
  - `ValidadorDeSaidaEstruturada` valida a saída de extração/resumo: JSON com **conjunto fechado de campos**, `codigoRegra` ∈ lista de regras do protocolo, `estado` ∈ enum, tamanhos máximos de texto, e nenhum campo de controle (perfil, estado da solicitação, resultado geral) vindo do conteúdo. Qualquer violação → rejeita (`SaidaInvalida`), nunca "corrige".
  - Como o motor de regras, o resumo e a extração reais ainda não existem, a prova é feita com um **pipeline de exemplo só de teste** (adaptador mockado que “obedece” ao texto malicioso) mostrando que critérios, perfil e estado não mudam e que a saída hostil é rejeitada. Limite registrado: a garantia no motor real é verificada pelas histórias que o criarem, repetindo `InjecaoEmDocumentoTest`.
- **Alternativas**: filtro de palavras (“ignore”, “aprove”…): frágil e fácil de contornar, rejeitado; confiança no prompt da IA: viola a constituição V.

## D10 — Segredos: varredura repetível e redator de log
- **Decisão**:
  - `VarreduraDeSegredos` (classe de teste, usada por `VarreduraDeSegredosTest`) percorre a raiz do repositório e `mobile/` (ignorando `.git`, `target`, `.env`, binários) procurando: `DB_PASSWORD`/`IOT_API_KEY`/`password`/`senha` com valor literal não vazio fora de exemplos aceitos; cabeçalho de chave privada PEM; prefixos de token (`ghp_`, `github_pat_`, `AKIA…`, `sk-…`); URL JDBC com `usuario/senha@`; token `Bearer` com valor longo. Lista de exceções **explícita e documentada** (hashes BCrypt da carga sintética, senha fictícia documentada como “não é senha de ninguém”, chave de teste fictícia de `AmbienteDeAutorizacao`).
  - **Valores reais**: se `DB_USERNAME`, `DB_PASSWORD`, `IOT_API_KEY` ou o token do GitHub estiverem no ambiente do processo (ou no `.env` local), o teste confere que o **valor literal** não aparece em nenhum arquivo versionável; nunca imprime o valor, só o arquivo e a linha.
  - O teste planta um segredo falso num diretório temporário e exige que seja detectado (SC-004, evita “varredura que nunca acha nada”).
  - `RedatorDeLog.redigir(String)` mascara padrões (senha em `chave=valor`, `Bearer`, `user/senha@` em URL JDBC, valores de `IOT_API_KEY`) e é aplicado à mensagem de exceção em `RespostaErro.tratar` e nos pontos que registram falhas do banco. A pilha continua no log (sem a mensagem sensível); a resposta nunca traz nenhuma das duas.
- **Alternativas**: ferramenta externa (gitleaks): excelente, mas instalação extra e fora do `mvnw`; fica como recomendação em `docs/`, a varredura própria cobre o critério reproduzível sem instalar nada.

## D11 — HTTPS por configuração e recusa de configuração insegura
- **Decisão**: variável opcional `API_PUBLIC_URL` (endereço pelo qual o app alcança a API na demonstração remota). `ConfiguracaoDeCanal` recusa iniciar se `API_PUBLIC_URL` não for `https` **e** o host não for loopback (`localhost`, `127.0.0.1`, `::1`) nem o alias do emulador (`10.0.2.2`). O servidor Jetty continua em HTTP atrás de proxy/túnel TLS (provido pela equipe); a documentação diz como e o app Android deve declarar tráfego em texto claro **apenas** na variante de depuração. Sem a variável, nada muda.
- **Limite honesto**: esta história **não** emite certificado, não prova HTTPS ponta a ponta e não existe app para conferir; apenas impede a configuração remota insegura e documenta. Aparece em "não verificado".
- **Alternativas**: TLS embutido no Jetty (exige keystore e senha: mais um segredo); filtro que olha `X-Forwarded-Proto` (confiar em cabeçalho forjável sem proxy confiável, rejeitado).

## D12 — SQL parametrizado: conferência de código-fonte + teste opt-in
- **Decisão**: `SqlParametrizadoTest` varre `src/main/java/**` e falha se encontrar `createStatement(`, `Statement`, `executeQuery(`/`executeUpdate(`/`execute(` com argumento `String`, ou `prepareStatement(` cujo argumento não seja uma constante `static final String` literal ou literal. Isso torna a regra verificável a cada build. Teste opt-in (somente leitura) contra o Oracle: buscar usuário por e-mail `' OR 1=1 --` retorna vazio; roda só com `DB_*` no ambiente, como os demais testes Oracle.
- **Motivo**: “consultas parametrizadas” é um critério de aceite; a varredura impede regressão silenciosa nas próximas histórias.
- **Alternativas**: apenas revisão manual (não repetível).

## Resolvido sem pergunta ao usuário
- Limites de dimensão de imagem (25 MP) e tamanho do nome de exibição (100 caracteres): constantes centrais, revisáveis (D4, D6).
- Nenhuma tabela nova: os metadados já existem em `RC_DOCUMENTO` (RC-03).
