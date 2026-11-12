# Contrato do catálogo de regras (PROTOCOLO-ACADEMICO-FICTICIO v0.1)

Formato que `docs/protocolo-academico.md` deve seguir; é o insumo de RC-03 (tabelas), RC-12 (motor) e RC-23/RC-24 (dados e testes). Todos os dados são fictícios. Mensagens não diagnosticam, não definem urgência, não autorizam atendimento nem prometem vaga.

## Cabeçalho obrigatório
Localidade: Município Exemplo (fictício) · Especialidade: cardiologia ambulatorial (demonstrativa) · Protocolo: PROTOCOLO-ACADEMICO-FICTICIO v0.1 · Aviso: não é exigência oficial.

## Atributos por regra (todos obrigatórios)
`codigo`, `descricao`, `evidencia_exigida`, `natureza`, `condicao_aplicacao`, `mensagem`, `responsavel_correcao`.

## Catálogo proposto

| Código | Natureza | Descrição | Evidência exigida | Condição de aplicação | Mensagem à paciente | Responsável |
|---|---|---|---|---|---|---|
| RC-R01 | Obrigatória | Encaminhamento presente | Documento de encaminhamento para a especialidade | Sempre | "Não encontramos o encaminhamento. Envie o documento emitido pela sua unidade." | Paciente |
| RC-R02 | Obrigatória | Identificação coerente entre documentos | Nome e data de nascimento iguais nos documentos enviados | Sempre | "Nome ou data de nascimento diferem entre os documentos. Confira os dados." | Paciente |
| RC-R03 | Obrigatória | Documento de identificação | Documento de identificação ou cartão do SUS (sintéticos) legível | Sempre | "Envie um documento de identificação ou o cartão do SUS." | Paciente |
| RC-R04 | Obrigatória | Motivo do encaminhamento informado | Campo de motivo/justificativa preenchido no encaminhamento | Sempre | "O encaminhamento não traz o motivo. Peça à unidade emissora que complete o documento." | Unidade emissora |
| RC-R05 | Obrigatória | Encaminhamento dentro da validade | Data de emissão com no máximo 180 dias (validade demonstrativa) | Sempre | "A data do encaminhamento está fora da validade deste protocolo de teste. Peça um novo à unidade emissora." | Unidade emissora |
| RC-R06 | Condicional | Autorização do responsável legal | Autorização assinada do responsável | Paciente com menos de 18 anos na data do envio; a data de nascimento vem do documento de identificação (RC-R03). Se estiver ilegível, a aplicabilidade é indeterminada e o resultado é `NAO_VERIFICAVEL` | "Para pacientes menores de 18 anos, envie a autorização do responsável." | Paciente |

## Mensagem padrão para "não verificável"
Quando o resultado de qualquer regra for `NAO_VERIFICAVEL`, a mensagem é: "Não conseguimos ler esta informação no documento enviado. Isso não significa que ela esteja faltando. Envie uma foto ou arquivo mais nítido ou confira a transcrição." Responsável: paciente. Regras que dependem de outra unidade continuam podendo indicar a unidade emissora quando o resultado for `PENDENTE`.

## Observações
- RC-R02: se o erro estiver no próprio documento, a mensagem orienta a paciente a procurar a unidade emissora; o responsável registrado é sempre um só (paciente).
- Regras além das 6 ficam fora do MVP (melhoria P1).

## Resultados por regra e geral
Conforme `data-model.md` (tabela de derivação e precedência). A documentação final deve incluir a mesma tabela e 5 casos sintéticos em papel com resultado esperado por regra: (1) tudo conforme; (2) encaminhamento sem motivo; (3) documento de identificação ilegível; (4) paciente menor de idade sem autorização; (5) paciente adulta (R06 não aplicável) com nomes divergentes.
