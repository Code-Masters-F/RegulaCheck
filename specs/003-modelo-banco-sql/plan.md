# Implementation Plan: Modelo de banco e scripts SQL (RC-03)

**Branch**: `003-modelo-banco-sql` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-modelo-banco-sql/spec.md`

## Summary

Entregar o modelo relacional (diagrama e dicionário de dados) e os scripts Oracle do RegulaCheck: criação do esquema com tabelas `RC_*`, carga sintética, consultas de uso, testes de restrições e um script de limpeza separado e destrutivo. Diferente das histórias anteriores, o Oracle 19c da faculdade já está acessível (verificado em 12/11/2026), então os scripts serão **executados de verdade** nele e o resultado, registrado. Sem código de produção, JDBC, ORM nem alteração do `backend/`.

## Technical Context

**Language/Version**: SQL e PL/SQL para Oracle Database 19c Enterprise Edition (AL32UTF8). Scripts compatíveis com SQL Developer, SQLcl e SQL*Plus (`;` encerra instrução SQL; `/` encerra bloco PL/SQL).

**Primary Dependencies**: nenhuma no repositório. Para verificar: driver `ojdbc11` 23.26.4.0.0 e um executor de scripts **fora do repositório** (scratchpad), e, só para gerar os hashes da carga, `at.favre.lib:bcrypt` 0.10.2 (dependência `bytes` 1.6.1) num programa descartável. A mesma biblioteca é a candidata da RC-04 para a API.

**Storage**: Oracle da faculdade (`oracle.fiap.com.br:1521`, SID `orcl`), usuário individual de cada aluno (nunca versionado); esquema com tabelas `RC_*` convivendo com `PEQUENOVAREJO` e `VENDA` (não tocar).

**Testing**: script `04_testes_restricoes.sql` (cada violação deve falhar com o código ORA esperado, em blocos PL/SQL que capturam a exceção) e conferência das consultas contra resultados esperados documentados; execução registrada com data e saída real.

**Target Platform**: Oracle 19c; leitores: equipe e avaliador.

**Project Type**: Banco de dados (scripts e documentação).

**Performance Goals**: N/A (volume de demonstração pequeno). Índices apenas para chaves estrangeiras usadas pelas consultas.

**Constraints**: prefixo `RC_` em tabelas, restrições, índices e sequências; identificadores até 30 bytes por precaução (limite 128 no 19c, mas evita problemas em ferramentas antigas); sem DDL destrutivo automático; sem credenciais no repositório; dados sintéticos; senhas só em hash BCrypt.

**Scale/Scope**: 12 tabelas, ~25 restrições de valor, 3 contas, 1 protocolo, 6 regras, cerca de 4 solicitações de exemplo, 6 consultas.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação |
|---|---|
| I. Escopo acadêmico e dados sintéticos | Atende: contas, nomes e documentos fictícios; protocolo fictício. |
| II. Limites clínicos | Atende: nenhuma coluna de diagnóstico, urgência ou autorização; pendências só documentais. |
| III. Backend Java em monólito modular | Atende: nenhuma mudança no backend; Oracle via SQL, sem ORM. |
| IV. Segurança e autorização | Atende: senha só como hash BCrypt; sem segredo no Git (credenciais só no `.env`); arquivos referenciados por caminho interno, não expostos. Autorização por recurso permanece na API (RC-05). |
| V. Regras determinísticas, IA subordinada | Atende: o resultado por regra e a pendência são dados do motor; resumo simulado marcado (`SIMULADO`). |
| VI. Persistência, revisões e confiabilidade | Atende: revisão como entidade, vigência única por pedido por índice, análise ligada à revisão e à versão do protocolo, submissão única; esquema nunca alterado de forma destrutiva automática; scripts aditivos e numerados. |
| VII. Verificação honesta | Atende: execução real no Oracle com saída registrada; o que não for executado é marcado como não verificado. |
| VIII. Mudanças mínimas, documentadas | Atende: só `database/`, `docs/` e `specs/`; cabeçalho em cada script. |
| Fluxo SDD | Atende: specify → plan; seguem tasks → analyze → implement. |

**Resultado**: sem violações. Ressalva operacional: o script de limpeza (`90_`) é destrutivo por natureza; fica separado, com guarda de confirmação e restrito a objetos `RC_`. Reavaliação pós-design: mantém-se sem violações.

## Project Structure

### Documentation (this feature)

```text
specs/003-modelo-banco-sql/
├── plan.md
├── research.md
├── data-model.md                 # ER (Mermaid), dicionário de dados, restrições e cardinalidades
├── quickstart.md
├── contracts/
│   └── scripts-sql.md            # ordem, finalidade e garantias de cada script; consultas e resultados esperados
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
database/
├── README.md                          # visão geral, ordem de execução, conexão, verificação registrada
└── scripts/
    ├── README.md                      # finalidade, ordem e efeito de cada script
    ├── 01_criar_esquema.sql           # DDL (tabelas, restrições, índices)
    ├── 02_carga_sintetica.sql         # contas, protocolo, regras e exemplos
    ├── 03_consultas_uso.sql           # 6 SELECTs com JOIN e agregação
    ├── 04_testes_restricoes.sql       # violações que devem ser rejeitadas
    └── 90_limpeza_DESTRUTIVA.sql      # remove só objetos RC_; exige confirmação explícita
docs/
├── modelo-de-dados.md                 # NOVO: ER e dicionário (publicado a partir do data-model.md)
├── arquitetura.md, decisoes.md        # ALTERADOS
```

**Structure Decision**: scripts numerados em `database/scripts/` (ordem = nome). O `docs/modelo-de-dados.md` publica o diagrama e o dicionário; o `data-model.md` da spec é a origem. O executor usado na verificação fica fora do repositório, pois esta história não inclui código de acesso a dados.

## Complexity Tracking

Sem violações a justificar.
