# Data Model: Inventário documental

Não há persistência. O "modelo" é o registro do inventário (`inventario.md`, criado na implementação), uma tabela com uma linha por documento.

## Documento (linha do inventário)

| Campo | Descrição | Valores |
|---|---|---|
| Caminho | Relativo à raiz do repositório | texto único |
| Tipo | Natureza | referência, guia, contrato, decisão, evidência, índice, instrução de agente, SQL, gabarito |
| Público | Quem lê | grupo, avaliador, agente, paciente (textos de tela) |
| Temas | Temas que define ou repete | lista de IDs de Tema |
| Fonte de verdade | Sim/não, ou caminho da fonte | caminho ou "própria" |
| Protegido | Não pode ser reescrito (evidência, Spec Kit, binário) | sim/não |
| Destino | Ação | manter, normalizar, consolidar-em:`caminho`, remover |
| Estado | Progresso | pendente, feito, verificado |

## Tema

| Campo | Descrição |
|---|---|
| ID | Identificador curto (ex.: `T-limites-upload`) |
| Descrição | Assunto único |
| Fonte de verdade | Documento ou código que o define |
| Ocorrências | Documentos que o citam (devem ser referências) |

## Regras e relações

- Cada Tema tem exatamente uma fonte de verdade; cada Documento pode definir vários Temas e referenciar outros.
- Destino `remover` exige: nenhum Tema exclusivo, não exigido por regra, nenhuma referência pendente.
- Documento Protegido só admite destino `manter` ou `normalizar` (links/formatação).
- Transições de Estado: pendente → feito (alteração aplicada) → verificado (links e convenções conferidos).
