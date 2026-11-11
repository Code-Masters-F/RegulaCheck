## Contrato mínimo de API proposto

As rotas são uma proposta para manter um domínio só e orientar a implementação; ajustar nomes sem perder comportamentos.

| Método | Recurso                                               | Acesso e finalidade                        |
| ------ | ----------------------------------------------------- | ------------------------------------------ |
| POST   | /auth/login                                           | Autenticar conta demonstrativa             |
| GET    | /protocolos                                           | Paciente autenticada consulta opção ativa  |
| GET    | /solicitacoes                                         | Lista somente pedidos próprios             |
| POST   | /solicitacoes                                         | Cria rascunho                              |
| GET    | /solicitacoes/{id}                                    | Detalhes próprios                          |
| PUT    | /solicitacoes/{id}                                    | Atualiza rascunho permitido                |
| DELETE | /solicitacoes/{id}                                    | Exclui rascunho permitido                  |
| POST   | /solicitacoes/{id}/documentos                         | Upload multipart autenticado               |
| GET    | /solicitacoes/{id}/documentos/{docId}                 | Download protegido                         |
| DELETE | /solicitacoes/{id}/documentos/{docId}                 | Remove documento antes da submissão        |
| POST   | /solicitacoes/{id}/extracoes                          | Extrai arquivos da revisão atual           |
| PUT    | /solicitacoes/{id}/extracoes/{extracaoId}/conferencia | Registra conferência da transcrição        |
| POST   | /solicitacoes/{id}/analises                           | Executa regras e resumo de IA              |
| GET    | /solicitacoes/{id}/analises                           | Consulta resultados e histórico            |
| POST   | /solicitacoes/{id}/analises/{analiseId}/resumo        | Repete resumo em caso de falha             |
| POST   | /solicitacoes/{id}/submissoes-simuladas               | Conclui apenas no simulador                |
| POST   | /sessoes-captura/{id}/leituras                        | Credencial de simulador vinculada à sessão |
| GET    | /solicitacoes/{id}/leituras                           | Paciente consulta telemetria própria       |
| GET    | /admin/indicadores                                    | Administrador consulta agregados           |
