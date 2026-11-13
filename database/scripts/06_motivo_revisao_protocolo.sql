-- =============================================================================
-- RegulaCheck - 06_motivo_revisao_protocolo.sql  (RC-07)
-- Finalidade : permitir que RC_REVISAO registre o motivo PROTOCOLO_ALTERADO, usado quando a
--              paciente troca o servico (protocolo) de um pedido ainda em RASCUNHO
--              (PUT /solicitacoes/{id}).
-- Ordem      : depois do 01_criar_esquema.sql. Rode SOMENTE em banco criado ANTES da RC-07.
--              Banco criado com o 01_ atual ja tem o motivo novo: nao rode este script.
-- Efeito     : ADITIVO. Recria a restricao RC_REVISAO_CK_MOTIVO com um valor a mais. Nenhuma
--              linha e apagada ou alterada; os motivos antigos continuam validos.
-- Pre-requisito: RC_REVISAO existente. Se a restricao ja aceitar o motivo novo, o DROP e o ADD
--              apenas a recriam com o mesmo conteudo.
-- Se falhar no meio: se o DROP rodou e o ADD falhou, a tabela fica SEM a restricao. Rode o ADD
--              de novo (segunda instrucao) antes de usar a API; ate la o banco aceitaria
--              qualquer motivo.
-- Banco compartilhado: so o prefixo RC_ e tocado.
-- Sem este script num banco antigo: criar, listar e reabrir pedidos funcionam, mas trocar o
--              servico falha ao registrar a revisao (ORA-02290 no log; a API responde erro
--              interno generico).
-- =============================================================================

ALTER TABLE RC_REVISAO DROP CONSTRAINT RC_REVISAO_CK_MOTIVO;

ALTER TABLE RC_REVISAO ADD CONSTRAINT RC_REVISAO_CK_MOTIVO CHECK (MOTIVO IN
    ('CRIACAO', 'DOCUMENTO_ENVIADO', 'DOCUMENTO_SUBSTITUIDO', 'DOCUMENTO_REMOVIDO',
     'CONFERENCIA_ALTERADA', 'PROTOCOLO_ALTERADO'));
