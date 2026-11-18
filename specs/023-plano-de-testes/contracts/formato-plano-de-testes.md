# Contrato: estrutura de `docs/testes/plano-de-testes.md` (RC-24)

Não há endpoint novo. Este contrato fixa o formato, para que quem executa e quem revisa verifiquem o plano da mesma forma.

## Seções do documento (nesta ordem)

1. **Aviso e escopo**: material fictício, leitura e resumo **simulados** (decisão de 12/11/2026), resultado não é aprovação do SUS, o plano não é execução; execução, evidências e retestes são da RC-25.
2. **Status do plano**: data, commit do plano, versão testável (a preencher na execução), **revisão por outro integrante: pendente**.
3. **Decisões aprovadas × funcionalidades pendentes** (tabela da pesquisa D3) e **diferenças entre a issue e o contrato** (extração/resumo "reais", 403 × 404, indicadores, alertas).
4. **Status e regras de uso** (Não executado, Aprovado, Reprovado, Bloqueado) e a regra "um caso aprova só se todo o esperado conferir".
5. **Ambiente e preparação**: como iniciar API, banco, app e simulador; variáveis; contas sintéticas; material (`samples/`, gabaritos, data de referência); procedimentos-base; e, como subseção 5.5, os **procedimentos controlados** (falha de IA com dublê, conectividade, sessão expirada, sensores) e a **restauração dos dados**.
6. **Convenção de evidências**: `docs/evidencias/<ID-DO-CASO>/` e conteúdo mínimo.
7. **Catálogo de casos**: T01..T16, cada caso no modelo abaixo.
8. **Percurso integrado P01**.
9. **Matriz de rastreabilidade** (história, regra e conjunto → casos; tipo de execução e impedimentos; lacunas declaradas em 9.5).
10. **Pendências do plano** (revisão, itens a conferir, o que a RC-25 deve fazer).

## Modelo de um caso

```markdown
#### T01-01 — Login com credenciais válidas
| Campo | Conteúdo |
|---|---|
| Grupo | funcionamento / falha / segurança |
| Histórias | RC-04 |
| Regra | não se aplica |
| Tipo de execução | API real + Oracle |
| Pré-condições | … |
| Ambiente | commit/versão: ___ · API: ___ · dispositivo: ___ (sem credenciais) |
| Entrada | … |
| Passos | 1. … 2. … |
| Resultado esperado | resposta … · interface … · dados … |
| Impedimento conhecido | nenhum |
| Resultado obtido | |
| Status | Não executado |
| Evidência | |
| Responsável e data | |
```

## Garantias verificáveis por leitura

1. Existem T01..T16 e P01; cada variação com resultado distinto é um caso.
2. Todo caso tem os 14 campos acima; resultado obtido, evidência, responsável e data estão em branco; status é "Não executado".
3. Toda entrada documental cita um caminho que existe em `samples/` e o gabarito correspondente.
4. As seis regras e os quatro resultados por regra aparecem em algum caso.
5. Todo comando citado existe no repositório (conferido por leitura, não por execução).
6. Nenhum caso afirma "extração real" ou "resumo real por IA"; casos com dublê dizem `Dublê (não é integração real)`.
7. T03 e os alertas na interface de T16 trazem o impedimento conhecido (RC-20 P1; RC-22).
8. A revisão por outro integrante consta como **pendente**.
