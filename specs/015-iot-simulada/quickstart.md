# Quickstart: IoT simulada (RC-21 + RC-22)

Roteiro de verificação manual depois da implementação. Comandos a partir da raiz do repositório; no Windows PowerShell use `.\mvnw.cmd` e `$env:VAR = "..."`.

## 1. Testes automatizados (sem banco)

```bash
cd backend && sh ./mvnw clean verify   # JDK 21 no JAVA_HOME; o mvnw está no Git sem permissão de execução
```

Esperado: os testes novos de `iot` (classificador, rotas, concorrência, higiene de log, simulador por processo) verdes junto com os existentes.

## 2. Banco (Oracle, opcional neste ambiente)

1. Aplicar `database/scripts/11_iot_sessao_captura.sql` em banco existente (ou `01_` + `02_` em banco novo).
2. Rodar `04_testes_restricoes.sql` e conferir os casos novos de sessão aberta.
3. `RC_TESTE_ORACLE=SIM ./mvnw verify -Dtest=CapturaOracleTest`.

## 3. Demonstração ponta a ponta

1. Subir a API com `IOT_API_KEY` definida (valor qualquer, só local) e o banco configurado (ver `docs/ambiente.md`).
2. Login da paciente sintética A (`POST /auth/login`) e criar ou reusar um pedido.
3. Abrir a sessão: `POST /solicitacoes/{id}/sessoes-captura` → 201 e anotar o `id` da sessão (repetir a chamada devolve a mesma, 200). Na coleção Postman: "RC-21 Abrir sessao de captura".
4. Ver a saída do simulador sem enviar:

   ```bash
   java simulador-iot/SimuladorSensores.java --sessao <sessaoId> --sequencia fora-da-faixa --seco
   ```

5. Enviar `fora-da-faixa` e consultar `GET /solicitacoes/{id}/leituras` → luminosidade `ABAIXO_DA_FAIXA` (`MELHORAR_ILUMINACAO`) e distância fora da faixa.
6. Enviar `recuperacao` e consultar de novo → os dois `DENTRO_DA_FAIXA` (`CONDICAO_ADEQUADA`).
7. Esperar mais de 60 s sem enviar e consultar → os dois `DESATUALIZADA` (`VERIFICAR_ESTACAO`).
8. Enviar um documento e salvar o rascunho → funcionam igual, com sensores em qualquer situação.
9. Conferir no log da API que não aparecem a chave nem o corpo das leituras.

Registrar comandos e resultados reais em `docs/testes/` / `docs/evidencias/`.
