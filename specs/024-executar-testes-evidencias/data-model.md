# Modelo de dados: Executar testes e registrar evidências (RC-25)

Não há tabela nem entidade de código. O "modelo" é o conjunto de registros em Markdown que a rodada produz.

## Caso (já existe no plano)

Campos fixos (14), definidos pela RC-24: Grupo, Histórias, Regra, Tipo de execução, Pré-condições, Ambiente, Entrada, Passos, Resultado esperado, Impedimento conhecido, **Resultado obtido**, **Status**, **Evidência**, **Responsável e data**.

A RC-25 só preenche os quatro últimos e o campo **Ambiente** (commit/versão, API, dispositivo, sem credenciais). Resultado esperado e Passos não são editados para coincidir com o obtido.

### Estados do Status

```text
Não executado ──tentou e o esperado confere──────────────▶ Aprovado
Não executado ──tentou e algo diverge────────────────────▶ Reprovado
Não executado ──tentou e há impedimento registrado───────▶ Bloqueado
Reprovado ─────correção + reteste confere────────────────▶ Aprovado (nova execução; a falha fica no histórico)
Bloqueado ─────impedimento removido + nova tentativa─────▶ Aprovado | Reprovado
```

Transições proibidas: Não executado → Aprovado sem execução; qualquer status → Aprovado por evidência de outra data.

## Execução

Uma tentativa de um caso: número da execução (1, 2…), data e hora, commit, executor, passos, resultado bruto, status da execução. Um caso pode ter várias (falha e reteste). O status do caso no plano é o da **última** execução; as anteriores permanecem na pasta.

## Evidência (pasta `docs/evidencias/<ID-DO-CASO>/`)

| Item | Conteúdo |
|---|---|
| `registro.md` | cabeçalho do caso, execuções (uma seção por execução), status final, executor, achados ligados |
| `bruto/` | respostas HTTP, saídas de comando, relatórios de teste, capturas; sem credenciais |

Regra: o `id` do pedido criado consta no `registro.md`; token, senha, chave e `DB_*` aparecem mascarados.

## Achado

Identificador `A-nn`; caso(s) de origem; descrição objetiva (esperado × obtido); gravidade (**bloqueador** | **não bloqueador**); evidência; destino (**corrigido** com referência ao reteste | **pendente** | **fora do escopo**).

## Resumo da rodada (`docs/testes/resultado-da-rodada.md`)

Versão testada e ambiente; contagens por status (Aprovado, Reprovado, Bloqueado, Não executado — soma 94); tabela caso → status → evidência; achados; defeitos corrigidos e retestes; lacunas (seção 9.5 do plano) e o que mudou nelas; pendências (revisão por outro integrante).

## Invariantes

1. Soma das contagens por status = 94 (total de casos do plano).
2. Todo "Aprovado" tem `docs/evidencias/<ID>/registro.md` existente.
3. Todo "Bloqueado" tem tentativa e motivo na pasta.
4. Todo "Reprovado" tem achado ligado.
5. Nenhum arquivo versionado contém token, senha, chave ou `DB_*` reais.
