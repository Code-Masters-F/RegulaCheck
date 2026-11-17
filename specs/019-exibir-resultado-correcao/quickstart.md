# Quickstart de validação: RC-15

## Implementação backend — 17/11/2026

Recorte autorizado: `AnaliseResponse` passou a fornecer `rotuloResultado` e `pendencias[].orientacao` nas respostas existentes. Nenhuma rota, tabela ou regra foi acrescentada. O responsável, a revisão, o resumo simulado e as evidências permanecem vinculados à análise original. A orientação da RC-R02 é condicional porque a API não determina qual documento está errado.

**Verificações executadas** a partir de `backend/`:

- `rtk proxy .\mvnw.cmd -Dtest=OrientacaoResultadoEndpointTest test`: falhou antes da implementação pelos campos ausentes, conforme esperado. Depois da alteração, `rtk proxy .\mvnw.cmd -q -Dtest=OrientacaoResultadoEndpointTest test`: passou (3 testes).
- `rtk proxy .\mvnw.cmd -q -Dtest=HistoricoAnalisesEndpointTest test`, `ResumoEndpointTest` e `MatrizPerfisAnaliseTest` (mesmo formato, executados em sequência): passaram.
- `rtk proxy .\mvnw.cmd -q -DskipTests=false verify`: passou. Relatórios Surefire: 134 suites, 695 testes, 0 falhas, 0 erros, 32 ignorados. Testes com Oracle não executados sem ambiente de banco.
- `rtk proxy python -c "import yaml; yaml.safe_load(open('docs/api/openapi.yaml',encoding='utf-8')); print('yaml ok')"`: `yaml ok`.
- `rtk git diff --check`: sem erros.

**Pendente:** apresentação Android, leitura por voz, validação em aparelho, SC-005 com participantes e ações de reenvio/reanálise da RC-16. Os cenários abaixo continuam como roteiro futuro e não foram marcados como aprovados.

Este guia também descreve **verificações futuras da interface**. Em 16/11/2026 o projeto Android ainda não existia na `main`; nenhum cenário Android abaixo foi executado.

## Bloqueio da interface Android — 17/11/2026

Na branch `019-exibir-resultado-correcao`, `mobile/app/` não existe; faltam `settings.gradle.kts`, `ApiClient.kt`, `ResultadoScreen.kt` e `MainActivity.kt` previstos no plano anterior de interface. A branch local `017-app-android-percurso-minimo` também não contém `mobile/app/` (`git ls-tree -r --name-only 017-app-android-percurso-minimo -- mobile/app` retornou vazio). O checklist `checklists/requirements.md` tem 16 itens marcados e nenhum pendente. A implementação Android permanece pendente; nenhum teste Android foi executado. As tarefas deste recorte backend estão em `tasks.md`.

## Pré-requisitos

1. Base do aplicativo Android disponível, com login e navegação para o resultado do próprio pedido.
2. Backend RC-13/RC-14 acessível com banco e **dados sintéticos** do demonstrador; consultar `mobile/README.md` para endereço no emulador e `docs/ambiente.md` para configuração.
3. Conferir [contrato de apresentação](contracts/apresentacao-resultado.md), [modelo](data-model.md), `mobile/design/textos.md` (E3.3/E3.4) e `mobile/design/acessibilidade.md`.

## Execução planejada

- Com Maven disponível, verificar a base de resumo já publicada: `mvn -f backend/pom.xml -Dtest=ResumoEndpointTest test` (a partir da raiz). Isso verifica a RC-14, não a tela RC-15.
- Quando o projeto Android estiver em `mobile/` e fornecer Gradle Wrapper, executar a partir de `mobile/` `./gradlew test` e `./gradlew connectedAndroidTest` (ou `gradlew.bat` no Windows). Confirmar os nomes reais das tarefas no README da base antes de rodar; hoje esses comandos não são executáveis neste repositório.
- Em um emulador ou aparelho, autenticar a paciente fictícia e abrir resultados sintéticos: Pedido 2 (pendências da unidade emissora), Pedido 7 (informação não verificável) e Pedido 1 revisão 2 (sem pendências). Confirmar que os dados visíveis são os da análise selecionada, e não os de outra revisão.
- Abrir cada pendência: conferir código, descrição, versão, responsável e trecho apenas quando a evidência tem `origem`. Usar texto com `<`, `>` e `&` para verificar exibição literal.
- Simular resumo ausente ou `PADRAO`: resultado e pendências continuam visíveis, explicação padrão e selo SIMULADO aparecem, e “Tentar de novo” está disponível quando vigente. Simular 409 após mudança de revisão: recarregar o histórico e não combinar resumos.
- Simular rede/503 na consulta: mostrar falha textual e “Tentar novamente”, sem conclusão nova. Verificar estado em andamento, sem análise, resultado anterior, 401 (login), 403 (acesso negado), 404 (pedido não encontrado) e ausência de trecho. Nenhum caso deve mostrar conclusão não confirmada ou botão de edição clínica.
- Testar RC-R02 com `responsavel=PACIENTE`: divergência isolada pede conferência e informa condicionalmente que, se o erro estiver no original, a paciente solicita revisão à unidade, que corrige o documento; a tela não afirma saber qual original está errado. Testar arquivo ausente sem inclusão permitida: a limitação e o próximo passo são textuais, sem botão de anexar inoperante.
- Conferir leitura por voz, ordem dos elementos, texto ampliado, alvos de toque e estado entendido sem cor, seguindo `mobile/design/acessibilidade.md`.
- Para SC-005, observar pelo menos 3 pessoas que não desenharam o fluxo. Dar um cenário que a paciente pode resolver e outro que exige a unidade emissora; registrar tempo, resposta e dificuldades. Não marcar sucesso antes da execução.

## Resultado esperado

Os critérios SC-001 a SC-004 da [spec](spec.md) passam nos cenários sintéticos, e a SC-005 tem evidência de avaliação real. Registrar comandos, ambiente, participantes anonimizados, resultados e bloqueios no artefato de verificação da implementação. Não usar a revisão visual dos quadros como substituta desses testes.
