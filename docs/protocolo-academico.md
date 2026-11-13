# Protocolo acadêmico fictício (RC-01)

> **Documento acadêmico fictício.** O protocolo abaixo foi criado só para o demonstrador do RegulaCheck. **Não é exigência oficial** de nenhum serviço de saúde ou do SUS. Todos os dados usados são sintéticos.

Rastreabilidade: RC-01 ([ClickBus#6](https://github.com/Code-Masters-F/ClickBus/issues/6)) · especificação em [`specs/001-recorte-demonstrador/`](../specs/001-recorte-demonstrador/spec.md).

Seções: [Recorte](#recorte) · [Limites](#limites-do-recorte) · [Catálogo de regras](#catálogo-de-regras) · [Resultados por regra](#resultados-por-regra) · [Resultado geral](#resultado-geral) · [Casos de verificação](#casos-sintéticos-de-verificação) · [Documentos como dados](#tratamento-de-documentos) · [Aplicação pelo motor](#aplicação-pelo-motor-rc-12) · [Verificação](#verificação)

## Recorte

| Item | Valor | Situação |
|---|---|---|
| Localidade demonstrativa | Município Exemplo (fictício, sem vínculo com rede de saúde real) | Adotado (decisão do responsável, 12/11/2026) |
| Especialidade | Consulta ambulatorial em cardiologia (apenas contexto documental; as regras não avaliam conteúdo clínico) | Adotado (decisão do responsável, 12/11/2026) |
| Protocolo | `PROTOCOLO-ACADEMICO-FICTICIO v0.1` | Adotado (decisão do responsável, 12/11/2026) |
| Versionamento | `vMAJOR.MINOR`; qualquer mudança de regra gera nova versão e cada análise guarda a versão usada | Adotado |
| Horário de referência | Salvador | Adotado |

## Limites do recorte
- Somente dados sintéticos e protocolo fictício, identificados como tal na interface e na documentação.
- O sistema não diagnostica, não define urgência, não autoriza atendimento e não coloca ninguém em fila real do SUS.
- Não há promessa de redução real de filas.
- O resultado "sem pendências identificadas" **não** significa aprovação do SUS.
- Informação clínica incorreta ou ausente exige revisão pela unidade emissora; a paciente não altera o documento original.

## Catálogo de regras

Até 6 regras. Cada uma tem código, natureza, descrição, evidência, condição de aplicação, mensagem à paciente e **um único** responsável pela correção. As mensagens não diagnosticam, não prescrevem, não definem urgência nem prometem vaga ou aprovação. Regras além destas 6 ficam fora do MVP (melhoria P1).

| Código | Natureza | Descrição | Evidência exigida | Condição de aplicação | Mensagem à paciente | Responsável |
|---|---|---|---|---|---|---|
| RC-R01 | Obrigatória | Encaminhamento presente | Documento de encaminhamento para a especialidade | Sempre | "Não encontramos o encaminhamento. Envie o documento emitido pela sua unidade." | Paciente |
| RC-R02 | Obrigatória | Identificação coerente entre documentos | Nome e data de nascimento iguais nos documentos enviados | Sempre | "Nome ou data de nascimento diferem entre os documentos. Confira os dados." | Paciente |
| RC-R03 | Obrigatória | Documento de identificação | Documento de identificação ou cartão do SUS (sintéticos) legível | Sempre | "Envie um documento de identificação ou o cartão do SUS." | Paciente |
| RC-R04 | Obrigatória | Motivo do encaminhamento informado | Campo de motivo/justificativa preenchido no encaminhamento | Sempre | "O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento." | Unidade emissora |
| RC-R05 | Obrigatória | Encaminhamento dentro da validade | Data de emissão com no máximo 180 dias de antiguidade (validade demonstrativa) | Sempre | "A data do encaminhamento está fora da validade deste protocolo de teste. Peça um novo à unidade emissora." | Unidade emissora |
| RC-R06 | Condicional | Autorização do responsável legal | Autorização assinada do responsável | Paciente com menos de 18 anos na data do envio | "Para pacientes menores de 18 anos, envie a autorização do responsável." | Paciente |

Detalhes:
- **RC-R02:** se o erro estiver no próprio documento, a mensagem orienta a paciente a procurar a unidade emissora, mas o responsável registrado continua sendo um só (a paciente). A comparação exige dois ou mais documentos legíveis com identificação; sem isso, o resultado é "não verificável".
- **RC-R05:** os 180 dias contam da data de emissão até a data do envio. O valor é demonstrativo e não vem de norma oficial.
- **RC-R06:** a data de nascimento usada para decidir se a regra se aplica vem do documento de identificação (RC-R03). Se essa data estiver ilegível ou ausente por ilegibilidade, a aplicabilidade é indeterminada e o resultado é `NAO_VERIFICAVEL`. Para paciente adulta, o resultado é `NAO_APLICAVEL` e não gera pendência.

### Mensagem padrão para "não verificável"
"Não conseguimos ler esta informação no documento enviado. Isso não significa que ela esteja faltando. Envie uma foto ou arquivo mais nítido ou confira a transcrição." Responsável: paciente.

## Resultados por regra

Quatro resultados possíveis: `ATENDE`, `PENDENTE`, `NAO_VERIFICAVEL`, `NAO_APLICAVEL`. **Não encontrar um campo não comprova sua ausência**: só se declara a ausência quando o documento está legível.

| Aplicável? | Evidência | Resultado |
|---|---|---|
| Sim | encontrada e conforme | `ATENDE` |
| Sim | não localizada em documento legível, ou divergente | `PENDENTE` |
| Sim | ilegível, protegido ou leitura vazia | `NAO_VERIFICAVEL` |
| Não | qualquer | `NAO_APLICAVEL` |
| Indeterminada (dado da condição ilegível) | qualquer | `NAO_VERIFICAVEL` |

Documento ilegível **nunca** equivale a requisito ausente.

## Resultado geral

Todas as regras aplicáveis são avaliadas; uma pendência não interrompe a conferência das demais. Precedência:

1. Se alguma regra é `PENDENTE`: `COM_PENDENCIAS`.
2. Senão, se alguma é `NAO_VERIFICAVEL`: `REQUER_CONFERENCIA`.
3. Senão: `SEM_PENDENCIAS_IDENTIFICADAS`.

`NAO_APLICAVEL` não influencia o resultado. `FALHA_ANALISE` é uma falha de processamento, fora deste catálogo.

**Decisão de nomenclatura:** `REQUER_CONFERENCIA`, já previsto em [arquitetura.md](arquitetura.md), passa a cobrir tanto a conferência da transcrição pela paciente quanto o caso de haver regra não verificável. Em ambos, a paciente precisa agir (conferir ou reenviar) antes de qualquer conclusão.

## Casos sintéticos de verificação

Todos fictícios. Resultado esperado por regra (R01 a R06) e geral:

| # | Situação | R01 | R02 | R03 | R04 | R05 | R06 | Geral |
|---|---|---|---|---|---|---|---|---|
| 1 | Adulta, todos os documentos legíveis e conformes | ATENDE | ATENDE | ATENDE | ATENDE | ATENDE | NAO_APLICAVEL | `SEM_PENDENCIAS_IDENTIFICADAS` |
| 2 | Adulta, encaminhamento legível sem o campo de motivo | ATENDE | ATENDE | ATENDE | PENDENTE | ATENDE | NAO_APLICAVEL | `COM_PENDENCIAS` |
| 3 | Documento de identificação ilegível, demais conformes | ATENDE | NAO_VERIFICAVEL | NAO_VERIFICAVEL | ATENDE | ATENDE | NAO_VERIFICAVEL | `REQUER_CONFERENCIA` |
| 4 | Paciente de 15 anos, sem autorização do responsável | ATENDE | ATENDE | ATENDE | ATENDE | ATENDE | PENDENTE | `COM_PENDENCIAS` |
| 5 | Adulta, nomes diferentes entre os documentos | ATENDE | PENDENTE | ATENDE | ATENDE | ATENDE | NAO_APLICAVEL | `COM_PENDENCIAS` |

No caso 3, R02 e R06 ficam não verificáveis porque dependem do documento ilegível (nome, data de nascimento e idade).

## Tratamento de documentos
Conteúdo de documentos é dado não confiável. Instruções encontradas em um documento (por exemplo, "ignore as regras") não alteram critérios, perfil ou estado da solicitação.

## Aplicação pelo motor (RC-12)

Como o motor de regras (`backend/.../validacao`) aplica esta versão. As regras acima não mudam; isto é a interpretação das evidências. Detalhes e alternativas em [specs/013-executar-regras-protocolo/research.md](../specs/013-executar-regras-protocolo/research.md).

- **Evidência:** só vale o campo **identificado na leitura e confirmado pela paciente** (RC-11), com valor no formato esperado (datas `AAAA-MM-DD`, tipo pelos nomes dos tipos documentais, autorização `SIM`/`NAO`). Ilegível, não conferido, com divergência apontada, valor fora do formato e documento não lido (protegido ou leitura vazia) tornam **não verificável** a regra que depende deles. Campo não localizado em documento lido, sem contestação, conta como ausente.
- **Tipo do documento:** o lido e confirmado; o tipo escolhido no envio não é prova.
- **Data de referência ("data do envio"):** a data, em Salvador, do envio mais recente entre os documentos avaliados.
- **R01/R03:** documento de tipo não verificável pode ser o que falta, então impede "pendente" (fica não verificável). R03 exige identificação com nome e data de nascimento confirmados.
- **R02:** nomes comparados sem diferenciar acentos, maiúsculas e espaços extras; divergência entre valores confirmados é pendente mesmo que outro campo esteja ilegível; menos de dois documentos com nome e data confirmados é não verificável.
- **R04/R05:** consideram todos os encaminhamentos confirmados (basta um sem motivo ou fora da validade para ficar pendente). 180 dias atende, 181 não; emissão posterior à data de referência é pendente. Sem encaminhamento confirmado, ficam **não verificáveis** (nunca pendentes: a falta do encaminhamento é cobrada pela R01).
- **R06:** a idade vem só da data de nascimento do documento de identificação; ausente, não verificável, posterior à data de referência ou diferente entre identificações → condição indeterminada (não verificável). Quem completa 18 anos na data de referência é adulta.
- **Pendências:** pendente usa a mensagem e o responsável da regra; não verificável usa a mensagem padrão acima, com um motivo de lista fechada (campo ilegível, documento não lido, transcrição não conferida, divergência apontada, valor não interpretável, condição indeterminada, documentos insuficientes, depende de documento ausente) e a paciente como responsável.

## Verificação

| Verificação | Situação |
|---|---|
| Completude dos atributos das regras (SC-001) | Conferida na redação deste documento: 6 regras com os 7 atributos |
| Revisão cruzada com CLAUDE.md, constituição e decisões (SC-004) | Ver relatório da implementação da RC-01 |
| Aplicação em papel dos 5 casos por duas pessoas (SC-002) | **Dispensada** por decisão do responsável em 12/11/2026; não executada, portanto não verificada. Os casos continuam servindo de base para os testes futuros |
| Aplicação automática dos 5 casos pelo motor (RC-12) | Verificada por teste em 13/11/2026 (`CasosDoProtocoloTest`: 30 resultados e 5 gerais iguais à tabela de casos); gabarito escrito à mão em [specs/013-executar-regras-protocolo/quickstart.md](../specs/013-executar-regras-protocolo/quickstart.md) |
| Confirmação de localidade, especialidade, regras e 180 dias | Dispensada a validação pelo grupo; valores adotados por decisão do responsável em 12/11/2026 |
