# RegulaCheck — instruções para o Codex

Leia `CLAUDE.md` e `.specify/memory/constitution.md` antes de planejar ou alterar este projeto. Preserve as restrições de tecnologia, os limites do demonstrador e as alterações de outras pessoas.

## Spec Kit como processo padrão

- Use as skills oficiais `speckit-*` instaladas para o Codex neste repositório. Não diga que um procedimento manual foi executado pelo Spec Kit.
- Para trabalho aprovado, reutilize a especificação aplicável e siga specify → clarify quando necessário → plan → tasks → analyze → implement → verificação. Mantenha rastreabilidade entre pedido, requisitos, tarefas, arquivos e evidências reais.
- Não gere uma especificação nova por arquivo ou pequena alteração. Não marque desenho, teste, medição ou aprovação como concluídos sem evidência.
- Não crie commits, push, issues ou publicação automaticamente. A branch local de exploração é `codex/007-design-system-exploracao`.

## Fase atual: identidade aprovada e aplicação inicial

Em 13/11/2026, após a exploração, o responsável aprovou seguir com a identidade dos três exemplos inspirados no Headspace e pediu adicioná-los ao pen.dev, com resumo para outro agente. A direção visual está aprovada; isso não conclui o design system completo, os 73 estados ou a validação Android. Retomar por `design/README.md` e reutilizar `specs/007-epico-telas-pendev/` para rastreabilidade. Não criar outra feature por causa dessa transposição.

- A fonte nativa atual é `design/RegulaCheck.pen`; o resumo de continuidade é `design/README.md`. Textos e inventário anteriores em `mobile/design/` seguem sujeitos à reconciliação com cada tela.
- Usar a paleta da marca: `#112B3C`, `#205375`, `#F6680E` e `#EFEFEF`, com branco e tons de apoio registrados. Direção aprovada: Bricolage Grotesque em títulos, Nunito Sans em corpo/ações, minimalismo simpático e poucas ilustrações. Componentes `Atual/*` e três telas são a aplicação inicial; o catálogo anterior continua em revisão.
- Arquivos `.pen` são lidos e editados pelas ferramentas do pen.dev. Não ler, procurar conteúdo nem editar esses arquivos como texto.
- Estudar fluxos reais e referências identificadas, separar evidência de hipótese e preservar dados sintéticos. Skills e MCPs ajudam a pesquisa e a revisão; não substituem a validação de usabilidade.
- O repertório exploratório e os cuidados para desenho/implementação Android estão em `design/referencias.md`. Consultar esse registro ao confeccionar as telas Android; a skill comunitária Android é orientação auxiliar, sem impor mudanças de escopo, tema escuro ou cores dinâmicas.
- Manter pesquisas e referências em texto em `design/referencias.md`. Não adicionar painéis, sites, capturas de referências ou notas de pesquisa ao canvas do pen.dev sem novo pedido: o responsável considera isso poluição visual.
- Não copiar a marca ou fonte proprietária do Headspace; usar a referência como orientação de tom, respiro e clareza. Sem ilustrações decorativas nas telas funcionais atuais.
