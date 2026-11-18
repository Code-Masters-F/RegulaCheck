# Research: Conjunto sintético de demonstração (RC-23)

## Decisões

### D1 — Onde ficam os conjuntos
- **Decisão**: `samples/conjuntos/<NN>-<nome>/` com arquivos e `gabarito.md`; índice em `samples/catalogo-conjuntos.md`.
- **Racional**: `CLAUDE.md` define `samples/` como local de documentos sintéticos e gabaritos. Uma pasta por conjunto torna cada pacote independente e executável sozinho.
- **Alternativas**: um único `gabarito.md` gigante (difícil de ler e de ligar original→corrigido); pasta em `backend/src/test/resources` (esconde o material da equipe e da demonstração).

### D2 — Como o sistema "lê" os arquivos novos
- **Decisão**: manter a leitura simulada por SHA-256 e acrescentar as leituras dos arquivos novos em `backend/src/main/resources/extracao-simulada/catalogo.json`.
- **Racional**: é o mecanismo adotado em 12/11/2026 (RC-10); o leitor carrega um único recurso padrão (`LeitorSimulado.RECURSO_PADRAO`), então os conjuntos só funcionam na demonstração se estiverem nesse catálogo. Nenhuma alteração de código de produção.
- **Alternativas**: segundo catálogo de demonstração (exige mudança em `LeitorSimulado`/composição, fora do escopo); manter só em `catalogo-testes.json` (os conjuntos não funcionariam na demonstração real).

### D3 — Geração e imutabilidade
- **Decisão**: estender `GeradorDeAmostras` para criar apenas arquivos ausentes; nunca regenerar os existentes.
- **Racional**: o hash do arquivo commitado é a chave da leitura; regenerar um PDF pode trocar o hash e quebrar o catálogo (aviso já presente em `samples/README.md`).
- **Alternativas**: gerar tudo a cada execução (instável); criar arquivos à mão (não reproduzível e difícil de padronizar a marca de fictício).

### D4 — Gabarito antes da execução
- **Decisão**: o gabarito esperado por regra é escrito no código de definição e nos `gabarito.md` a partir da definição do protocolo; o teste de regressão vem depois, em commit posterior. O hash do commit dos gabaritos é registrado no catálogo.
- **Racional**: FR-005 exige ordem verificável. Gerar o gabarito do resultado do motor tornaria o teste circular.
- **Alternativas**: gabarito derivado da execução (circular, rejeitado); promessa verbal de ordem (não verificável).

### D5 — Data de referência
- **Decisão**: 12/11/2026 em todos os gabaritos (data do relógio fixo dos testes de análise existentes).
- **Racional**: a validade de 180 dias (RC-R05) e a maioridade (RC-R06) dependem da data; fixá-la evita resultados que mudam com o tempo.
- **Alternativas**: data de hoje (instável); datas relativas (confuso para quem lê o gabarito).

### D6 — Casos de fronteira dentro do catálogo
- **Decisão**: o conjunto 10 usa paciente que completa 18 anos exatamente na data de referência (regra condicional não aplicável); o conjunto 04 usa emissão claramente fora dos 180 dias, evitando o limite exato na primeira versão.
- **Racional**: a regra RC-R06 documenta que, com 18 anos ou mais "inclusive no dia do aniversário", a regra não se aplica; o conjunto exercita justamente essa fronteira. A fronteira de 180 dias já é coberta pelos testes da spec 013.
- **Alternativas**: dois conjuntos de fronteira (aumenta o escopo sem exigência).

### D7 — Formato inválido
- **Decisão**: dois arquivos pequenos: um de tipo não suportado (texto) e um PDF de 6 páginas (limite do MVP é 5). O gabarito diz "recusado no envio, sem leitura nem análise" e cita o motivo; o código HTTP exato é lido da API existente (`docs/api/openapi.yaml`) durante a verificação.
- **Racional**: evita arquivo de 5 MB no repositório e testa duas recusas distintas com arquivos de poucos KB.
- **Alternativas**: arquivo acima de 5 MB (pesa no repositório); só um tipo de recusa (cobertura menor).

### D8 — Foto cortada × foto ilegível (revisada em 17/11/2026)
- **Decisão**: **ilegível** (borrada) → campo `ILEGIVEL` → regras dependentes não verificáveis → `REQUER_CONFERENCIA` (conjunto 05). **Cortada** (a linha da data de nascimento ficou fora da foto, o resto foi lido com clareza) → campo `NAO_LOCALIZADO` num documento lido com clareza → pela spec 013 (L87) conta como **ausente** → RC-R03 `PENDENTE` (responsável paciente: refazer a foto inteira) → `COM_PENDENCIAS` (conjunto 06).
- **Racional**: a spec 013 e o JavaDoc de RC-R03 tratam não localizado em documento claro como ausente; a distinção "não localizado ≠ ausente" da constituição vale para documento ilegível ou não lido. Os dois conjuntos passam a exercitar resultados diferentes.
- **Alternativas**: modelar a foto cortada como `ILEGIVEL` (idêntica ao conjunto 05, sem ganho de cobertura).

## T001–T003 — evidências (17/11/2026)

- **T001**: `.\mvnw.cmd -DskipTests test-compile` (a partir de `backend/`): `BUILD SUCCESS`.
- **T003**: `docs/api/erros.md`: PDF com mais de 5 páginas → `400 REQUISICAO_INVALIDA` (`detalhes.arquivo`); extensão não permitida ou incompatível com o conteúdo → `415 EXTENSAO_INCOMPATIVEL`; conteúdo que não é PDF/JPEG/PNG → `415 TIPO_NAO_SUPORTADO`.
- **Data de referência**: `AvaliacaoService.dataDeReferencia` usa a **data do envio mais recente** dos documentos da leitura, no fuso `America/Bahia`. Nos testes, `CenarioAuth.relogio` é fixo em `2026-11-12T20:00:00Z` → referência **2026-11-12**. Em execução real é o dia do envio (a fronteira dos 18 anos só é exercitada na regressão).
- **Precedência do resultado geral** (`ResultadoGeral.de`): alguma `PENDENTE` → `COM_PENDENCIAS`; senão alguma `NAO_VERIFICAVEL` → `REQUER_CONFERENCIA`; senão `SEM_PENDENCIAS_IDENTIFICADAS`; `NAO_APLICAVEL` não influencia.
- **Reenvio (T028)**: `DocumentoService.enviar` só recusa pedido fora de `RASCUNHO`, chave de envio reutilizada e limite de 3 documentos; a análise não muda o estado do pedido. Logo é possível acrescentar documento (conjunto 14) e substituir (`CenarioDocumento.substituir`, conjunto 13) depois de uma análise; a nova revisão invalida a anterior (spec 010 FR-013). A restrição "não acrescentar após a primeira análise" do contrato da RC-15 é da **apresentação no app** (RC-16), não do backend.
- **Documento de autorização**: `TipoDocumental.AUTORIZACAO_RESPONSAVEL` é um valor válido de `TIPO_DOCUMENTO`. Nele, `NOME` e `DATA_NASCIMENTO` ficam `NAO_LOCALIZADO` (o nome do responsável não pode entrar em `NOME`, para não gerar divergência em RC-R02).

## T002 — matriz das seis regras (derivada das definições, sem executar o motor)

Persona adulta: Maria Exemplo da Silva, nascida em 1990-04-12. Menor: Lia Exemplo Souza, 2012-06-20. Limite: Davi Exemplo Costa, 2008-11-12 (18 anos exatos em 2026-11-12). Outra pessoa: Carlos Exemplo Lima, 1985-01-30. Emissão padrão 2026-10-20 (23 dias); vencida 2026-03-10 (247 dias). Referência 2026-11-12. `A`=atende, `P`=pendente, `NV`=não verificável, `NA`=não aplicável. Conferência `TC` = tudo confirmado, `NC` = nada conferido.

| # | Conjunto | Conf. | R01 | R02 | R03 | R04 | R05 | R06 | Geral |
|---|---|---|---|---|---|---|---|---|---|
| 01 | completo | TC | A | A | A | A | A | NA | SEM_PENDENCIAS_IDENTIFICADAS |
| 02 | sem-identificacao | TC | A | NV | P (paciente) | A | A | NV | COM_PENDENCIAS |
| 03 | sem-motivo | TC | A | A | A | P (unidade emissora) | A | NA | COM_PENDENCIAS |
| 04 | varias-pendencias (só encaminhamento, sem motivo e vencido) | TC | A | NV | P (paciente) | P (unidade) | P (unidade) | NV | COM_PENDENCIAS |
| 05 | foto-ilegivel (data de nascimento ilegível) | TC | A | NV | NV | A | A | NV | REQUER_CONFERENCIA |
| 06 | foto-cortada (data de nascimento não localizada) | TC | A | NV | P (paciente) | A | A | NV | COM_PENDENCIAS |
| 07 | outra-pessoa (identificação de Carlos) | TC | A | P (paciente) | A | A | A | NA | COM_PENDENCIAS |
| 08 | menor-com-autorizacao | TC | A | A | A | A | A | A | SEM_PENDENCIAS_IDENTIFICADAS |
| 09 | menor-sem-autorizacao | TC | A | A | A | A | A | P (paciente) | COM_PENDENCIAS |
| 10 | maioridade-limite | TC | A | A | A | A | A | NA | SEM_PENDENCIAS_IDENTIFICADAS |
| 11 | instrucao-no-documento | TC | A | A | A | A (conteúdo não é avaliado) | A | NA | SEM_PENDENCIAS_IDENTIFICADAS |
| 12 | formato-invalido | — | recusado no envio, sem leitura e sem análise | | | | | | — |
| 13 | corrigido-de-04 (substitui o encaminhamento e envia a identificação) | TC | A | A | A | A | A | NA | SEM_PENDENCIAS_IDENTIFICADAS (rev. 2) |
| 14 | corrigido-de-09 (envia a autorização) | TC | A | A | A | A | A | A | SEM_PENDENCIAS_IDENTIFICADAS (rev. 2) |
| 15 | nada-conferido (arquivos do 01) | NC | NV | NV | NV | NV | NV | NV | REQUER_CONFERENCIA |

Notas de derivação:
- RC-R02 exige ao menos dois documentos com nome **e** data confirmados; com um só (02, 04, 06) fica `NV` (documentos insuficientes), sem divergência.
- RC-R06 só olha a data de nascimento do documento de **identificação**; sem identificação ou com a data não confirmada (02, 04, 05, 06, 15) a condição é indeterminada → `NV`. No 10, a idade exata é 18 → `NA`.
- RC-R04/R05 sem encaminhamento confirmado seriam `NV`; todos os conjuntos acima têm encaminhamento. No 04, motivo ausente e emissão vencida em documento lido com clareza são `P`, responsável unidade emissora.
- RC-R03 é `P` quando há documento lido sem identificação (02, 04) ou identificação clara sem data (06).
- O conjunto 15 reaproveita os arquivos do 01 (mesmos hashes) sem nenhuma decisão de conferência: nenhum campo é evidência.

## Pendências

Nenhum `NEEDS CLARIFICATION` aberto. O que precisa ser conferido na implementação, sem inventar: o código/mensagem exatos de recusa de formato (D7) e a célula de RC-R02 com um documento sem nome legível (D8), ambos contra a spec 013 e a documentação da API.
