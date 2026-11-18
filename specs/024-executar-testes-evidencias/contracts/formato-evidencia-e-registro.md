# Contrato: pasta de evidência e preenchimento do plano (RC-25)

Não há endpoint novo. Este contrato fixa o formato dos registros para que quem revisa verifique todas as execuções da mesma forma.

## 1. Pasta de evidência

`docs/evidencias/<ID-DO-CASO>/` (ex.: `docs/evidencias/T06-02/`; o percurso usa `docs/evidencias/P01/`).

```text
docs/evidencias/T06-02/
├── registro.md
└── bruto/
    ├── 01-login.txt          # saída do passo, mascarada
    ├── 02-envio-resposta.json
    └── 03-captura.png        # quando houver interface
```

## 2. Modelo de `registro.md`

```markdown
# T06-02 — <título do caso>

Plano: [plano-de-testes.md](../../testes/plano-de-testes.md) · Tipo de execução: <API real + Oracle | App no emulador | Dublê (não é integração real) | ...>
Leitura e resumo: **simulados** (decisão de 12/11/2026).

## Execução 1
| Item | Valor |
|---|---|
| Data e hora | 2026-11-18 14:05 (America/Bahia) |
| Versão | commit `abc1234` (alterações locais: só `docs/` e `specs/`) · API: <jar/versão> · dispositivo: <emulador ou n/a> |
| Responsável e execução | <nome do integrante responsável> · executado por <pessoa ou "com apoio do Claude Code"> |
| Pedido criado | id <n> (ou n/a) |
| Passos executados | 1. … 2. … (comandos com `<TOKEN>` no lugar do token) |
| Resultado bruto | `bruto/01-…`, `bruto/02-…` |
| Esperado × obtido | item a item (✔ confere / ✘ difere) |
| Status | Aprovado \| Reprovado \| Bloqueado |
| Motivo (se Bloqueado ou Reprovado) | … · achado `A-nn` |

## Execução 2 (reteste, se houver)
…mesmos itens; referência à correção (commit/arquivo) e ao achado fechado.

## Status final do caso
<status da última execução> — <responsável> — <data>
```

Regras: um item do esperado que difere reprova o caso; "Bloqueado" exige a tentativa descrita; caso por dublê traz o nome do teste, o comando e a saída, e a frase `Dublê (não é integração real)`.

## 3. Preenchimento do plano

Para cada caso executado, em `docs/testes/plano-de-testes.md`:

| Campo | Preenchimento |
|---|---|
| Ambiente | `commit abc1234 · API: <jar> · dispositivo: <emulador/n/a>` (sem credenciais) |
| Resultado obtido | uma a três linhas objetivas, esperado × obtido |
| Status | Aprovado, Reprovado ou Bloqueado; casos não tentados ficam "Não executado" |
| Evidência | link relativo `../evidencias/<ID>/registro.md` |
| Responsável e data | nome e `AAAA-MM-DD` |

Resultado esperado, Passos e Entrada **não** são editados. Se o plano estiver errado (ex.: rota inexistente), registrar como achado e corrigir o plano em item separado, explícito no resumo.

## 4. Mascaramento

| Dado | Substituir por |
|---|---|
| Token de sessão | `<TOKEN>` |
| Senha de demonstração | `<SENHA>` |
| `X-Chave-Simulador` / `IOT_API_KEY` | `<CHAVE>` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `<DB>` |

Capturas de tela não podem mostrar campo de senha preenchido em texto claro, nem o console com variáveis de ambiente.

## 5. Garantias verificáveis

1. Cada pasta citada no plano existe e tem `registro.md`.
2. Todo "Aprovado" no plano tem Evidência, Responsável e data preenchidos.
3. Todo "Bloqueado" tem a tentativa e o motivo na pasta.
4. Todo "Reprovado" tem achado ligado e permanece visível mesmo após reteste.
5. Busca por `Bearer `, `DB_PASSWORD=`, a senha de demonstração e a chave do simulador não retorna ocorrência real.
6. A soma das contagens do resumo é 94 e coincide com os status do plano.
