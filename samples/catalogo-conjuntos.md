# Catálogo dos conjuntos sintéticos (RC-23)

> Material **fictício**, sem dado real. Leitura **simulada**. Resultado esperado não é aprovação do SUS. Data de referência da regressão: 2026-11-12. Gabaritos registrados em 2026-11-17, antes da primeira execução. Commit do gabarito: `3fefe2e` (gabaritos dos conjuntos 13 e 14 revistos depois da primeira execução, ver research.md D9).

| # | Conjunto | Situação coberta | Arquivos | Conferência | Resultado geral esperado | Original |
|---|---|---|---|---|---|---|
| 01 | [01-completo](conjuntos/01-completo/gabarito.md) | Caminho feliz: encaminhamento e identificacao completos de adulta; os arquivos oficiais da RC-10. | PDF, PNG | TUDO_CONFIRMADO | SEM_PENDENCIAS_IDENTIFICADAS | - |
| 02 | [02-sem-identificacao](conjuntos/02-sem-identificacao/gabarito.md) | Requisito ausente: so o encaminhamento; falta o documento de identificacao (RC-R03). | PDF | TUDO_CONFIRMADO | COM_PENDENCIAS | - |
| 03 | [03-sem-motivo](conjuntos/03-sem-motivo/gabarito.md) | Encaminhamento nitido sem o campo de motivo: a unidade emissora precisa completar (RC-R04). | PDF, PNG | TUDO_CONFIRMADO | COM_PENDENCIAS | - |
| 04 | [04-varias-pendencias](conjuntos/04-varias-pendencias/gabarito.md) | Varias pendencias de uma vez: sem identificacao, sem motivo e emissao fora dos 180 dias. | PDF | TUDO_CONFIRMADO | COM_PENDENCIAS | - |
| 05 | [05-foto-ilegivel](conjuntos/05-foto-ilegivel/gabarito.md) | Fotografia da identificacao borrada: a data de nascimento fica ilegivel e nada e afirmado. | PDF, PNG | TUDO_CONFIRMADO | REQUER_CONFERENCIA | - |
| 06 | [06-foto-cortada](conjuntos/06-foto-cortada/gabarito.md) | Fotografia da identificacao cortada: a linha da data de nascimento ficou fora da imagem. | PDF, PNG | TUDO_CONFIRMADO | COM_PENDENCIAS | - |
| 07 | [07-outra-pessoa](conjuntos/07-outra-pessoa/gabarito.md) | Documento de outra pessoa: a identificacao enviada tem nome e nascimento diferentes (RC-R02). | PDF, PNG | TUDO_CONFIRMADO | COM_PENDENCIAS | - |
| 08 | [08-menor-com-autorizacao](conjuntos/08-menor-com-autorizacao/gabarito.md) | Regra condicional aplicavel e atendida: paciente menor de 18 anos com autorizacao assinada. | PDF, PNG, PDF | TUDO_CONFIRMADO | SEM_PENDENCIAS_IDENTIFICADAS | - |
| 09 | [09-menor-sem-autorizacao](conjuntos/09-menor-sem-autorizacao/gabarito.md) | Regra condicional aplicavel e pendente: menor de 18 anos sem autorizacao do responsavel. | PDF, PNG | TUDO_CONFIRMADO | COM_PENDENCIAS | - |
| 10 | [10-maioridade-limite](conjuntos/10-maioridade-limite/gabarito.md) | Regra condicional nao aplicavel na fronteira: a paciente completa 18 anos na data de referencia. | PDF, PNG | TUDO_CONFIRMADO | SEM_PENDENCIAS_IDENTIFICADAS | - |
| 11 | [11-instrucao-no-documento](conjuntos/11-instrucao-no-documento/gabarito.md) | Texto no documento tentando dar ordens ao sistema: e so dado do campo de motivo. | PDF, PNG | TUDO_CONFIRMADO | SEM_PENDENCIAS_IDENTIFICADAS | - |
| 12 | [12-formato-invalido](conjuntos/12-formato-invalido/gabarito.md) | Arquivos que a aplicacao deve recusar no envio: formato nao aceito e PDF com mais de 5 paginas. | TXT, PDF | - | recusa no envio | - |
| 13 | [13-corrigido-de-04](conjuntos/13-corrigido-de-04/gabarito.md) | Reenvio parcial do conjunto 04: o encaminhamento e substituido por um completo e valido; a identificacao continua faltando. | PDF | TUDO_CONFIRMADO | COM_PENDENCIAS | 04 |
| 14 | [14-corrigido-de-09](conjuntos/14-corrigido-de-09/gabarito.md) | Reenvio do conjunto 09: o encaminhamento e substituido por uma versao com a autorizacao assinada do responsavel. | PDF | TUDO_CONFIRMADO | SEM_PENDENCIAS_IDENTIFICADAS | 09 |
| 15 | [15-nada-conferido](conjuntos/15-nada-conferido/gabarito.md) | Mesmos arquivos do conjunto 01, sem nenhuma conferencia: nenhum campo vale como evidencia. | PDF, PNG | NADA_CONFERIDO | REQUER_CONFERENCIA | - |
