# Research: Fluxo de telas e informação de cada tela

Fase 0 do plano. Cada decisão cita a fonte no backend ou no design system. Regra de governo (spec, Clarifications): **a tela acompanha o backend**; decisões aqui são de apresentação e navegação, nunca de negócio.

Fontes lidas em `origin/main` (merge do PR #13, `0091634`, 13/11/2026): `CONTRATO_API.md`, `docs/api/estados-e-revisoes.md`, `docs/api/contratos-dados.md`, `docs/protocolo-academico.md`, contratos das specs 004, 008, 009, 010, 011, 012 e 013; issues RC-15 (#37), RC-16 (#38), RC-17 (#39), RC-18 (#41), RC-22 (#47).

## D1. Situação de cada operação usada pelas telas

- **Decision**: classificar cada operação em *implementada* (RC-04, RC-07 a RC-11) ou *contratada* (análises RC-13, resumo RC-14, reenvio RC-15/16, submissão RC-17, indicadores RC-20, sensores RC-21/22). Quadros que dependem de operação contratada levam a anotação "contrato provisório".
- **Rationale**: as telas de resultado, comprovante, sensores e administrador precisam existir até 16–17/11, antes das histórias de backend. O formato vem de `docs/api/contratos-dados.md` (análise, pendência, submissão) e do contrato interno da RC-12 (`AvaliacaoDoPedido`).
- **Alternatives considered**: esperar a publicação das rotas (atrasa o percurso principal além do marco de 16/11); desenhar sem anotação (esconde o risco de mudança).

## D2. Ordem do percurso em rascunho

- **Decision**: Anexar → Ler documentos → Conferir → Verificar critérios.
- **Rationale**: documento novo só é aceito em `RASCUNHO` (RC-08, 409 fora dele); a leitura não muda o estado (RC-10); a conferência não muda o estado nem a revisão (RC-11); a primeira análise leva `RASCUNHO → PROCESSANDO` (`estados-e-revisoes.md`). Logo a ordem é imposta pelo backend, e a tela avisa antes da primeira verificação que anexar deixará de ser possível.
- **Alternatives considered**: verificar antes de conferir (permitido pelo backend, mas o protocolo trata campo não confirmado como não verificável, gerando "Confira a leitura"; a tela mostra a contagem e não bloqueia — D6).

## D3. Tela central do pedido (E1.3)

- **Decision**: uma tela "Pedido" com dados do pedido, superfície "Próximo passo" (uma ação principal) e seções navegáveis: Documentos, Leitura e conferência, Resultado.
- **Rationale**: RC-18 exige "tela de detalhe" com ação principal por estado ("anexar, conferir leitura, corrigir, analisar novamente ou consultar simulação"). O estado sozinho não distingue as quatro situações do rascunho; a tela combina o estado do pedido com a existência e a situação da leitura e da conferência (consultas separadas do backend), o que é composição de dados, não regra nova.
- **Alternatives considered**: abrir direto a tarefa do estado (perde o lugar para dados do pedido, exclusão e troca de serviço).

## D4. Identificação do pedido

- **Decision**: "Pedido {id}" (ex.: "Pedido 12"). Exemplos e `dados-ficticios.md` trocam `PED-000X` por números.
- **Rationale**: o pedido só tem `id` decimal opaco (`contratos-dados.md`); o comprovante terá referência própria (RC-17). Formatar um código inexistente induziria a paciente a usá-lo como protocolo.
- **Alternatives considered**: `PED-` + id com zeros (parece código oficial; nenhum dado sustenta).

## D5. Anexo: tipo antes da origem

- **Decision**: escolher o tipo documental (Encaminhamento, Identificação ou cartão do SUS, Autorização do responsável, Outro) e depois a origem (Fotografar ou Escolher arquivo), numa folha de escolha.
- **Rationale**: `tipoDocumental` é obrigatório no envio (RC-08). Escolher antes evita perder o arquivo numa recusa por tipo ausente.
- **Alternatives considered**: tipo depois do arquivo (mais um passo depois da escolha, com risco de abandono).

## D6. Verificar com campos sem decisão

- **Decision**: não bloquear; antes de verificar, um diálogo (E3.8) mostra quantos campos identificados estão sem decisão e que contarão como não verificados, e — na primeira verificação de um rascunho — que depois não será possível anexar documento novo.
- **Rationale**: o backend não condiciona a análise à conferência (`liberadoParaSubmissao` é da submissão; a RC-12 avalia com o que houver). A tela não cria bloqueio.
- **Alternatives considered**: bloquear o botão (regra de negócio nova, rejeitada pelo responsável).

## D7. Conferência: um documento por vez

- **Decision**: "Documento {i} de {n}"; "Salvar e continuar" grava as decisões daquele documento num único envio; no último, "Concluir conferência".
- **Rationale**: o backend aceita de 1 a 18 decisões por envio, tudo ou nada, e a decisão pode ser refeita; salvar por documento cabe no contrato e reduz a tela a 6 campos. Decisão de tela reversível (spec, Clarifications).
- **Alternatives considered**: lista única com até 18 campos e um só envio (rolagem longa, erro de lote afeta tudo).

## D8. Decisões possíveis por situação do campo

- **Decision**: identificado → "Está correto" (`CONFIRMADA`) ou "Está diferente", sob a pergunta "Confere com o seu documento?" (`DIVERGENTE` + `VALOR_DIFERENTE_DO_DOCUMENTO`); não localizado → só "Esta informação está no documento" (`DIVERGENTE` + `CAMPO_EXISTE_E_NAO_FOI_LIDO`); ilegível → nenhuma; documento não lido → nenhuma, com "Substituir arquivo".
- **Rationale**: é exatamente o que o PUT da conferência aceita; o resto é 422 `DECISAO_NAO_APLICAVEL` (RC-11). Oferecer só o aceito evita o erro.
- **Alternatives considered**: nenhuma; outro conjunto contradiz o contrato.

## D9. Ordem dos campos na conferência

- **Decision**: primeiro identificados e ilegíveis, depois "Não localizados nesta leitura", com rótulos legíveis dos seis códigos.
- **Rationale**: a leitura devolve os seis códigos para todo documento lido, inclusive os que não se aplicam ao tipo (ex.: motivo no cartão do SUS). Agrupar não esconde nada (todos continuam visíveis) e põe primeiro o que pede decisão.
- **Alternatives considered**: esconder campos pelo tipo do documento (a tela estaria inferindo relevância, o que o backend não faz: o tipo declarado não é prova, RC-12).

## D10. Correção depois da verificação

- **Decision**: a tela oferece apenas substituir e remover; anexar não aparece fora de rascunho. Para pendência de documento ausente, explica a limitação e oferece substituir um documento enviado. Depois de qualquer troca: aviso "O resultado anterior não vale mais" e ação "Ler documentos de novo".
- **Rationale**: PUT/DELETE de documento valem em qualquer estado exceto submetido e tornam a análise não vigente sem mudar o estado (decisão da RC-09); POST só em rascunho (RC-08); a leitura fica `DESATUALIZADA` (RC-10) e a conferência só consulta (RC-11).
- **Alternatives considered**: oferecer anexar (o backend recusaria com 409). A limitação fica registrada para a RC-16, sem proposta de mudança.

## D11. Comparação entre resultados

- **Decision**: no resultado, a seção "Em relação à verificação anterior" (resolvidas, continuam, novas) aparece só quando o backend fornecer a análise anterior; antes do novo resultado, nada é marcado como resolvido.
- **Rationale**: RC-16 exige comparação e proíbe marcar resolvida pelo simples upload; o histórico é da RC-13 (`GET …/analises`, "resultados e histórico").
- **Alternatives considered**: comparar na tela a partir de cópia local (dado que o backend não garante).

## D12. Pendência: informações e ações

- **Decision**: mostrar critério, versão do protocolo, explicação, responsável e, quando o backend fornecer, motivo e origem (documento, página, trecho). Ações: "Abrir documento" (download, RC-08) para conferir o original; "Já tenho uma nova versão" (substituir) quando a responsável é a unidade emissora; "Substituir arquivo" para ilegível/não lido; "Conferir leitura" para transcrição não conferida ou divergência.
- **Rationale**: `contratos-dados.md` garante `codigoRegra`, `mensagem` e `responsavelCorrecao`; motivo e evidências existem no resultado interno da RC-12 e a RC-15 pede o trecho "quando disponível". As ações são operações existentes.
- **Alternatives considered**: botão "Enviar à unidade" (operação inexistente; proibida pelo brief e pela spec).

## D13. Estados de envio de arquivo

- **Decision**: "Enviando…" (durante a tentativa), "Enviado" (após 201/200), "Não enviado" (falha ou recusa). O componente F4 "Aguardando envio" não é usado.
- **Rationale**: o envio é síncrono, um arquivo por vez, e o backend só guarda documentos `ENVIADO` (RC-08). Não existe fila.
- **Alternatives considered**: manter "Aguardando" para arquivos escolhidos e não enviados (estado que não existe no backend).

## D14. Sensores no anexo por foto

- **Decision**: "Fotografar" passa por E4.1 (Condições da captura) e segue para a câmera; "Escolher arquivo" não passa. Classificação e orientação exibidas são as fornecidas pelo backend; se a RC-22 não as fornecer, a tela mostra valor e idade da leitura.
- **Rationale**: RC-22 ("orientações sobre iluminação e distância ao capturar documentos"; indisponibilidade não bloqueia upload). Decisão de tela reversível.
- **Alternatives considered**: tela própria no pedido (desconectada do momento da captura).

## D15. Organização no pen.dev

- **Decision**: arquivo único `design/RegulaCheck.pen`; uma faixa horizontal por história (RC-36 a RC-39) abaixo dos quadros F1–F4; quadros 390 × 844 nomeados `<ID> <Tela> — <Estado>`; anotações de operação e de "contrato provisório" como nós `note` ao lado do quadro, fora da tela.
- **Rationale**: `design/README.md` adotou o arquivo único em 13/11; nós `note` existem no esquema `.pen` e não poluem a tela.
- **Alternatives considered**: cinco arquivos `.pen` do plano histórico do 007 (duplicaria o design system por importação).

## D16. Ajustes no design system antes de desenhar

- **Decision**: completar o catálogo v1.0 com as peças que o fluxo exige e que não existem: ação "Sair" no `Cabeçalho/Marca`; data em `Pedido/Linha`; nome do arquivo e "Mais opções" em `Documento/Linha`; `Folha/Escolha`; `Campo/Senha`; `Conferência/*` (campo com decisões); `Pedido/Próximo passo`; `Linha/Navegação`; `Critério/Linha`; `Comparação/*`; `Dado/Linha`; `Aviso/Resultado não vigente`. Ver lista completa em `contracts/inventario-quadros.md`.
- **Rationale**: FR-006 exige montar só com componentes v1.0; sem essas peças, os quadros criariam elementos soltos.
- **Alternatives considered**: desenhar soltos e componentizar depois (gera divergência entre quadros).

## D17. Numeração da feature

- **Decision**: `014-fluxo-telas-app`, próximo número livre em `origin/main`. Se a RC-13 publicar outra `014`, renumerar esta sem mudar conteúdo.
- **Rationale**: numeração sequencial do Spec Kit (`init-options.json`).
- **Alternatives considered**: número reservado fora da sequência (foge da convenção).

## D18. Dados de exemplo: estrutura do backend, texto exemplar (13/11/2026)

- **Decision**: a **estrutura** dos exemplos segue o backend — numeração e estados dos Pedidos 1 a 4 da carga `database/scripts/02_carga_sintetica.sql`, regras pendentes, quantidade e tipo dos documentos, situações dos campos nos catálogos da leitura simulada, referência `SIM-2026-0001`; estados ausentes da carga usam os Pedidos 5 a 8. Os **valores exibidos** são exemplares realistas e fictícios, em português correto: paciente "Ana Beatriz Moreira", localidade "Santa Aurora (município fictício)", nomes de arquivo plausíveis, trechos e resumos redigidos. Mapa em `data-model.md` §5.
- **Rationale**: o `/speckit-analyze` apontou que a numeração por estado não existia no backend; a primeira correção copiou os literais da carga e dos catálogos ("Paciente A (fictícia)", "Maria Exemplo da Silva", "texto sintetico" sem acento, "[SIMULADO]…"), o que o responsável recusou por quebrar a imersão na avaliação das telas. Valores exemplares não violam FR-002: ele trata da origem do campo no app, não do conteúdo ilustrativo do quadro.
- **Alternatives considered**: literais do backend (rejeitado pelo responsável); dados livres sem relação com a carga (perde a coerência entre estados, regras e documentos que a demonstração vai mostrar).
- **Evidência adicional para a RC-16**: a carga registra no Pedido 1 um documento de identificação enviado na revisão 2, depois de uma análise — exatamente o caminho que a API atual recusa (anexar fora de rascunho). Reforça a limitação registrada em D10.

## D19. Verificação em andamento só no pedido (correção do analyze)

- **Decision**: o estado "Em verificação" aparece em E1.3; E3.3 não tem quadro de processamento. O quadro liberado vira E3.2 "Texto do documento como dado" (cobre FR-009 com o caso de injeção do catálogo de testes).
- **Rationale**: o contrato leva a resposta 202 da verificação para E1.3 e nenhuma navegação abre E3.3 durante o processamento.
- **Alternatives considered**: manter os dois quadros (duplicação sem caminho de navegação).

Nenhum item da Technical Context ficou como NEEDS CLARIFICATION.
