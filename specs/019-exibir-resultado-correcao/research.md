# Research: RC-15 — resultado e orientação

**Data**: 2026-11-16. Pesquisa documental no repositório; não é evidência de teste do app.

## D1 — Superfície de entrega

**Decision**: Planejar a apresentação no aplicativo Android nativo em Kotlin, seguindo os quadros E3.3/E3.4 e o design system v1.0.
**Rationale**: A história pede exibição e orientação à paciente. `mobile/README.md` registra a plataforma escolhida e que o projeto Android ainda não foi criado; `specs/015-fluxo-telas-app/` e `mobile/design/` registram o desenho.
**Alternatives considered**: Nova página web ou alteração apenas da API não entregariam a tela móvel prevista. Redesenhar o canvas nesta etapa duplicaria a feature 015.

## D2 — Fonte de resultado e resumo

**Decision**: Usar a análise da RC-13 como fonte de resultado, revisão, protocolo, pendências e evidências; usar o resumo da RC-14 associado à mesma análise. A obtenção do resumo não bloqueia a lista determinística.
**Rationale**: `GET /solicitacoes/{id}/analises` devolve um retrato do pedido, `vigenteId`, análises e resumo armazenado; `POST /solicitacoes/{id}/resumo` gera ou repete o resumo da análise vigente. O contrato já permite fallback `PADRAO`.
**Alternatives considered**: Recalcular regras no cliente ou construir orientação a partir de texto livre criaria divergência e risco clínico. Nova rota de leitura não é necessária no contrato atual.

## D3 — Vigência e erro

**Decision**: Selecionar a análise vigente pelo `vigenteId` do retrato consultado; exibir análise antiga apenas como “Resultado anterior”. Em 409 na tentativa de resumo, recarregar o retrato antes de exibir novo conteúdo. Tratar ausência de análise, processamento, falha e rede como estados próprios.
**Rationale**: Alterar documento ou conferência retira a vigência sem apagar o histórico. O cliente não deve misturar revisões nem publicar uma conclusão não confirmada.
**Alternatives considered**: Escolher sempre o primeiro item da lista poderia tomar uma análise interrompida ou antiga como vigente; manter conteúdo antigo após 409 poderia atribuir resumo à revisão errada.

## D4 — Próxima ação por pendência

**Decision**: Mostrar `responsavel` e `explicacao` da regra sem alterar o valor recebido. Distinguir “responsável registrado pela regra” de “próxima ação”: na RC-R02, `PACIENTE` confere os documentos e, se o erro estiver no original médico, solicita revisão à unidade emissora; a unidade corrige o original. Mera divergência ou `NAO_LOCALIZADO` não comprova esse erro, portanto a orientação é condicional, sem criar um campo de evidência que a API não fornece. Arquivo ilegível ou não lido direciona à substituição existente quando permitida. Para arquivo ausente sem opção de inclusão após a primeira análise, explicar a limitação e orientar a obter o arquivo para o fluxo de reenvio quando disponível, sem botão inoperante. Não inferir “foto cortada” apenas de `ILEGIVEL`.
**Rationale**: `docs/protocolo-academico.md` registra RC-R02 com responsável `PACIENTE` mesmo quando o original exige revisão da unidade. O protocolo distingue não localizado de ausente e a API não permite acrescentar documento depois da primeira verificação; `specs/015-fluxo-telas-app/contracts/telas-operacoes.md` registra esse limite para a RC-16.
**Alternatives considered**: Mostrar “anexar documento” sem operação válida ou chamar toda informação ilegível de “ausente” induziria uma ação falsa.

## D5 — Evidência e texto não confiável

**Decision**: Relacionar `evidencias[].documentoId` aos `documentos[]` da mesma análise. Mostrar página e trecho somente quando `origem` existir; renderizar o trecho como texto literal.
**Rationale**: A RC-13 preserva a origem da revisão analisada quando disponível. O conteúdo do documento não pode agir como instrução nem ser transformado em HTML.
**Alternatives considered**: Buscar o documento atual para preencher lacunas poderia misturar revisões; fabricar trecho ou interpretar marcação violaria a spec e a constituição.

## D6 — Acessibilidade e validação

**Decision**: Reutilizar tokens e componentes v1.0; representar estado por rótulo e ícone, manter ordem de leitura, alvos de toque de 48 dp, texto base de 16 sp e suporte a fonte ampliada. Medir SC-005 com pessoas que não desenharam o fluxo quando houver app executável.
**Rationale**: `mobile/design/design-system.md` e `mobile/design/acessibilidade.md` registram essas metas. Os quadros desenhados não comprovam usabilidade nem comportamento no Android.
**Alternatives considered**: Considerar o design como teste concluído confundiria revisão visual com validação em aparelho e com usuária.

## Dependência de execução

O projeto Android ainda não existe na `main` em 16/11/2026. A implementação da RC-15 depende da base móvel, login e navegação do percurso. A versão exata de Kotlin/SDK e a árvore de código serão herdadas dessa base. Esta dependência não exige escolha de tecnologia nova nesta etapa nem autoriza marcar a RC-15 como implementada.
