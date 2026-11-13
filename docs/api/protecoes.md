# Proteções de entrada, arquivos e texto (RC-06)

Guia para as histórias de negócio (envio de documentos, extração, análise, resumo). Contrato completo: [specs/006-protecao-entradas/contracts/api-protecoes.md](../../specs/006-protecao-entradas/contracts/api-protecoes.md). Dados exclusivamente sintéticos; o sistema não diagnostica, não define urgência nem autoriza atendimento.

> **Situação:** as proteções existem como **mecanismos reutilizáveis** e foram provadas com arquivos e textos sintéticos de teste. **A RC-08 é o primeiro uso real:** `POST/GET /solicitacoes/{id}/documentos` e `GET …/documentos/{docId}` (ver [contratos-dados.md](contratos-dados.md)) usam a conferência, o armazenamento privado e o download seguro descritos abaixo, e repetem a matriz de testes de `DownloadAutenticadoTest` para o seu recurso (`MatrizPerfisDocumentoTest`, `ListarAbrirDocumentoEndpointTest`, `RecusaDeArquivoEndpointTest`).
>
> **Como a RC-08 montou as peças** (`Composicao`): `ConferenciaDeArquivo.para(raiz)` e `ArmazenamentoEmDisco(raiz)`, com a pasta de quarentena também como pasta temporária do `multipart/form-data` do servlet de `/solicitacoes/*` (`RotasPrivadas.registrar` com `MultipartConfigElement`: 5 MB por arquivo, +64 KB de folga no corpo, nada em memória). O limite de tamanho do Jetty responde 413 pelo mesmo erro `ARQUIVO_MUITO_GRANDE`. A ordem do envio é conferir → guardar o arquivo → transação do banco; se a transação falhar ou recusar, o arquivo é removido (item 2 abaixo).

## Limites do MVP
3 arquivos por pedido · 5 MB por arquivo · PDF até 5 páginas · JPEG e PNG · imagem até 25 milhões de pixels · nome de exibição até 100 caracteres. Ficam em `LimitesArquivo`.

## Como proteger uma rota nova de arquivo (≈ 20 min)

0. **Montar as peças** (na `Composicao`; a RC-08 já faz isto para as rotas de documentos, e as próximas histórias de arquivo reutilizam as mesmas peças):
   ```java
   Path raiz = configuracao.armazenamentoConfigurado() ? Path.of(configuracao.armazenamentoRaiz()) : null;
   ArmazenamentoPrivado armazenamento = new ArmazenamentoEmDisco(raiz);
   ConferenciaDeArquivo conferencia = ConferenciaDeArquivo.para(raiz);
   ```

1. **Receber e conferir** — `LimitesArquivo.exigirQuantidade(n)` para a quantidade e, para cada arquivo, `ConferenciaDeArquivo.conferir(fluxo, nomeOriginal)`. O resultado é `Aceito` (arquivo em quarentena) ou `Recusado(motivo)`; para devolver o erro padrão use `motivo.excecao()`. Em recusa nada permanece gravado.
2. **Guardar** — `ArmazenamentoPrivado.guardar(arquivoConferido)` grava na área privada (`DOCUMENT_STORAGE_PATH`) com nome interno aleatório e devolve o que registrar em `RC_DOCUMENTO` (`id` → `CAMINHO_INTERNO`, `tipo`, `tamanhoBytes`, `paginas`, `nomeExibicao` → `NOME_ORIGINAL`). Se a gravação da linha falhar depois, chame `remover(id)`.
3. **Declarar a rota** — `RotasPrivadas.registrar(caminho, servlet, Perfil.PACIENTE)` (ver [autorizacao.md](autorizacao.md)).
4. **Baixar** — busque o documento já filtrado pela dona, passe por `Propriedade.exigir` e responda com `DownloadSeguro.enviar(...)` (anexo, tipo vindo da conferência, `nosniff`, sem cache). Documento alheio → 404 idêntico ao inexistente; administrador e simulador → 403; sem token → 401.
5. **Texto de documento** — trate como `TextoNaoConfiavel`; para enviar a outro componente use só `delimitar(origem)`; valide toda saída de adaptador com `ValidadorDeSaidaEstruturada` (campos, códigos de regra e estados fechados). Saída inválida é descartada e vale o resultado determinístico com resumo padrão identificado.
6. **Resumo/texto exibido** — devolva `TextoSimulado` (`simulado: true`, `formato: "texto_puro"`); o serializador JSON já escapa `<`, `>`, `&` e `'`.

## Erros de arquivo

| Situação | HTTP | Código |
|---|---|---|
| Conteúdo não é PDF/JPEG/PNG | 415 | `TIPO_NAO_SUPORTADO` |
| Extensão ausente, não permitida ou diferente do conteúdo | 415 | `EXTENSAO_INCOMPATIVEL` |
| Acima de 5 MB (a leitura para no limite) | 413 | `ARQUIVO_MUITO_GRANDE` |
| PDF com mais de 5 páginas | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivo`) |
| PDF protegido por senha/criptografia | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivo`) |
| Vazio, truncado, corrompido ou imagem grande demais | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivo`) |
| Mais de 3 arquivos | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivos`) |

Corpo JSON malformado, vazio ou acima de 4 KB continua sendo 400 (contrato da RC-04); 413 é só para arquivo.

## Cabeçalhos em todas as respostas
`X-Content-Type-Options: nosniff` · `Cache-Control: no-store` · `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` · `Referrer-Policy: no-referrer` · `Strict-Transport-Security` quando `API_PUBLIC_URL` é `https`.

## Configuração
- `DOCUMENT_STORAGE_PATH`: raiz da área privada (fora do repositório e de qualquer pasta pública). Opcional para iniciar; sem ela a conferência funciona (quarentena temporária privada) e `guardar` falha com 503 `DEPENDENCIA_INDISPONIVEL`.
- `API_PUBLIC_URL`: endereço usado pelo app na demonstração remota. Se for remoto, tem de ser `https`; a API **recusa iniciar** com `http` remoto, URL inválida ou URL com usuário e senha. `http` só é aceito em `localhost`, `127.0.0.1`, `[::1]` e `10.0.2.2`.

## Verificações repetíveis (rodam no `.\mvnw.cmd clean verify`)
- `VarreduraDeSegredosTest`: nenhum segredo no repositório nem em `mobile/` (e, se houver `.env` local ou variáveis `DB_*`/`IOT_API_KEY`/token no ambiente, o valor real não aparece em nenhum arquivo; o valor nunca é impresso). Também prova que detecta um segredo plantado. Para rodar só ela: `.\mvnw.cmd -Dtest=VarreduraDeSegredosTest test`. Como reforço opcional, [gitleaks](https://github.com/gitleaks/gitleaks) pode ser usado, sem ser exigido.
- `SqlParametrizadoTest`: nenhuma consulta montada por concatenação (heurística sobre o código-fonte; não substitui revisão).
- `HigieneDeLogGeralTest` / `RedatorDeLogTest`: logs e respostas sem senha, token, chave, credencial de conexão ou pilha.
- `InjecaoSqlOracleTest` (opt-in, **somente leitura**, exige `DB_*` e `RC_TESTE_ORACLE=SIM`): entradas com aspas e comandos de banco não alteram a consulta no Oracle real.

## Limites conhecidos
Não há antivírus, OCR real, cifração do arquivo em repouso, cotas, retenção nem limitação de taxa. A conferência de tipo, tamanho e páginas não prova que o documento é legítimo nem legível. A garantia de que instruções em documentos não alteram critérios foi provada com um pipeline de exemplo em teste; o motor de regras e os adaptadores reais, quando existirem, devem repetir `InjecaoEmDocumentoTest`. A verificação de que o app Android usa HTTPS depende de o app existir.
