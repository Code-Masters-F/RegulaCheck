# Resultado da rodada de testes (RC-25)

> Rascunho em preenchimento. Leitura de documentos e resumo são **simulados** (decisão de 12/11/2026). Nenhum caso é aprovado sem execução e evidência. A revisão do plano e desta rodada por outro integrante continua **pendente**.

## 1. Versão e ambiente

| Item | Valor |
|---|---|
| Commit testado | `385b1fe` (main com o plano da RC-24, PR #77) + branch `024-executar-testes-evidencias` (só `docs/` e `specs/` alterados) |
| Data | 2026-11-18 |
| JDK | Temurin 25.0.2 (backend); app exige JDK 21 do Android Studio só na sessão |
| API / Oracle | `regulacheck-backend.jar` sobe e `/health` responde 200; **sem `DB_*` as rotas de negócio respondem 503** (tentativa registrada em `docs/evidencias/_ambiente/`). Oracle: aguardando o responsável |
| Emulador | `adb` 36.0.2 em `Android/Sdk/platform-tools`; **nenhum dispositivo conectado** no início; AVD ainda não iniciado |
| Variáveis | `DB_*`, `DOCUMENT_STORAGE_PATH`, `IOT_API_KEY` ausentes no ambiente do agente (só presença verificada, valores nunca registrados) |

## 2. Contagem por status

| Status | Casos |
|---|---|
| Aprovado | 0 |
| Reprovado | 0 |
| Bloqueado | 0 |
| Não executado | 94 |
| **Total** | **94** |

## 3. Casos

| Caso | Título | Status | Evidência |
|---|---|---|---|
| T01-01 | Login com credenciais válidas | Não executado | |
| T01-02 | Senha incorreta | Não executado | |
| T01-03 | E-mail inexistente (resposta idêntica à senha errada) | Não executado | |
| T01-04 | Login com campos ausentes ou vazios | Não executado | |
| T01-05 | Rota privada sem sessão, com token inválido e depois do logout | Não executado | |
| T01-06 | Resposta e log não vazam senha, hash ou token | Não executado | |
| T01-07 | App: login com senha incorreta | Não executado | |
| T01-08 | App: conta administrador não entra no app | Não executado | |
| T02-01 | A não vê pedidos de B na listagem nem no detalhe | Não executado | |
| T02-02 | A não altera nem exclui pedido de B | Não executado | |
| T02-03 | A não baixa nem lista documento de B | Não executado | |
| T02-04 | A não acessa leitura, conferência, análise, resumo nem submissão de B | Não executado | |
| T02-05 | Corpo inválido em pedido alheio continua 404 | Não executado | |
| T02-06 | Matriz automatizada de isolamento | Não executado | |
| T03-01 | Paciente em rota de indicadores administrativos | Não executado | |
| T03-02 | Administrador recebe somente agregados | Não executado | |
| T03-03 | Administrador não acessa pedido individual de paciente | Não executado | |
| T03-04 | Administrador não baixa documento, análise nem submissão | Não executado | |
| T04-01 | Criar rascunho válido | Não executado | |
| T04-02 | Campo obrigatório ausente, vazio ou inválido | Não executado | |
| T04-03 | Serviço inexistente | Não executado | |
| T04-04 | Trocar o serviço do rascunho (único campo editável) | Não executado | |
| T04-05 | Editar pedido que já saiu de rascunho | Não executado | |
| T05-01 | Enviar PDF válido | Não executado | |
| T05-02 | Enviar PNG válido | Não executado | |
| T05-03 | Enviar JPEG válido | Não executado | |
| T05-04 | Leitura simulada do conjunto 01 com origem dos campos | Não executado | |
| T05-05 | Repetir a leitura não duplica | Não executado | |
| T05-06 | Ler pedido sem documentos | Não executado | |
| T06-01 | Formato inválido (texto) | Não executado | |
| T06-02 | PDF com mais de 5 páginas | Não executado | |
| T06-03 | PDF protegido por senha | Não executado | |
| T06-04 | Arquivo vazio ou corrompido | Não executado | |
| T06-05 | Arquivo acima de 5 MB | Não executado | |
| T06-06 | Extensão incompatível com o conteúdo | Não executado | |
| T06-07 | Quarto documento e várias partes na mesma requisição | Não executado | |
| T06-08 | Recusas por teste automatizado (matriz de motivos) | Não executado | |
| T07-01 | Conjunto 01: análise sem pendências | Não executado | |
| T07-02 | Resumo simulado, coerente e sem promessa oficial | Não executado | |
| T07-03 | Só campo confirmado vale como evidência | Não executado | |
| T08-01 | Documento de identificação ausente | Não executado | |
| T08-02 | Motivo do encaminhamento ausente (responsável: unidade emissora) | Não executado | |
| T08-03 | Várias pendências de uma vez | Não executado | |
| T09-01 | Aplicável e atendida (menor com autorização) | Não executado | |
| T09-02 | Aplicável e pendente (menor sem autorização) | Não executado | |
| T09-03 | Não aplicável (maioridade no limite) | Não executado | |
| T09-04 | Condição impossível de verificar exige conferência | Não executado | |
| T10-01 | Foto ilegível: requer conferência, sem afirmar ausência | Não executado | |
| T10-02 | Foto cortada | Não executado | |
| T10-03 | Documento de outra pessoa | Não executado | |
| T10-04 | Paciente aponta divergência na transcrição | Não executado | |
| T10-05 | Valor digitado pela paciente é recusado | Não executado | |
| T11-01 | Corrigir o conjunto 04 com o conjunto 13 | Não executado | |
| T11-02 | Corrigir o conjunto 09 com o conjunto 14 | Não executado | |
| T11-03 | Resultado antigo não libera o pedido atual | Não executado | |
| T11-04 | Reenvio guiado (verificar a versão corrigida) | Não executado | |
| T11-05 | Edição durante a análise | Não executado | |
| T12-01 | Resumo indisponível: resultado determinístico preservado | Não executado | |
| T12-02 | Saída do resumo fora do contrato é rejeitada | Não executado | |
| T12-03 | Instrução escrita no documento é só dado | Não executado | |
| T12-04 | Falha do leitor simulado | Não executado | |
| T12-05 | Falha da análise e nova tentativa | Não executado | |
| T13-01 | Submissão válida | Não executado | |
| T13-02 | Submissão repetida é idempotente | Não executado | |
| T13-03 | Submissão com pendência é recusada | Não executado | |
| T13-04 | Submissão com análise antiga, sem análise ou em andamento | Não executado | |
| T13-05 | Submeter pedido de outra paciente | Não executado | |
| T13-06 | Pedido submetido é só consulta | Não executado | |
| T13-07 | Concorrência na conclusão | Não executado | |
| T14-01 | Excluir o próprio rascunho | Não executado | |
| T14-02 | Excluir sem a confirmação | Não executado | |
| T14-03 | Excluir pedido submetido | Não executado | |
| T14-04 | Excluir pedido que não é rascunho | Não executado | |
| T14-05 | Excluir pedido de outra paciente | Não executado | |
| T14-06 | Remover um documento do rascunho | Não executado | |
| T15-01 | Repetir um envio interrompido não duplica | Não executado | |
| T15-02 | Mesma chave com outro conteúdo | Não executado | |
| T15-03 | Sessão expirada na API | Não executado | |
| T15-04 | Banco indisponível | Não executado | |
| T15-05 | Pedir a análise duas vezes não duplica | Não executado | |
| T15-06 | App: sem conexão ao abrir um pedido | Não executado | |
| T15-07 | App: sessão expirada | Não executado | |
| T15-08 | App: envio interrompido por falta de rede e retentativa sem duplicar | Não executado | |
| T15-09 | App: queda durante a análise não presume resultado | Não executado | |
| T16-01 | Sequência normal | Não executado | |
| T16-02 | Fora da faixa | Não executado | |
| T16-03 | Sequência de recuperação | Não executado | |
| T16-04 | Leitura desatualizada | Não executado | |
| T16-05 | Envio sem credencial ou com chave errada | Não executado | |
| T16-06 | Alertas da estação na interface do app | Não executado | |
| T16-07 | Chave do simulador não vale fora da telemetria; conta não envia telemetria | Não executado | |
| T16-08 | Sensor indisponível não bloqueia envio de documento | Não executado | |
| T16-09 | Simulador reproduzível (`--seco`) e testes automatizados do backend | Não executado | |
| P01 | Login → pedido → upload → leitura → conferência → análise → resumo → correção → reanálise → submissão | Não executado | |

## 4. Achados

Nenhum registrado ainda.

## 5. Defeitos corrigidos e retestes

Nenhum.

## 6. Lacunas (plano, seção 9.5) e pendências

Ver plano de testes, seções 9.5 e 10. Revisão por outro integrante: **pendente**.
