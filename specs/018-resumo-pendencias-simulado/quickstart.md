# Verificação: RC-14

1. Preparar Oracle com `01_criar_esquema.sql` ou migrações até `10_analise_historico.sql`; carregar somente dados sintéticos.
2. Autenticar paciente, ler e conferir documentos, pedir análise conforme [RC-13](../014-persistir-analises-historico/quickstart.md).
3. `POST /solicitacoes/{id}/resumo`: conferir simulação, códigos, ações e pendências preservadas. Repetir e conferir resposta existente.
4. Simular falha/saída inválida do adaptador em teste: conferir `PADRAO`, nova tentativa e rejeição de código desconhecido.
5. Alterar revisão ou conferência durante geração: nenhuma publicação obsoleta. Tentar pedido alheio: mesma resposta 404 de pedido inexistente.
