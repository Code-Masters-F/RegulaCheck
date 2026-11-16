# RegulaCheck — instruções para o Codex

Leia `CLAUDE.md` e `.specify/memory/constitution.md` antes de planejar ou alterar este projeto. Preserve as restrições de tecnologia, os limites do demonstrador e as alterações de outras pessoas.

## Spec Kit como processo padrão

- Use as skills oficiais `speckit-*` instaladas para o Codex neste repositório. Não diga que um procedimento manual foi executado pelo Spec Kit.
- Para trabalho aprovado, reutilize a especificação aplicável e siga specify → clarify quando necessário → plan → tasks → analyze → implement → verificação. Mantenha rastreabilidade entre pedido, requisitos, tarefas, arquivos e evidências reais.
- Não gere uma especificação nova por arquivo ou pequena alteração. Não marque desenho, teste, medição ou aprovação como concluídos sem evidência.
- Não crie commits, push, issues ou publicação automaticamente. O trabalho de design foi feito na branch `codex/007-design-system-exploracao` (PR #66).

## Fase atual: design system v1.0 e fluxo de telas desenhado

Em 13/11/2026 o responsável aprovou a identidade dos três exemplos inspirados no Headspace, que virou o design system v1.0. Em 14/11/2026 a feature `specs/015-fluxo-telas-app/` (criada como 014 e renumerada em 15/11/2026, quando a RC-13 publicou outra 014) desenhou os 94 quadros do fluxo, acompanhando o backend. Isso não conclui a validação com uma pessoa que não desenhou (T060), a aprovação do grupo nem a validação Android. Retomar por `design/README.md`; a rastreabilidade do fluxo de telas está em `specs/015-fluxo-telas-app/`, e a do épico em `specs/007-epico-telas-pendev/`.

- A fonte nativa atual é `design/RegulaCheck.pen`; o resumo de continuidade é `design/README.md`. Inventário, textos e dados das telas estão em `mobile/design/` e seguem `specs/015-fluxo-telas-app/`; alterou o desenho, altere esses arquivos.
- Usar a paleta da marca: `#112B3C`, `#205375`, `#F6680E` e `#EFEFEF`, com branco e tons de apoio registrados. Direção aprovada: Bricolage Grotesque em títulos, Nunito Sans em corpo/ações, minimalismo simpático e poucas ilustrações. As telas usam só os componentes v1.0; os componentes `Legado/*` não devem ser usados.
- Arquivos `.pen` são lidos e editados pelas ferramentas do pen.dev. Não ler, procurar conteúdo nem editar esses arquivos como texto.
- Estudar fluxos reais e referências identificadas, separar evidência de hipótese e preservar dados sintéticos. Skills e MCPs ajudam a pesquisa e a revisão; não substituem a validação de usabilidade.
- Os cuidados para desenho e implementação Android estão em `mobile/design/design-system.md` (Comportamentos) e `mobile/design/acessibilidade.md`. Consultá-los ao confeccionar as telas Android; a skill comunitária Android é orientação auxiliar, sem impor mudanças de escopo, tema escuro ou cores dinâmicas.
- Não adicionar painéis, sites, capturas de referências ou notas de pesquisa ao canvas do pen.dev sem novo pedido: o responsável considera isso poluição visual. A pesquisa de referências e as capturas saíram do repositório em 14/11/2026.
- Não copiar a marca ou fonte proprietária do Headspace; usar a referência como orientação de tom, respiro e clareza. Sem ilustrações decorativas nas telas funcionais atuais.
