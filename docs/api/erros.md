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
| 403 | `ACESSO_NEGADO` | identidade válida, mas o perfil não pode usar a rota (ex.: administrador em recurso individual de paciente, paciente em rota administrativa, chave do simulador fora da telemetria, conta na telemetria). Mensagem fixa: "Voce nao tem permissao para esta operacao." (RC-05) |
| 404 | `NAO_ENCONTRADO` | recurso inexistente **ou** de outra paciente, sessão de captura inexistente ou encerrada, identificador ilegível, rota inexistente. Corpo idêntico em todos os casos; mensagem fixa: "Recurso nao encontrado." (RC-05) |
| 405 | `METODO_NAO_PERMITIDO` | método HTTP não suportado pela rota |
| 409 | `CONFLITO_ESTADO` | operação incompatível com o estado ou a revisão atual |
| 413 | `ARQUIVO_MUITO_GRANDE` | arquivo acima de 5 MB |
| 415 | `TIPO_NAO_SUPORTADO` | tipo real diferente de PDF/JPEG/PNG |
| 422 | `REGRA_DE_NEGOCIO` | opcional: violação de regra (ex.: submeter com pendências) |
| 500 | `ERRO_INTERNO` | falha inesperada; mensagem genérica, detalhe só no log |
| 502/503 | `DEPENDENCIA_INDISPONIVEL` | banco ou adaptador indisponível |

Rotas inexistentes devolvem 404 `NAO_ENCONTRADO` no mesmo formato. Falha da IA ou da extração simuladas não é erro HTTP: preserva o resultado determinístico e devolve resumo padrão identificado.

## Ordem de verificação (RC-05)

Vale para toda rota privada, sempre nesta ordem:

1. **Autenticação** → sem identidade válida: `401 NAO_AUTENTICADO` (mesma resposta para qualquer motivo).
2. **Perfil permitido na rota** → `403 ACESSO_NEGADO`, antes de qualquer consulta ao recurso (por isso o administrador pedindo um recurso individual recebe 403 também quando o identificador não existe).
3. **Dona do recurso** → `404 NAO_ENCONTRADO` se o recurso não existe ou é de outra paciente; a busca já vem filtrada pela dona.
4. **Validação do pedido** → `400 REQUISICAO_INVALIDA` (e 413/415 quando couber). Fica depois da dona para que um corpo ruim não permita descobrir se um recurso alheio existe.
5. Regra de negócio e demais erros (409, 503…).

Método HTTP não previsto numa rota privada responde 405 somente depois de 401/403. As mensagens de 403 e 404 não citam dona, perfil exigido, identificador interno, SQL nem nome de arquivo.
