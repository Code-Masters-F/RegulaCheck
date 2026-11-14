# Repertório de identidade e UX

Histórico da pesquisa em 13/11/2026. **Estado atual:** o responsável aprovou seguir com a identidade inspirada no Headspace e sua transposição ao pen.dev; ponto de retomada em `design/README.md`. As alternativas abaixo são histórico, não direções igualmente vigentes. Este registro não substitui a especificação do épico 007.

Por solicitação do responsável, o painel de referências foi removido de `design/RegulaCheck.pen` em 13/11/2026 para manter o canvas limpo. O repertório permanece somente neste registro textual, para contexto de trabalho. As fontes são exemplos reais; as adaptações abaixo são hipóteses para discussão, sem validação com pacientes.

## As seis referências

| Referência | O que foi observado | Hipótese para o RegulaCheck | Limite |
|---|---|---|---|
| 1. [Headspace](https://www.headspace.com/) + tela Explore via Refcat | Títulos expressivos, fundo claro, ilustrações com várias cores e continuidade entre atividades. O site complementa o estudo de marca. | Azul profundo para orientação; laranja pontual para acolhimento; um gesto gráfico autoral. Cor pode aparecer além da dupla principal. | Não importar navegação iOS, gamificação, cursos ou linguagem promocional. |
| 2. [Firefox](https://www.firefox.com/en-US/) | Tipografia com caráter, contraste forte e identidade reconhecível além dos componentes. | Explorar uma família de títulos com personalidade e corpo de texto muito legível. | Não copiar o roxo, mascote ou grandes blocos de campanha. É referência de marca de um site, não de fluxo Android. |
| 3. [GOV.UK — File upload](https://design-system.service.gov.uk/components/file-upload/) | Rótulo explícito e seletor nativo; orientação para solicitar apenas arquivos necessários. | Informar documento esperado e limites antes de selecionar. Depois, nome, prévia, estado e substituição. | O seletor Android e a captura por câmera precisam de desenho próprio; a referência web não define sua aparência. |
| 4. [NHS — Complete multiple tasks](https://service-manual.nhs.uk/design-system/patterns/complete-multiple-tasks) | Tarefas agrupadas por assunto e estados textuais distinguem concluído e incompleto. | “Continuar pedido” levando ao próximo passo; preservar rascunho e indicar o que falta. | Só usar lista de tarefas se a complexidade justificar. Não alongar um fluxo simples. |
| 5. [GOV.UK — Error summary](https://design-system.service.gov.uk/components/error-summary/) | Resumo nomeia problemas e oferece links para o local da correção, com texto além da cor. | Cada pendência explica observação, documento/página e ação possível. | Erro de envio, transcrição incerta e pendência clínica são situações distintas. Evitar culpa atribuída à paciente. |
| 6. [NHS — Check answers](https://service-manual.nhs.uk/design-system/patterns/check-answers) | Dados agrupados com ações próximas e explicação do destino antes de enviar. | Conferir transcrição com origem visível e indicar quem resolve cada problema. | A separação de responsáveis é uma adaptação do RegulaCheck: paciente confere transcrição; unidade emissora revisa conteúdo clínico. |

Links dos exemplos funcionais consultados (não mantidos no canvas):

- Upload: https://design-system.service.gov.uk/components/file-upload/default/
- Retomada: https://service-manual.nhs.uk/design-example/patterns/complete-multiple-tasks/example
- Erros: https://design-system.service.gov.uk/components/error-summary/default/
- Conferência: https://service-manual.nhs.uk/design-example/patterns/check-answers/example

### Proveniência da imagem do Refcat

- Aplicativo: Headspace; fluxo: Explore; plataforma: iOS.
- Captura registrada pelo catálogo: 08/09/2025, 10:06:46.470 UTC; consulta: 13/11/2026.
- `screenDocId`: `28986bf8-81b2-4af0-84df-b5654a8c98f9__f2c7edab-00b5-460c-9663-1cf64517f7db__c1f4c734-34a2-45d4-ac1c-23557c57b4c0__1cba404f-413c-4a8b-be36-d791e9ca4fed`.
- Imagem retornada pela ferramenta `get_screen`, acesso gratuito: `referencias/headspace-explore-refcat.webp`. É referência de terceiro para estudo; não é arte do RegulaCheck nem ativo para incorporar ao aplicativo.
- Foram executadas consultas de catálogo e leitura de tela. Não foram chamadas geração de telas ou exportação de design system; nenhuma cota de geração ou exportação foi usada.

## Pesquisa Refcat para novos exemplos — 13/11/2026

Pedido: remover o repertório do canvas e gerar novas propostas visuais respeitando a paleta da marca. Cores-base lidas nas variáveis do `.pen`: `#112B3C`, `#205375`, `#F6680E` e `#EFEFEF`; branco e os tons de apoio já documentados completam o sistema. Isso não constitui aprovação da composição, tipografia ou componentes.

Foram consultados `list_facets` e `search_screens` para Headspace, documentos/upload, tarefas, Monzo, Todoist e Dropbox, priorizando Android e iOS. As buscas amplas de upload retornaram telas bloqueadas no plano gratuito; elas não foram abertas nem usadas como evidência visual. O resultado gratuito de calendário do Todoist não foi escolhido porque calendário não é a tarefa central desta tela. Não foi solicitada assinatura ou geração no Refcat.

### Evidências visuais efetivamente abertas

| Aplicativo / fluxo | Plataforma e captura do catálogo | Observação direta | Aplicação proposta |
|---|---|---|---|
| Headspace / Explore | iOS · 08/09/2025 | Títulos com peso, amplo respiro, cores concentradas em ilustrações e continuidade visual entre atividades. | Tipografia humana e hierarquia clara; não copiar cursos, mascotes ou gamificação. Reutilizada a captura gratuita já obtida no Refcat. |
| Monzo / Creating category targets | iOS · 03/08/2026 | Uma área prioritária arredondada, texto com contraste de tamanho e ação explícita abaixo da informação que a justifica. A imagem contém um gráfico financeiro. | Diferenciar o pedido que exige ação; não trazer gráfico, saldo, métricas ou navegação iOS para o produto. |
| Dropbox / File options | Android · 10/09/2024 | A prévia domina a tela, com nome do arquivo no topo e ações diretamente relacionadas a ele nas áreas inferior e superior. A captura é uma foto aberta; o nome do fluxo no catálogo não corresponde a um menu de opções aberto. | Preservar a relação entre documento e ação; não reutilizar a foto nem presumir um fluxo de upload a partir desta captura. |

Proveniência adicional:

- Monzo: `f6915aea-cb42-474d-99f2-066d3177949a__ccc5ff24-f952-4059-8421-67eaa921f7b0__80dc847f-5247-41a2-8d45-8605b5315a70__8e2b7c06-14e9-41cd-8114-2a5670c66485`; arquivo `referencias/monzo-category-targets-refcat.webp`.
- Dropbox: `16003c93-3f21-46b1-999c-1bc5bc2ff9b6__c0d12357-09b2-4c40-8059-d0311499500f__19d05f28-3aa5-40b3-9883-8a454970fa56__2f52c07a-a3b9-4b92-9a64-908761f3bba8`; arquivo `referencias/dropbox-file-options-android-refcat.webp`.
- Headspace Android / Starting a breathing exercise: a consulta retornou metadados, mas nenhuma imagem utilizável nessa resposta. Não foi tratada como fonte visual; usamos a captura iOS já disponível.

Essas datas são das capturas do catálogo, não uma afirmação sobre a versão atual dos aplicativos. Consulta em 13/11/2026.

### Recorte dos exemplos gerados

Três variações independentes da tela **Meus pedidos**, com os mesmos dois pedidos sintéticos e a mesma paleta, para comparar hierarquia e identidade: Clareza editorial, Leveza documental e Horizonte azul. Todas priorizam a próxima ação do PED-0004 (com pendências); o PED-0001 permanece rascunho. Os pedidos e datas vêm de `mobile/design/dados-ficticios.md`.

Prompts e referências anexadas: `exploracoes/prompts-refcat-2026-11-13.md`. São imagens conceituais para avaliação; não comprovam interação, medidas exatas, contraste de todos os pixels, fonte ampliada, TalkBack, persistência ou integração com Android. As cores exatas continuam definidas pelos tokens; a renderização por imagem pode aproximá-las.

Ordem efetivamente exibida no chat, preservada para escolhas posteriores:

1. Clareza editorial → `exploracoes/2026-11-13/meus-pedidos-01.png`.
2. Leveza documental → `exploracoes/2026-11-13/meus-pedidos-02.png`.
3. Horizonte azul → `exploracoes/2026-11-13/meus-pedidos-03.png`.

As três imagens foram inspecionadas: conteúdo legível, dois pedidos na ordem prevista, ações associadas aos pedidos e aviso acadêmico presentes, sem recorte aparente. Nenhuma direção foi escolhida pelo responsável até este registro. A biblioteca e a prévia anterior permanecem no `.pen`; os novos exemplos são imagens para discussão no chat, sem inserir novas referências no canvas.

## Seleção para avaliação do responsável — após rejeição das imagens genéricas

Em 13/11/2026, o responsável considerou as três propostas ainda genéricas e pediu referências reais do Refcat para escolher uma direção antes de novos desenhos. Essa avaliação foi recebida: escolheu Headspace / Explore pelo minimalismo simpático. Pediu tipografia semelhante, sem ser idêntica, e uso pontual de ilustrações, adequado ao contexto burocrático.

Seleção curta efetivamente inspecionada:

1. **Headspace / Explore / iOS / 08/09/2025**: acolhimento por tipografia, ritmo e linguagem de ilustração. Captura já registrada em `referencias/headspace-explore-refcat.webp`.
2. **N26 / Landing page / web / 11/10/2026**: direção de marca mais sóbria, grandes campos de cor profunda, composição editorial e contraste entre texto e fotografia. É um site de apresentação, usado para linguagem de marca, não como modelo de fluxo Android. Captura: `referencias/n26-landing-refcat.webp`. ID: `imp-web-n26__unknown__saaslanding-n26__saaslanding-n26-s0`.
3. **Cleo / Set budget limit / iOS / 27/02/2024**: tipografia condensada de destaque, ilustração e representação visual de informação; personalidade mais ousada. A captura exibe uma visão de orçamento, apesar do nome do fluxo. Usar para avaliar o grau de expressividade, sem transportar dados financeiros ou tom irreverente para questões clínicas. Captura: `referencias/cleo-set-budget-refcat.webp`. ID: `38773bf7-217f-4ae9-8e15-196bb30d0160__0faf3a52-7986-4bbf-902f-c21790b286ae__725158cd-5752-436e-a524-1a682fb697fd__a252fcfd-1c89-43d2-a090-7c70d5af3c5c`.

Também foram pesquisados Finch, stoic, Lemonade e Opal. Não indicados nesta seleção: os resultados não forneceram uma referência visual suficientemente pertinente e verificada para este recorte.

O MCP informa nomes, IDs e slugs, mas não devolveu permalinks de navegação utilizáveis. O padrão de URL pública sugerido na documentação não foi confirmado: as páginas de aplicativo testadas retornaram 404, e o sitemap em inglês não listou essas telas. Não inventar links diretos. Para localizar na interface, abrir `https://refcat.app/dashboard/`, pesquisar o nome do aplicativo e procurar o nome do fluxo acima. As capturas locais são evidências retornadas pelo catálogo, não novas propostas geradas. As três fontes selecionadas tiveram captura acessível gratuitamente nesta consulta.

### Interpretação da direção escolhida

Proposta tipográfica para avaliação: Bricolage Grotesque em títulos e Nunito Sans em textos e ações, consultadas no repositório oficial Google Fonts. A semelhança pretendida é de espírito visual; não foi afirmada a identificação da fonte proprietária do Headspace. Manter a paleta da marca, respiro, superfícies claras e arredondadas apenas para agrupamentos úteis, linguagem concreta e uma próxima ação clara. Não preencher telas funcionais com ilustrações.

Registro dos prompts: `exploracoes/headspace-direcao-2026-11-13.md`. Três telas da mesma identidade foram aprovadas como direção e transpostas para camadas nativas; ver `design/README.md`. O catálogo completo e a implementação Android continuam pendentes. Não inserir novas referências no canvas do pen.dev.

## Android: considerar já no desenho, validar na implementação

A skill comunitária [Android Design Guidelines](https://github.com/ehmo/platform-design-skills/tree/main/skills/android), licença MIT, foi instalada em `C:/Users/Eliseu Santos/.codex/skills/android-design-guidelines`. Ela é material de consulta, não um SDK ou aplicativo Android. Sua disponibilidade no catálogo do Codex depende de recarregar a sessão.

Durante o desenho, prever:

- Área de toque de pelo menos 48 dp, espaçamento entre ações e estado de foco.
- Texto ampliado, rótulos extensos e mensagens que não dependam só de cor ou ícone.
- Ordem de leitura e nomes acessíveis para TalkBack; evitar ícones sem rótulo compreensível.
- Espaço para barras do sistema, teclado e rolagem; conteúdo importante e ação principal não podem ficar ocultos.
- Estados de seleção, envio, falha, cancelamento e retomada, inclusive conexão interrompida.

Quando houver implementação Android, conferir seletor de arquivos do sistema, acesso à câmera quando necessário, comportamento de Voltar, restauração de estado/rascunho, semântica do leitor de tela e tamanhos de fonte no aparelho. O desenho não comprova persistência, upload ou acessibilidade implementados.

Referências oficiais: [acessibilidade e alvos de toque](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [áreas do sistema e teclado](https://developer.android.com/develop/ui/compose/layouts/insets).

Não foram instalados SDK, emulador ou dependências de aplicação. Tema escuro e cores dinâmicas da plataforma continuam sujeitos ao escopo e à identidade que forem aprovados; a skill não os torna decisões do produto.

## Refcat: configuração local

- MCP remoto: `https://mcp.refcat.app/mcp`; nome cadastrado no Codex: `refcat`.
- Autenticação: variável local do usuário Windows `REFCAT_API_KEY`, referenciada pela configuração `bearer_token_env_var`.
- A chave foi configurada pelo responsável no próprio computador. Seu valor não está neste projeto.
- Auxiliar: `scripts/configurar-refcat.ps1`, que pede entrada oculta e salva a variável no ambiente do usuário. Não inserir a chave no código, `.pen`, documentação ou conversa.
- Verificado em 13/11/2026: inicialização MCP, listagem de ferramentas, `list_facets`, `search_screens` e `get_screen` autenticados, usando HTTPS com validação nativa do Windows.
- O teste com Python encontrou falha de certificado na cadeia usada por esse cliente; o cliente nativo Windows concluiu as mesmas chamadas com validação TLS ativa.
- Reiniciar o Codex para que o processo receba a variável criada após sua abertura e carregue o novo MCP na sessão.

Documentação: [Refcat MCP](https://refcat.app/en/guides/mcp).
