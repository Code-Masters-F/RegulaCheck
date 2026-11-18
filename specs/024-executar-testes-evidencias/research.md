# Research: Executar testes e registrar evidências (RC-25)

Sem NEEDS CLARIFICATION na spec. As decisões abaixo fecham escolhas de execução.

## D1 — Versão testada

- **Decisão**: testar o commit da `main` que contém o plano (merge do PR #77) mais esta branch; registrar o `git rev-parse --short HEAD` e se há alterações locais. Cada pasta de evidência cita o commit.
- **Racional**: o plano exige "commit/versão" por caso; o resultado deve corresponder ao que será entregue.
- **Alternativas**: tag dedicada (rejeitada: sem autorização para criar tags); versão por caso (rejeitada: confusa).

## D2 — Ordem de execução em ondas

- **Decisão**: ambiente → API real → P01 → dublês → app → sensores → correção/reteste → consolidação.
- **Racional**: API e P01 sustentam o objetivo da issue e destravam o resto; app e sensores dependem de dispositivo e do que a API já provou.
- **Alternativas**: seguir T01..T16 em ordem numérica (rejeitada: atrasa o P01 e mistura dependências de ambiente).

## D3 — Quem sobe o ambiente

- **Decisão**: a API com Oracle é iniciada pelo responsável (como na RC-19); o agente não lê `.env` nem copia `DB_*`. O agente executa curl, scripts e testes locais e registra o que viu. Sem API com banco, os casos "API real + Oracle" são **tentados** (resposta 503 registrada) e ficam Bloqueados.
- **Racional**: credenciais não entram no agente nem no repositório (spec FR-011); a tentativa registrada cumpre a regra do plano.
- **Alternativas**: H2 ou banco em memória para "simular Oracle" (rejeitada: troca silenciosa do banco e prova diferente).

## D4 — O que conta como "execução real" do percurso

- **Decisão**: "upload real" = arquivo real de `samples/` enviado ao backend por HTTP ou pelo app; extração e resumo seguem **simulados**. O P01 usa conjuntos 09 → 14 (plano, seção 8).
- **Racional**: decisão de 12/11/2026; evita prometer OCR ou IA reais.
- **Alternativas**: integrar OCR/IA real (fora de escopo e proibido pelo CLAUDE.md §11).

## D5 — Casos por dublê e testes automatizados

- **Decisão**: executar o teste automatizado indicado pelo caso (ex.: `ResumoEndpointTest`, `LeituraMalSucedidaTest`) com comando e saída bruta, e registrar "Dublê (não é integração real)". A baseline `mvnw clean verify` não aprova caso, a menos que o caso aponte esse teste como seu procedimento.
- **Racional**: o plano 5.5 declara que a falha de IA só se provoca por dublê.
- **Alternativas**: alterar o adaptador para falhar na API real (rejeitada: código só para teste; quebra "mudanças mínimas").

## D6 — App no emulador

- **Decisão**: localizar o `adb` (Android SDK / Android Studio), usar o emulador existente (RC-19 usou `Medium_Phone`), JDK 21 só na sessão, URL `http://10.0.2.2:8080`. Capturas com `adb exec-out screencap` salvas em `bruto/`. Sem dispositivo, casos do app ficam Bloqueados após tentativa.
- **Racional**: reproduz o procedimento já usado em RC-18/RC-19; evidência anterior não substitui execução (FR-015).
- **Alternativas**: testes de UI instrumentados novos (rejeitada: escopo de teste novo, não de execução).

## D7 — Impedimentos conhecidos (RC-20, RC-22)

- **Decisão**: no início da execução, conferir se as rotas de indicadores e a tela de alertas já existem (grep/chamada). Se existirem, executar normalmente. Se não, **tentar** (ex.: `GET /admin/indicadores` → 404; ausência de tela) e marcar Bloqueado com a resposta bruta.
- **Racional**: o plano proíbe Bloqueado sem tentativa; a existência pode mudar até a execução.
- **Alternativas**: marcar Bloqueado direto (rejeitada: viola a regra 2 do plano).

## D8 — Material que falta (PDF protegido, JPEG)

- **Decisão**: T06-03 exige PDF protegido sintético: gerar um arquivo novo **fora** de `samples/` existentes (nome novo, nunca sobrescrever amostra) e registrar o método e o hash; JPEG idem. Se não for possível gerar, Bloqueado com o motivo.
- **Racional**: o hash identifica a leitura simulada; regenerar amostra quebraria gabaritos.
- **Alternativas**: dispensar o caso (rejeitada: lacuna declarada deve ser tentada).

## D9 — Formato e mascaramento da evidência

- **Decisão**: cada pasta tem `registro.md` (campos fixos, ver contrato) e `bruto/` (saídas, respostas, capturas). Token, senha, chave e `DB_*` são substituídos por `<TOKEN>`, `<SENHA>`, `<CHAVE>`, `<DB>` antes de gravar; após a rodada, busca por padrões sensíveis no repositório.
- **Racional**: SC-006/FR-011; evita o erro mais caro (credencial versionada).
- **Alternativas**: gravar bruto sem tratamento (rejeitada).

## D10 — Defeitos

- **Decisão**: classificar achado como **bloqueador** (impede P01 ou um cenário essencial) ou **não bloqueador**. Bloqueador: registrar falha (Reprovado), corrigir com teste, repetir os casos afetados e o P01 em execução nova; a falha original fica na pasta (`registro.md` com histórico de execuções). Não bloqueador: achado pendente no resumo.
- **Racional**: critério da issue; auditabilidade.
- **Alternativas**: reexecutar tudo após cada correção (rejeitada: fora do critério e do tempo).

## D11 — Resumo da rodada

- **Decisão**: `docs/testes/resultado-da-rodada.md` com versão, ambiente, contagens por status (somando 94), tabela caso → status → evidência, achados, correções, lacunas e pendências (revisão por outro integrante continua pendente).
- **Racional**: SC-005 e FR-014; ponto único para a apresentação e para o PDF.
- **Alternativas**: só atualizar o plano (rejeitada: sem visão consolidada).

## D12 — Autorizações

- **Decisão**: sem commit, push, PR, comentário ou fechamento de issue sem pedido explícito. A execução produz arquivos no working tree; o responsável autoriza o fechamento. Operação destrutiva no banco: só com autorização específica.
- **Racional**: CLAUDE.md §2.9 e instruções do usuário.
