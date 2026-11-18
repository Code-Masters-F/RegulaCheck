# Research: App Android — condições da captura (RC-22, parte do app)

Decisões da feature 023. Regras de telemetria (faixas, prazo de 60 s, situações, códigos de orientação): [spec 016](../016-iot-simulada/spec.md) e [api-iot.md](../016-iot-simulada/contracts/api-iot.md); não repetidas aqui.

## D1 — E4.1 como passo do "anexar", dentro da tela de documentos

- **Decision**: a E4.1 é aberta pela folha E2.7 e desenhada no lugar do conteúdo de "Seus documentos", na mesma entrada da pilha. O estado `capturaTipo` é salvo com `rememberSaveable`, e o botão voltar do sistema fecha a E4.1 com um `BackHandler` próprio. O seletor do sistema e o envio continuam na `DocumentosTela`/`DocumentosViewModel` da RC-19.
- **Rationale**: a ação principal da E4.1 só abre o seletor, e o resultado do seletor precisa chegar ao `DocumentosViewModel`, que faz o envio com `X-Chave-Envio`. Como passo do mesmo fluxo, nada precisa passar de uma entrada da pilha para outra. O envio aparece na lista ("Enviando…" → "Enviado") sem nova navegação, e "voltar" leva aos documentos.
- **Alternatives considered**: `Tela.CondicoesCaptura` na pilha, com o resultado devolvido à tela anterior. Isso exigiria um mecanismo novo de resultado no `Navegador` ou acesso ao `ViewModelStore` de outra entrada: mais código e acoplamento, sem ganho para a paciente.

## D2 — "Fotografar" sem câmera (adaptação registrada)

- **Decision**: o app não abre a câmera. "Fotografar" leva à E4.1, e a ação principal dela ("Escolher a foto" ou "Escolher a foto mesmo assim", que substituem "Fotografar" e "Fotografar mesmo assim") abre o mesmo seletor do sistema da RC-19, filtrado para JPEG e PNG, com o tipo já escolhido. "Escolher arquivo", na folha e na E4.1, abre o seletor com PDF, JPEG e PNG. Foram ajustados o apoio de "Fotografar" na folha e o apoio do título da E4.1, que agora diz que a foto é tirada com a câmera do aparelho.
- **Rationale**: a câmera está fora do escopo (#40/#42/#47), e um botão "Fotografar" que abre um seletor de arquivos prometeria o que não acontece. O filtro de imagens dá sentido às duas ações da E4.1 sem criar outro seletor.
- **Alternatives considered**: manter "Fotografar" com o seletor, o que engana a paciente; ou remover "Escolher arquivo" da E4.1, o que contraria o desenho.

## D3 — Atualização periódica sequencial, ligada à visibilidade

- **Decision**: `CondicoesCapturaViewModel.acompanhar()` é uma função `suspend` chamada pela tela dentro de `repeatOnLifecycle(STARTED)`. Ela pede a sessão uma vez por visita e depois repete `consultar → delay(4 s)`. A próxima consulta só começa depois que a anterior termina, e uma trava (`Mutex.tryLock`) impede dois laços ao mesmo tempo. Sair da tela, abrir o seletor ou mandar o app para segundo plano cancela a corrotina, e o cancelamento também cancela a chamada HTTP em andamento (`Call.cancel`, já implementado no `ApiClient`).
- **Rationale**: a #47 pede atualização enquanto a tela está visível, sem empilhar requisições. Cada consulta é uma leitura nova do estado, não a repetição de uma operação que falhou. Mesmo depois de uma falha, a consulta seguinte acontece no ritmo normal, e é ela que permite mostrar a recuperação dos sensores. O pedido de sessão (POST) não se repete dentro da visita. Se falhar por rede, a tela mostra "indisponível" e continua só com a consulta. Uma nova visita ou a volta do segundo plano pede a sessão de novo, o que é seguro porque a rota é idempotente (spec 016, FR-008).
- **Alternatives considered**: `viewModelScope` com um `Job` cancelado em `onStop`, que exige ligar o ciclo de vida à mão; ou um intervalo fixo com `launch` a cada tick, que deixaria consultas se sobreporem com a rede lenta.

## D4 — Idade da leitura pelo relógio do servidor

- **Decision**: `idade = consultadaEm − ultimaLeitura.medidaEm`, com os dois valores vindos da mesma resposta. O resultado é limitado a no mínimo 0 e formatado como "{n} s", "{n} min" ou "{n} h".
- **Rationale**: o relógio do emulador pode divergir do servidor. A situação `DESATUALIZADA` já é calculada pela API com o mesmo relógio, então a idade mostrada concorda com ela.
- **Alternatives considered**: usar o relógio do aparelho (`Instant.now()`), que mostraria idades negativas ou inconsistentes com a situação.

## D5 — "Recuperada" como memória da visita

- **Decision**: o ViewModel guarda, por tipo de sensor, o último valor visto fora da faixa (`ABAIXO`/`ACIMA`). Quando o mesmo sensor volta a `DENTRO_DA_FAIXA`, ele passa a "Adequada de novo", com "Condições adequadas de novo." e "antes: {valor}". A marca vale enquanto ele continuar dentro da faixa e some se ele sair de novo. `DESATUALIZADA` e `SEM_LEITURA` não apagam o "esteve fora". A memória zera a cada nova visita (`novaVisita()`) e não muda com falha de rede.
- **Rationale**: o quadro `DGf2T` mostra a recuperação por sensor ("Adequada de novo" e "antes: 60 lux"), e `textos.md` traz "Condições adequadas de novo.". A API não guarda a transição, então ela só pode ser percebida comparando consultas da mesma visita.
- **Alternatives considered**: um aviso único na tela inteira, que esconderia qual sensor se recuperou; ou consultar o histórico de leituras, que não existe na API.

## D6 — Estados de tela e mapeamento de erros

- **Decision**: os estados são `Carregando`, `Exibindo(apresentação)` e `Encerrada(erro)`.
  - **Indisponível** = falha de rede, tempo esgotado, 5xx ou resposta fora do contrato, **ou** os dois sensores `SEM_LEITURA` (sem sessão ou sem leituras). É um `Exibindo`: a ação de seguir aparece ("… mesmo assim") e a consulta continua.
  - **Encerrada** = 401 (o app já vai ao login), 403, 404, ou 409 ao abrir a sessão. A consulta para e a tela mostra a explicação e o caminho de volta.
  - O rótulo da ação principal ganha "mesmo assim" quando a tela está indisponível ou quando algum sensor está desatualizado ou sem leitura. Fora da faixa, ele continua "Escolher a foto", como nos quadros `fOrmB` e `NyDDm`.
- **Rationale**: atende aos critérios da #47 e à regra de que o sensor não bloqueia nada. Os erros de acesso não são temporários, então repetir a consulta não ajudaria.

## D7 — Textos: quadro × `textos.md`

- **Decision**: quando o quadro e `textos.md` divergem, vale o quadro revisado, e `textos.md` é alinhado. No estado desatualizado, o quadro `Cna25` mostra a situação "Desatualizada" e a linha "Última leitura há {tempo} · sensor simulado", enquanto `textos.md` tinha "· desatualizada". Orientação: vale a `orientacao.mensagem` da API (pedido da #47 e do usuário). Os textos locais por código são usados só se a mensagem vier vazia; "Melhore a iluminação e tente de novo." é o desses textos para `MELHORAR_ILUMINACAO`. Criados: "Alta" (luminosidade acima da faixa, sem quadro), "Sem leitura" (só um sensor sem leitura) e "Consultando os sensores…" (carregando).
- **Rationale**: o desenho é a referência visual e a API é a fonte da orientação (contrato da spec 016, em que o código é o contrato e o texto pode mudar).
