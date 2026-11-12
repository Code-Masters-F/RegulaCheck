# Research: Perfis e isolamento de dados (RC-05)

Decisões do plano. Nenhum item ficou em aberto (`NEEDS CLARIFICATION`).

## D1 — Recurso de outra paciente responde 404, perfil indevido responde 403
- **Decisão**: 404 `NAO_ENCONTRADO` quando o recurso não existe **ou** é de outra paciente; 403 `ACESSO_NEGADO` apenas quando o perfil não é permitido na rota.
- **Por quê**: o contrato de erros já reserva exatamente esses dois usos (`docs/api/erros.md`); 403 em recurso alheio confirmaria que o ID existe.
- **Alternativa rejeitada**: 403 para recurso alheio (vaza existência).

## D2 — Ordem de verificação: 401 → 403 → 404 → 400 → regra de negócio
- **Decisão**: o filtro de autenticação roda primeiro, o de perfil depois, o servlet só então busca o recurso pela dona e valida o corpo.
- **Por quê**: sem identidade não se avalia perfil; sem perfil não se toca o recurso; validar o corpo antes da dona permitiria sondar existência por mensagens diferentes.

## D3 — Negar por omissão via registro único de rotas privadas
- **Decisão**: `RotasPrivadas.registrar(contexto, caminho, servlet, perfis…)` é o único caminho para publicar rota privada; ele instala `AutenticacaoFiltro` + `AutorizacaoFiltro` e guarda a declaração. Um teste varre os mapeamentos do contexto e falha se algum caminho não for público conhecido (`/health`, `/auth/login`, coringa 404) nem declarado.
- **Por quê**: o risco real é uma história futura esquecer o filtro; a falha deve aparecer no `verify`. Perfis vazios na declaração são recusados na montagem (erro de configuração, não rota aberta).
- **Limite assumido**: `RotasPrivadas.exigirTudoDeclarado()` roda ao final de `Composicao.montar` e impede a API de subir se um servlet foi publicado direto no contexto; é uma guarda **de montagem**, não de cada requisição. Um filtro global `/*` por requisição mudaria o 404 das rotas inexistentes definido na RC-02 e foi rejeitado.
- **Alternativa rejeitada**: anotações com reflexão (mais código e magia, contra a simplicidade exigida); filtro global "tudo é privado" (complica as rotas públicas e o 404 coringa).

## D4 — O filtro de perfil não consulta o banco
- **Decisão**: usa o perfil já carregado por `AutenticacaoService.autenticar`, que lê a conta a cada requisição (RC-04). Isso satisfaz FR-013: mudança de perfil no banco vale na próxima chamada.
- **Por quê**: evita segunda consulta; nenhuma cópia do perfil fica no token.

## D5 — Dona do recurso garantida na consulta, não depois dela
- **Decisão**: o repositório de cada recurso expõe a busca já filtrada pela dona (`buscarDaPaciente(id, pacienteId)`, com a cadeia de FK até `RC_SOLICITACAO.PACIENTE_ID`), e o serviço passa o resultado por `Propriedade.exigir(Optional)`: vazio vira o mesmo 404 para "inexistente" e "alheio".
- **Por quê**: buscar primeiro e comparar o dono depois deixa uma janela de esquecimento e diferença de tempo; uma consulta só reduz ambos. Para aninhados (documento dentro de solicitação) a consulta confere a dona do documento **e** que o pai informado é o pai real.
- **Escopo**: esta história entrega o auxiliar, o padrão e os testes com recurso de exemplo; cada história de negócio escreve sua consulta.

## D6 — Administrador: 403 em recurso individual, sem lista vazia
- **Decisão**: rotas de recursos individuais declaram só `PACIENTE`; o administrador cai no 403 do filtro de perfil, antes de qualquer consulta. Rotas de protocolo/indicadores agregados declaram `ADMINISTRADOR` (a paciente também pode ler o protocolo vigente quando a história o exigir, declarando ambos).
- **Por quê**: lista vazia sugeriria "não há dados" e permitiria inferir; 403 é claro e barato.

## D7 — Simulador é uma chave de configuração, não uma conta
- **Decisão**: `IOT_API_KEY` (já prevista em `CLAUDE.md` §13), enviada no cabeçalho `X-Chave-Simulador`, validada por `SimuladorFiltro` **somente** na rota de telemetria. Comparação em tempo constante (`MessageDigest.isEqual` sobre SHA-256 de ambos). Sem a variável definida, a telemetria responde sempre 401 (API sobe normalmente).
- **Por quê**: não altera o Oracle; o simulador é um script, não uma pessoa; chave única é suficiente para o MVP.
- **Limitação registrada**: a chave não pertence a uma paciente, então não distingue "sessão de outra paciente"; a recusa vale para sessão inexistente/encerrada (404). Vínculo por sessão exigiria coluna `RC_` e fica para a RC-21 se for pedido.
- **Alternativas rejeitadas**: novo valor `SIMULADOR` no CHECK de `RC_USUARIO` (alteraria o banco do usuário, obrigaria senha/BCrypt para um script e misturaria pessoa e máquina); token por sessão de captura (precisa de coluna nova e da rota que o emite).

## D8 — Credenciais não se cruzam
- **Decisão**: a chave do simulador numa rota de conta responde 403 (`AutenticacaoFiltro` a reconhece só para recusar com o código certo); um token de conta na rota de telemetria responde 403 (`SimuladorFiltro` consulta a sessão); rota de telemetria sem credencial nenhuma responde 401.
- **Por quê**: 401 = sem identidade válida; 403 = identidade válida sem permissão. Mantém a regra única do contrato.

## D9 — Prova com rotas e recursos de exemplo só em teste
- **Decisão**: as rotas de exemplo (`/teste/recursos/{id}`, `/teste/documentos/{id}`, `/teste/admin/indicadores`, `/teste/telemetria/{id}`) vivem em `src/test` e são montadas com a mesma `RotasPrivadas` da produção.
- **Por quê**: FR-023 proíbe CRUD de negócio; a prova precisa usar o mecanismo real, não uma cópia.
- **Limite**: não prova as rotas de negócio futuras; cada história repete a matriz do seu recurso.

## D10 — Registro de recusas sem dado sensível
- **Decisão**: cada recusa 401/403/404-por-dona gera uma linha de log com `usuarioId` (quando houver), perfil, método, caminho sem query e código. Nunca token, chave, corpo ou nome de arquivo.
- **Por quê**: FR-021 e a constituição (nenhum log com segredo); o caminho pode conter ID de recurso, que é técnico, não sensível.
