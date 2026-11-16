# Feature Specification: Resumo de pendências simulado (RC-14)

**Feature Branch**: branch atual
**Created**: 2026-11-16
**Status**: Implementada e verificada em memória em 16/11/2026; verificação Oracle pendente
**Backlog**: RC-14, issue #36; depende de RC-06, RC-12 e RC-13.
**Input**: Como paciente, quero receber uma explicação simples para saber o que providenciar antes de reenviar. Resumo por IA simulada, a partir de regras e resultados verificados, com códigos conhecidos, ações concretas, limites clínicos e fallback recuperável.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entender pendências (Priority: P1)

A paciente solicita um resumo da análise vigente e vê, em português simples, cada pendência, sua regra e o que fazer. A simulação é identificada junto ao texto.

**Why this priority**: Orienta o próximo passo da paciente.
**Independent Test**: Solicitar resumo para análise vigente com pendências sintéticas e conferir texto, referências e ações.

**Acceptance Scenarios**:

1. **Given** análise vigente com pendências, **When** a paciente solicita resumo, **Then** recebe explicação em português, código de cada regra e ação concreta, identificada como simulada.
2. **Given** análise vigente sem pendências, **When** solicita resumo, **Then** recebe mensagem sem promessa de aprovação.
3. **Given** resultado que requer conferência, **When** solicita resumo, **Then** recebe orientação para conferir a transcrição, sem afirmar ausência do dado original.

### User Story 2 - Recuperar falha (Priority: P2)

Se a geração simulada falha ou devolve conteúdo inválido, a paciente vê explicação padrão identificada e pode tentar novamente. As pendências determinísticas continuam visíveis.

**Why this priority**: Falha de orientação não pode ocultar as regras.
**Independent Test**: Simular falha e saída inválida; conferir fallback, preservação das pendências e nova tentativa.

**Acceptance Scenarios**:

1. **Given** falha do gerador, **When** solicita resumo, **Then** vê explicação padrão identificada, pendências originais e opção de tentar novamente.
2. **Given** saída com código desconhecido ou estrutura inválida, **When** solicita resumo, **Then** a saída é rejeitada e ocorre o mesmo fallback.

### Edge Cases

- Pedido inexistente ou de outra paciente recebe a mesma recusa.
- Sem análise vigente, o resumo não é gerado.
- Alteração da revisão ou conferência durante geração impede publicar resumo obsoleto.
- Repetição sem mudança não duplica o resumo; nova tentativa após fallback pode substituí-lo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Somente a paciente dona do pedido pode solicitar ou consultar seu resumo.
- **FR-002**: A geração usa exclusivamente a análise vigente, os resultados verificados e os dados fechados das regras; conteúdo livre de documentos não constitui instrução.
- **FR-003**: Cada item referencia código de regra existente na análise e ação concreta coerente com a pendência.
- **FR-004**: O mesmo contrato estruturado e a mesma validação são usados para a saída simulada e eventual integração futura; códigos desconhecidos ou saída fora do contrato são rejeitados.
- **FR-005**: Resumo não altera resultado de regra, pendência, estado ou vigência; não inventa exigências, diagnostica, prescreve nem promete vaga ou aprovação.
- **FR-006**: Resposta e documentação identificam sempre o resumo como simulado.
- **FR-007**: Falha ou saída inválida preserva pendências determinísticas, fornece explicação padrão identificada e permite tentar novamente.
- **FR-008**: Resumo de análise desatualizada não pode ser publicado como vigente.
- **FR-009**: A paciente pode obter o resumo junto à consulta da análise vigente e solicitá-lo novamente após fallback.

### Key Entities

- **Resumo**: explicação geral, itens por regra, indicação de simulação e origem padrão ou gerada; associado a uma análise.
- **Item do resumo**: código de regra e ação concreta, associado à pendência correspondente.
- **Análise vigente**: fonte imutável dos resultados determinísticos e vínculo do resumo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em 100% dos cenários com pendências, cada item corresponde a uma regra pendente da análise e indica uma ação.
- **SC-002**: Em 100% das falhas e saídas inválidas verificadas, as pendências originais seguem disponíveis e a paciente pode tentar novamente.
- **SC-003**: Em 100% das respostas, a simulação é identificada e não há alegação de diagnóstico, vaga ou aprovação.
- **SC-004**: Em 100% das tentativas com código desconhecido, a saída é rejeitada.
- **SC-005**: Em 100% das consultas após gerar o resumo, ele aparece ligado à análise correspondente.

## Assumptions

- O gerador é simulado, sem provedor externo, conforme decisão de 12/11/2026.
- As ações vêm do protocolo acadêmico fictício v0.1 e das pendências verificadas.
- A RC-13 fornece análise vigente, histórico e autorização por pedido.
- Dados e documentos usados na verificação são sintéticos.
