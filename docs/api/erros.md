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
| 400 | `REQUISICAO_INVALIDA` | corpo ou parâmetro inválido (detalhes por campo) |
| 401 | `CREDENCIAIS_INVALIDAS` | login com senha errada, e-mail inexistente, conta desativada ou senha acima de 72 bytes: sempre a mesma resposta (RC-04) |
| 401 | `NAO_AUTENTICADO` | token ausente, malformado, desconhecido, expirado ou encerrado, ou conta desativada: sempre a mesma resposta (RC-04) |
| 403 | `ACESSO_NEGADO` | perfil sem permissão (RC-05) |
| 404 | `NAO_ENCONTRADO` | recurso inexistente **ou** de outra paciente (não revelar existência) |
| 405 | `METODO_NAO_PERMITIDO` | método HTTP não suportado pela rota |
| 409 | `CONFLITO_ESTADO` | operação incompatível com o estado ou a revisão atual |
| 413 | `ARQUIVO_MUITO_GRANDE` | arquivo acima de 5 MB |
| 415 | `TIPO_NAO_SUPORTADO` | tipo real diferente de PDF/JPEG/PNG |
| 422 | `REGRA_DE_NEGOCIO` | opcional: violação de regra (ex.: submeter com pendências) |
| 500 | `ERRO_INTERNO` | falha inesperada; mensagem genérica, detalhe só no log |
| 502/503 | `DEPENDENCIA_INDISPONIVEL` | banco ou adaptador indisponível |

Rotas inexistentes devolvem 404 `NAO_ENCONTRADO` no mesmo formato. Falha da IA ou da extração simuladas não é erro HTTP: preserva o resultado determinístico e devolve resumo padrão identificado.
