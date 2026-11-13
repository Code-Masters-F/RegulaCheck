# Research: Épico de telas do aplicativo no pen.dev

Decisões do plano. Formato: decisão, motivo, alternativas.

## D1 — Cinco arquivos `.pen`, um por grupo de histórias
- **Decisão**: `00-fundamentos`, `01-entrada`, `02-envio`, `03-resultado`, `04-sensores-admin-acessibilidade` em `mobile/design/`.
- **Motivo**: um arquivo único ficaria pesado de revisar e travaria o trabalho em paralelo; um arquivo por história permite entregar, revisar e aprovar uma história de cada vez (SC-007).
- **Alternativas**: um arquivo só (difícil de revisar); um arquivo por tela (≈ 25 arquivos, perde a visão do conjunto).

## D2 — Um quadro por tela e por estado, com nome padronizado
- **Decisão**: nome `<ID> <Tela> — <Estado>` (ex.: `E1.2 Lista de pedidos — Vazia`); IDs `F` (fundamentos), `E1` (entrada), `E2` (envio), `E3` (resultado), `E4` (sensores/admin/acessibilidade). Os quadros ficam lado a lado, uma linha por tela, uma coluna por estado.
- **Motivo**: permite achar e conferir variantes (FR-002) e ligar o inventário aos quadros; nome estável vira o "contrato" entre o desenho e o código futuro.
- **Alternativas**: páginas por estado (esconde as variantes); sem padrão (impossível de auditar).

## D3 — Texto revisável fora do `.pen`
- **Decisão**: `textos.md` guarda todos os textos da interface por tela/estado em português; o desenho deve usar exatamente esses textos. A revisão de linguagem proibida (SC-003) e de tom é feita sobre esse arquivo.
- **Motivo**: a ferramenta do pen.dev manda acessar o `.pen` só pelas suas ferramentas (o site o chama de JSON aberto, mas isso não foi confirmado aqui); sem um espelho em texto não há revisão no Git nem busca por frases proibidas.
- **Alternativas**: revisar só visualmente (não repetível, sem histórico).

## D4 — Fundamentos antes das telas
- **Decisão**: `00-fundamentos` define variáveis (cores, tipografia, espaçamento, raios) e o catálogo de componentes: botão (primário, secundário, destrutivo, desabilitado, carregando), campo de texto, seletor, cartão de pedido, selo de estado (texto + ícone), cartão de pendência, linha de documento, aviso acadêmico, selo "SIMULADO", faixa de erro/sem conexão, diálogo de confirmação, esqueleto de carregamento, estado vazio, barra superior e navegação.
- **Motivo**: reaproveitar garante consistência e reduz retrabalho (cada tela tem várias variantes).
- **Alternativas**: desenhar tela a tela sem catálogo (inconsistência e retrabalho).

## D5 — Acessibilidade como metas objetivas
- **Decisão**: texto com contraste ≥ 4,5:1 (3:1 para texto grande e ícones), alvos de toque ≥ 48 dp, texto base ≥ 16 sp, informação nunca só por cor (sempre texto e/ou ícone), layout que suporta fonte ampliada sem cortar conteúdo essencial, rótulos para leitor de tela anotados no `INDEX.md` e ordem de leitura registrada por tela.
- **Motivo**: torna FR-026 verificável; segue as diretrizes de acessibilidade do Android/WCAG AA.
- **Alternativas**: "acessível" sem metas (não verificável).

## D6 — Conteúdo obrigatório em todas as telas
- **Decisão**: um componente **Aviso acadêmico** (rodapé ou faixa) em toda tela; o selo **SIMULADO** em resumo, extração e comprovante; vocabulário registrado em `checklist-revisao.md`. Proibido **afirmar** diagnóstico, urgência, autorização de atendimento, "aprovado pelo SUS", vaga garantida, promessa de prazo ou de redução de fila; as **negações e avisos** correspondentes ("não é aprovação do SUS", "não entra em fila real") são obrigatórios e permitidos. A busca de termos (T034) lista as ocorrências de palavras como "SUS", "fila", "vaga", "urgência" e "aprov", e cada ocorrência é conferida à mão como afirmação ou negação.
- **Motivo**: o risco do produto é parecer oficial ou clínico; o componente fixo reduz o risco de esquecimento.
- **Alternativas**: lembrar caso a caso (falha com o tempo).

## D7 — Dados fictícios padronizados
- **Decisão**: `dados-ficticios.md` define um conjunto único: duas pacientes (A e B), pedidos em cada um dos sete estados, documentos de exemplo (um PDF e uma foto), campos extraídos, pendências por regra do `PROTOCOLO-ACADEMICO-FICTICIO v0.1`, leituras de sensores e totais do administrador. Nomes e e-mails `.invalid`.
- **Motivo**: telas que contam a mesma história (o mesmo pedido em vários momentos) são mais fáceis de revisar e de demonstrar; evita dado real.
- **Alternativas**: dados diferentes por tela (confuso, risco de dado real).

## D8 — Capacidades do pen.dev e alternativa
- **Decisão**: a primeira tarefa de execução lê a documentação do pen.dev (`read_skill`) e `get_app_state` com o aplicativo conectado para confirmar: criação de variáveis, componentes reutilizáveis, quadros nomeados e **exportação de imagens**. Se houver exportação, as imagens de cada quadro vão para `mobile/design/previas/` (PNG) para facilitar a revisão no GitHub; se não houver, a revisão é feita no aplicativo e o `INDEX.md` registra o nome do quadro e uma descrição curta.
- **O que o site oficial informa** (https://www.pen.dev/, consultado em 12/11/2026, indicado pelo responsável): pen.dev, antigo pencil.dev, é uma "tela agêntica" de design; funciona como aplicativo, versão web gratuita e extensões para VS Code e Cursor, e tem CLI para uso sem interface; agentes (inclusive Claude Code) trabalham na tela por MCP; os quadros podem ser **exportados em PDF, PNG, JPEG e WEBP (1x, 2x, 3x)** e como HTML com CSS ou Tailwind; o `.pen` é descrito como "um arquivo JSON" de formato aberto e documentado; o site **não** menciona Git nem histórico de versões. Isso torna provável a exportação de prévias, mas a confirmação continua sendo a de T001, no aplicativo conectado. A mensagem de erro desta sessão ("antigravity_ide") indica que a ferramenta tentou falar com uma instalação do pen.dev em um IDE, que precisa estar aberta.
- **Cautela sobre o `.pen`**: apesar de o site descrevê-lo como JSON, as instruções da ferramenta pencil MCP dizem que os arquivos são protegidos e que só devem ser acessados pelas ferramentas do pen.dev. Por isso o plano continua tratando o `.pen` como **não legível e não revisável por diff**, e mantém o espelho em Markdown (D3). Se T001 mostrar que o arquivo é texto legível, a revisão pode usar também o diff.
- **Motivo**: não assumir recursos que não foram verificados (nesta sessão o pen.dev não estava conectado).
- **Alternativas**: assumir exportação (risco de plano irreal).

## D9 — Mapa de percursos em texto
- **Decisão**: `mapa-de-percursos.md` lista os percursos (entrada, envio, resultado, reenvio, comprovante, administrador, sensores, conexão ruim) como sequências de IDs de quadros, com um diagrama em texto (Mermaid) e as condições de cada ligação (ex.: 401 → login). Não há protótipo executável.
- **Motivo**: revisável em Git, independe de recurso do pen.dev e basta para SC-005.
- **Alternativas**: protótipo navegável no pen.dev (depende de recurso não verificado; pode entrar depois por decisão do grupo).

## Resolvido sem pergunta ao responsável
- Pasta `mobile/design/`, retrato 360 × 800 dp, sem modo escuro nem tablet (D1, spec Assumptions).
- Metas de acessibilidade numéricas (D5), usando padrões do Android e do WCAG AA.
