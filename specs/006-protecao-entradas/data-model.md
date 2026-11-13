# Data Model: Proteger entradas e processamento (RC-06)

**Sem tabelas, colunas ou scripts SQL novos.** `RC_DOCUMENTO` (RC-03) já guarda o que a conferência produz, com as mesmas restrições. Esta história define os tipos de código e a área privada de arquivos.

## Correspondência com `RC_DOCUMENTO`

| Resultado da conferência | Coluna | Restrição já existente |
|---|---|---|
| `TipoArquivo` (PDF, JPEG, PNG) | `TIPO` | `RC_DOCUMENTO_CK_TIPO` |
| tamanho em bytes | `TAMANHO_BYTES` | `BETWEEN 1 AND 5242880` |
| páginas (imagem = 1) | `PAGINAS` | `>= 1` e PDF `<= 5` |
| `IdInterno` | `CAMINHO_INTERNO` | `UNIQUE`; guarda **só o identificador**, nunca caminho absoluto |
| `NomeExibicao` | `NOME_ORIGINAL` | até 255 caracteres; só texto de exibição |

A linha é gravada pela história de envio, **depois** de `ArmazenamentoPrivado.guardar` ter sucesso; se a gravação da linha falhar, a história de envio chama `remover(idInterno)`. A dona do documento é a da cadeia `RC_DOCUMENTO.SOLICITACAO_ID → RC_SOLICITACAO.PACIENTE_ID` (RC-05).

## Área privada de arquivos

```text
<DOCUMENT_STORAGE_PATH>/
├── .quarentena/            # cópias temporárias em conferência (apagadas em recusa ou falha); sem a variável, a conferência usa um diretório temporário privado equivalente
└── <idInterno de 32 hex>   # arquivos aceitos; sem extensão, sem nome original
```

- Fica fora de qualquer diretório servido pela API (nenhuma rota serve arquivos estáticos).
- `uploads/` e `storage/` já estão no `.gitignore`; o caminho real vem só da configuração.
- Sem a variável, `guardar` falha com erro claro e a API continua subindo.

## Tipos de código (sem persistência)

| Tipo | Papel |
|---|---|
| `ConferenciaDeArquivo` | fluxo → quarentena (≤ 5 MB) → tipo real → extensão → páginas/dimensões → `ResultadoConferencia` |
| `MotivoRecusa` | lista fechada, cada item com status HTTP, código estável e mensagem (tabela abaixo) |
| `ArquivoConferido` | arquivo temporário aceito (tipo, tamanho, páginas) ainda não guardado |
| `ArmazenamentoPrivado` | contrato `guardar` / `abrir` / `remover`; implementação `ArmazenamentoEmDisco` |
| `IdInterno` | identificador aleatório de 32 hex; formato validado no construtor |
| `NomeExibicao` | nome original saneado, só para exibição |
| `DownloadSeguro` | grava o fluxo na resposta como anexo com os cabeçalhos seguros |
| `CabecalhosSegurosFiltro` | cabeçalhos de segurança em todas as respostas |
| `TextoSimulado` | DTO do resumo simulado: `texto`, `simulado=true`, `formato="texto_puro"` |
| `TextoNaoConfiavel` | texto de documento/entrada; `toString` sem conteúdo; `delimitar` para mensagens a outro componente |
| `ValidadorDeSaidaEstruturada` | contrato fechado da saída de extração/resumo |
| `RedatorDeLog` | mascara segredos em mensagens antes do log |
| `ConfiguracaoDeCanal` | recusa `API_PUBLIC_URL` remota sem https |

## Motivos de recusa (lista fechada)

| Motivo | HTTP | Código | Quando |
|---|---|---|---|
| `TIPO_NAO_SUPORTADO` | 415 | `TIPO_NAO_SUPORTADO` | assinatura não é PDF/JPEG/PNG |
| `EXTENSAO_INCOMPATIVEL` | 415 | `EXTENSAO_INCOMPATIVEL` | extensão ausente, não permitida ou diferente do tipo real |
| `TAMANHO_EXCEDIDO` | 413 | `ARQUIVO_MUITO_GRANDE` | mais de 5 MB (leitura interrompida no limite) |
| `EXCESSO_DE_PAGINAS` | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivo`) | PDF com mais de 5 páginas |
| `PDF_PROTEGIDO` | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivo`) | PDF com senha/criptografia |
| `ARQUIVO_ILEGIVEL` | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivo`) | vazio, truncado, corrompido, imagem acima do limite de dimensões |
| `EXCESSO_DE_ARQUIVOS` | 400 | `REQUISICAO_INVALIDA` (detalhe `arquivos`) | mais de 3 arquivos no pedido |

`ARQUIVO_MUITO_GRANDE` já consta em `docs/api/erros.md`; `EXTENSAO_INCOMPATIVEL` é novo. As mensagens orientam a pessoa (por exemplo, “Envie o PDF sem senha ou um novo arquivo”) sem citar caminho nem detalhe técnico.

## Configuração

| Variável | Uso | Obrigatória para iniciar? |
|---|---|---|
| `DOCUMENT_STORAGE_PATH` | raiz da área privada de arquivos (já reservada) | não; sem ela, a conferência funciona e `guardar` falha com erro claro |
| `API_PUBLIC_URL` (nova) | endereço pelo qual o app alcança a API na demonstração remota | não; se informada e remota, precisa ser `https` |
