# API: resumo simulado

`POST /solicitacoes/{id}/resumo`, sessão de paciente, sem corpo. 201 ao gerar, 200 se já houver resumo gerado. Resposta: análise identificada, `resumo: { texto, origem, simulado: true, itens: [{ codigoRegra, acao, responsavel }] }` e as pendências determinísticas. Origem `PADRAO` indica falha/saída inválida e permite nova tentativa. 404 id inexistente ou de outra paciente; 409 sem análise vigente ou vigência alterada durante geração; 503 banco indisponível. Nenhum texto de documento entra no gerador.

`GET /solicitacoes/{id}/analises`: acrescenta `resumo` somente quando armazenado. Histórico mantém o resumo associado à análise original. Campos de resultado e pendência da RC-13 não mudam.
