# Pesquisa e decisões — RC-01

Não havia `NEEDS CLARIFICATION` no contexto técnico. As decisões abaixo fecham as suposições da spec.

## D1. Localidade demonstrativa
- **Decisão**: "Município Exemplo" (fictício), sem UF real nem rede de saúde real; horário de referência de Salvador.
- **Rationale**: evita qualquer leitura de protocolo oficial (constituição I).
- **Alternativas**: usar município real (rejeitado: sugere exigência oficial).

## D2. Especialidade
- **Decisão**: consulta ambulatorial em cardiologia, apenas como contexto documental.
- **Rationale**: documentação típica simples (encaminhamento, identificação) e sem regra clínica; as regras não avaliam conteúdo clínico.
- **Alternativas**: ortopedia, dermatologia (equivalentes; sem ganho). Pode ser trocada sem alterar o motor.

## D3. Versão do protocolo
- **Decisão**: `PROTOCOLO-ACADEMICO-FICTICIO v0.1`, versionamento `vMAJOR.MINOR`; mudança de regra gera nova versão e análises guardam a versão usada (constituição VI).

## D4. Catálogo de regras (6)
- **Decisão**: RC-R01 a RC-R06 (ver contrato). Cinco obrigatórias e uma condicional (menor de 18 anos), para exercitar "não aplicável".
- **Rationale**: todas verificáveis só com documentos sintéticos; cobrem presença, coerência entre documentos, validade temporal e condicionalidade; uma delas (R04) cobre a responsabilidade da unidade emissora.
- **Alternativas**: regras de conteúdo clínico (rejeitado: viola limites clínicos); mais de 6 (rejeitado: limite do backlog).

## D5. Semântica de resultados
- **Decisão**: por regra: `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL`. Evidência não localizada em documento legível → `PENDENTE`; ilegível, protegido ou leitura vazia → `NAO_VERIFICAVEL`.
- **Rationale**: não encontrar não prova ausência (CLAUDE.md §11).

## D6. Composição do resultado geral
- **Decisão**: precedência: qualquer `PENDENTE` → `COM_PENDENCIAS`; senão qualquer `NAO_VERIFICAVEL` → `REQUER_CONFERENCIA`; senão `SEM_PENDENCIAS_IDENTIFICADAS`. `NAO_APLICAVEL` não pesa.
- **Rationale**: alinha com os estados propostos em `docs/arquitetura.md`; "sem pendências" nunca equivale a aprovação do SUS. Resultado `FALHA_ANALISE` é de processamento, fora do catálogo.

## D7. Local e formato
- **Decisão**: Markdown em `docs/`, em dois arquivos; contrato do catálogo em tabela que mapeia 1:1 para futuras tabelas Regra/Protocolo (RC-03) e para o motor (RC-12).
- **Alternativas**: JSON/YAML do catálogo (adiado: o formato de carga será decidido em RC-03/RC-12 para não antecipar implementação).

## D8. Verificação
- **Decisão**: revisão cruzada com CLAUDE.md, constituição v2.0.0 e decisoes.md; exercício em papel com 5 casos sintéticos por duas pessoas (SC-002); resultado registrado honestamente (se não executado, marcado pendente).
