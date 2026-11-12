# Protocolo acadêmico fictício (RC-01)

> **Documento acadêmico fictício.** O protocolo abaixo foi criado só para o demonstrador do RegulaCheck. **Não é exigência oficial** de nenhum serviço de saúde ou do SUS. Todos os dados usados são sintéticos.

Rastreabilidade: RC-01 ([ClickBus#6](https://github.com/Code-Masters-F/ClickBus/issues/6)) · especificação em [`specs/001-recorte-demonstrador/`](../specs/001-recorte-demonstrador/spec.md).

Seções: [Recorte](#recorte) · [Limites](#limites-do-recorte) · [Catálogo de regras](#catálogo-de-regras) · [Resultados por regra](#resultados-por-regra) · [Resultado geral](#resultado-geral) · [Casos de verificação](#casos-sintéticos-de-verificação) · [Documentos como dados](#tratamento-de-documentos) · [Verificação](#verificação)

## Recorte

| Item | Valor | Situação |
|---|---|---|
| Localidade demonstrativa | Município Exemplo (fictício, sem vínculo com rede de saúde real) | Proposto, a confirmar pelo grupo |
| Especialidade | Consulta ambulatorial em cardiologia (apenas contexto documental; as regras não avaliam conteúdo clínico) | Proposto, a confirmar pelo grupo |
| Protocolo | `PROTOCOLO-ACADEMICO-FICTICIO v0.1` | Proposto, a confirmar pelo grupo |
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

## Verificação

| Verificação | Situação |
|---|---|
| Completude dos atributos das regras (SC-001) | Conferida na redação deste documento: 6 regras com os 7 atributos |
| Revisão cruzada com CLAUDE.md, constituição e decisões (SC-004) | Ver relatório da implementação da RC-01 |
| Aplicação em papel dos 5 casos por duas pessoas (SC-002) | **Pendente** (exige duas pessoas do grupo) |
| Confirmação de localidade, especialidade, regras e 180 dias pelo grupo | **Pendente** (valores marcados como "proposto") |
