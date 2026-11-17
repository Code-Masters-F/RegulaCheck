# Contrato de apresentação: resultado e pendência (RC-15)

Este contrato define a resposta visível da tela. **Não cria endpoint**. Fontes: [análises RC-13](../../014-persistir-analises-historico/contracts/api-analises.md), [resumo RC-14](../../018-resumo-pendencias-simulado/contracts/api-resumo.md), [textos E3.3/E3.4](../../../mobile/design/textos.md).

**Recorte backend de 17/11/2026:** `Analise` inclui `rotuloResultado` e `pendencias[].orientacao` em `POST /solicitacoes/{id}/analises`, `GET /solicitacoes/{id}/analises` e `POST /solicitacoes/{id}/resumo`. São campos textuais aditivos, derivados da análise gravada; não alteram `resultadoGeral`, `responsavel`, resumo, evidência ou revisão. `orientacao` da RC-R02 usa “se o erro estiver no documento médico original” e não diagnostica o documento. A API não informa se a ação de reenvio está disponível no estado atual. As obrigações de tela abaixo continuam pendentes até existir cliente Android.

## Entrada e seleção

1. Com sessão da paciente, consultar `GET /solicitacoes/{id}/analises`.
2. Para resultado atual, selecionar somente o item cujo `id` é `vigenteId` e cuja `vigente` é `true`. Se não houver, mostrar estado sem análise vigente; análises antigas só podem ser abertas como “Resultado anterior”.
3. Usar `resultadoGeral`, `protocolo`, `resumo`, `pendencias`, `evidencias` e `documentos` da análise selecionada. Não juntar dados de outra revisão.
4. Se `resumo` estiver ausente e a análise for vigente, mostrar “O resumo não está disponível agora. Os critérios abaixo continuam valendo.” e oferecer “Tentar de novo” para obtê-lo pelo `POST /solicitacoes/{id}/resumo`. Se vier `PADRAO`, mostrar a explicação padrão identificada como SIMULADO, manter resultado e lista determinística e oferecer “Tentar de novo”. Em 409, consultar novamente o histórico antes de qualquer nova apresentação.

## Resultado visível

| Dados/estado | Apresentação obrigatória | Ação permitida |
|---|---|---|
| `COM_PENDENCIAS` | “Com pendências”; resumo identificado SIMULADO; “O que falta” com critério, tipo e responsável registrado pela regra | abrir detalhe da pendência |
| `REQUER_CONFERENCIA` | “Confira a leitura”; dado não verificável não é declarado ausente | abrir detalhe ou navegar à conferência já existente |
| `SEM_PENDENCIAS_IDENTIFICADAS` | “Sem pendências identificadas”; protocolo demonstrativo; nenhuma garantia de aceitação, vaga ou aprovação do SUS | consultar critérios; conclusão simulada pertence à RC-17 |
| `execucao=EM_ANDAMENTO` ou sem análise | andamento ou ausência em texto; nenhuma conclusão nova | atualizar consulta quando cabível |
| análise `vigente=false` | “Resultado anterior”, revisão avaliada e indicação de que não vale para a revisão atual | consultar como histórico; não orientar a partir dela como se fosse atual |
| erro de rede/503 | “Não foi possível consultar o resultado agora”; nenhuma conclusão nova | “Tentar novamente” a consulta |
| 401 | “Sessão expirada” | voltar ao login |
| 403 | “Acesso negado” | voltar à lista dos próprios pedidos |
| 404 | “Pedido não encontrado” sem revelar existência de pedido alheio | voltar à lista dos próprios pedidos |

Resultado, tipo de pendência e responsável usam **texto e ícone**; cor é apoio. Aviso acadêmico aparece em todas as telas de resultado. Resumo e leitura são marcados SIMULADO.

## Detalhe de pendência

- Mostrar `codigoRegra`, `descricaoRegra`, `tipo`, `explicacao`, `responsavel` e `protocolo.versao` da mesma análise. Rotular `responsavel` como “Responsável registrado pela regra” quando a próxima ação envolver outra pessoa ou unidade; não reescrever o enum da API.
- Para cada evidência, obter `nomeArquivo` de `documentos[]` pelo `documentoId`. Mostrar campo/situação e `origem.pagina`/`origem.trecho` apenas se existirem. Trecho é texto literal, inclusive `<`, `>` e `&`.
- `ILEGIVEL` ou `DOCUMENTO_NAO_LIDO`: orientar arquivo mais nítido ou substituição quando houver documento substituível; não afirmar que uma foto foi cortada sem evidência específica.
- `NAO_LOCALIZADO`: manter a distinção entre campo não localizado na leitura e comprovadamente ausente no original. Quando a pendência clínica indicar `UNIDADE_EMISSORA`, orientar a solicitar revisão à unidade sem afirmar que o original está errado sem evidência.
- RC-R02 com `responsavel=PACIENTE`: orientar a conferir a divergência e exibir “Se o erro estiver no documento médico original, solicite a correção à unidade emissora”; a unidade corrige o original. O responsável registrado pela regra continua `PACIENTE`. A API atual não informa qual documento está errado; não afirmar que mera divergência prova erro clínico.
- Documento médico incorreto ou conteúdo clínico ausente: indicar condicionalmente a unidade emissora quando a origem do problema não estiver comprovada pela resposta. Nunca oferecer edição do original no aplicativo.
- Arquivo ausente após primeira análise: explicar em linguagem da paciente que não é possível acrescentar outro arquivo nesse ponto do fluxo; orientar a obter o arquivo para o fluxo de reenvio quando disponível e oferecer substituição somente de arquivo existente e permitido. A ampliação do contrato pertence à RC-16.

## Erros e limites

- 401: encerrar sessão local e voltar ao login; 403: acesso negado; 404: pedido não encontrado, sem revelar pedido alheio; 503/rede: nova consulta possível.
- Falha ou `PADRAO` no resumo não muda resultado nem esconde pendências. Toques repetidos durante a tentativa de resumo não geram apresentações duplicadas.
- Nenhum texto pode afirmar diagnóstico, urgência, aprovação ou envio real ao SUS. Nenhuma saída da IA altera critério, responsável ou estado do pedido.
