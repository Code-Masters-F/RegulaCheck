# Specification Quality Checklist: Substituir documento e excluir rascunho (RC-09)

**Purpose**: Validar completude e qualidade da especificação antes do planejamento
**Created**: 2026-11-13
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Sem detalhes de implementação (linguagem, framework, SQL); o contrato de API é citado só como "resposta documentada", sem códigos
- [x] Focada em valor para a paciente
- [x] Compreensível por quem não é da área técnica
- [x] Seções obrigatórias preenchidas

## Requirement Completeness

- [x] Nenhum marcador [NEEDS CLARIFICATION] restante
- [x] Requisitos testáveis e sem ambiguidade
- [x] Critérios de sucesso mensuráveis
- [x] Critérios de sucesso sem detalhes de tecnologia
- [x] Cenários de aceite definidos
- [x] Casos de borda identificados
- [x] Escopo delimitado
- [x] Dependências e premissas identificadas

## Feature Readiness

- [x] Todo requisito funcional tem critério de aceite (FR-001–FR-022 ↔ US1–US5 e SC-001–SC-008)
- [x] Cenários cobrem os fluxos principais (remover, substituir, excluir, pedido submetido, invalidação)
- [x] A feature atende os resultados mensuráveis
- [x] Sem vazamento de implementação na especificação

## Notes

- Decisões tomadas por padrão (premissas, sem pergunta ao usuário): política de arquivos (arquivo apagado, registro histórico mantido, exclusão do rascunho apaga tudo), exclusão só de rascunho, substituição em operação única, confirmação como valor explícito (forma exata no plano).
- FR-013 a FR-016 para estados de análise e submissão só poderão ser exercitados de ponta a ponta quando essas histórias existirem; no plano, serão testados com pedidos montados nesses estados. Isso deve constar como limitação na verificação.
- Pronto para `/speckit-clarify` (opcional) ou `/speckit-plan`.
