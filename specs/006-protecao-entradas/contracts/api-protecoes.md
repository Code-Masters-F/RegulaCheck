# Contrato: proteções de entrada, arquivos e texto (RC-06)

Complementa [`docs/api/erros.md`](../../../docs/api/erros.md) e o contrato de autorização da RC-05. Valem para **toda** rota que receba arquivo, devolva arquivo ou devolva texto derivado de entrada. Formato de erro: o padrão do projeto (`codigo`, `mensagem`, `detalhes`, `idRastreamento`).

## Limites do MVP (constantes centrais)

| Item | Limite |
|---|---|
| Arquivos por pedido | 3 |
| Tamanho por arquivo | 5 MB (5.242.880 bytes) |
| Tipos aceitos | PDF, JPEG, PNG (pelo conteúdo real) |
| Extensões aceitas | `.pdf`, `.jpg`, `.jpeg`, `.png` (devem corresponder ao tipo real) |
| Páginas de PDF | 5 |
| Pixels de imagem | 25 milhões (largura × altura) |
| Nome de exibição | 100 caracteres |

## Respostas de recusa de arquivo

| Situação | Status | Código | Mensagem (orientação) |
|---|---|---|---|
| Conteúdo não é PDF/JPEG/PNG | 415 | `TIPO_NAO_SUPORTADO` | “Envie um arquivo PDF, JPEG ou PNG.” |
| Extensão ausente, não permitida ou diferente do conteúdo | 415 | `EXTENSAO_INCOMPATIVEL` | “A extensão do arquivo não corresponde ao seu conteúdo.” |
| Acima de 5 MB | 413 | `ARQUIVO_MUITO_GRANDE` | “O arquivo excede 5 MB.” |
| PDF com mais de 5 páginas | 400 | `REQUISICAO_INVALIDA` | detalhe `arquivo`: “O PDF pode ter no máximo 5 páginas.” |
| PDF protegido por senha | 400 | `REQUISICAO_INVALIDA` | detalhe `arquivo`: “PDF protegido por senha. Envie uma versão sem proteção.” |
| Vazio, truncado, corrompido ou imagem grande demais | 400 | `REQUISICAO_INVALIDA` | detalhe `arquivo`: “Não foi possível ler o arquivo. Envie um novo arquivo.” |
| Mais de 3 arquivos | 400 | `REQUISICAO_INVALIDA` | detalhe `arquivos`: “No máximo 3 arquivos por pedido.” |

Observação: `TIPO_NAO_SUPORTADO` (415) também é usado pela RC-04 quando o corpo de uma rota JSON não é `application/json`; o significado muda conforme a rota e a mensagem diferencia. Corpo JSON malformado, vazio ou acima de 4 KB continua sendo **400** `REQUISICAO_INVALIDA` (contrato da RC-04); 413 é só para arquivo.

Regras: nada é gravado em disco nem no banco em qualquer recusa; a mensagem nunca cita caminho, nome interno, biblioteca ou pilha.

## Download

| Identidade | Resposta |
|---|---|
| Dona do documento | 200 com o arquivo |
| Outra paciente (documento existe) | 404 `NAO_ENCONTRADO` — idêntico ao de documento inexistente |
| Administrador | 403 `ACESSO_NEGADO` |
| Simulador (chave) | 403 `ACESSO_NEGADO` |
| Sem autenticação | 401 `NAO_AUTENTICADO` |

Cabeçalhos do 200: `Content-Type` do tipo real guardado; `Content-Disposition: attachment; filename*=UTF-8''<nome saneado>`; `X-Content-Type-Options: nosniff`; `Cache-Control: no-store`; `Content-Length`. Arquivo registrado mas ausente da área privada: 500 `ERRO_INTERNO` com `idRastreamento` (sem caminho).

## Cabeçalhos em todas as respostas

`X-Content-Type-Options: nosniff` · `Cache-Control: no-store` · `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` · `Referrer-Policy: no-referrer` (e `Strict-Transport-Security` quando `API_PUBLIC_URL` é https).

## Textos derivados de entrada (resumo simulado e similares)

- JSON com `<`, `>`, `&` e `'` escapados (`<` …): o texto decodificado pelo cliente é o original, mas a resposta nunca contém marcação literal.
- DTOs de texto simulado trazem `simulado: true` e `formato: "texto_puro"`. O app deve exibir como texto (sem interpretar HTML).

## Conteúdo de documento (regra de uso para as histórias futuras)

1. Todo texto vindo de arquivo entra no código como `TextoNaoConfiavel`.
2. Para enviar a outro componente (adaptador de IA/extração), use somente `delimitar(...)`.
3. Toda saída de adaptador passa por `ValidadorDeSaidaEstruturada`; violação → descartar e usar o resultado determinístico com resumo padrão identificado.
4. Critérios, perfil e estado da solicitação nunca são lidos do conteúdo do documento.

## Configuração segura

- `DOCUMENT_STORAGE_PATH`: raiz da área privada; fora de pasta pública e do repositório.
- `API_PUBLIC_URL`: se informada e remota, deve ser `https://`; caso contrário a API não inicia.
- Segredos só em variáveis de ambiente / `.env` local ignorado; nenhum no app.
