# Checklist de revisão das telas

Aplicar a cada história (E1 a E4) antes de pedir aprovação. Registrar o resultado no fim, dizendo **o que foi e o que não foi revisado**. Marcar este checklist não comprova que o app funciona: ele só avalia o desenho.

## 1. Conteúdo e linguagem
- [ ] Aviso acadêmico visível em toda tela ("Aplicativo acadêmico · dados fictícios · protocolo de teste, não é exigência oficial").
- [ ] Selo **SIMULADO** em toda tela com extração, resumo ou comprovante.
- [ ] Comprovante com o destaque "SIMULAÇÃO ACADÊMICA — NÃO ENVIADO AO SUS".
- [ ] Nenhum texto **afirma** diagnóstico, urgência, autorização de atendimento, vaga, prazo, redução de fila ou aprovação do SUS.
- [ ] Os avisos que **negam** isso estão presentes onde são exigidos ("sem pendências identificadas não é aprovação do SUS"; "não entra em fila real").
- [ ] Informação clínica ausente ou documento médico incorreto → orienta procurar a unidade emissora; nenhuma tela oferece preencher o dado como se constasse no documento.
- [ ] Texto vindo de documento ou do resumo aparece como texto puro (inclusive `<`, `>`, `&`).
- [ ] Português do Brasil, frase curta, diz o que fazer em seguida.
- [ ] Só dados de `dados-ficticios.md`; nenhum dado de pessoa real, senha, token ou chave.

### Busca de termos (sobre `textos.md` e `INDEX.md`)
Listar as ocorrências de: `SUS`, `fila`, `vaga`, `urgên`, `aprov`, `autoriz`, `diagnóst`, `prazo`, `garant`. Cada ocorrência é conferida à mão: **negação ou aviso = permitida; afirmação = corrigir**. Registrar a contagem de cada tipo.

## 2. Estados (matriz tela × estado de `specs/007-epico-telas-pendev/contracts/telas.md`)
- [ ] Toda célula "P" tem quadro próprio com o nome `<ID> <Tela> — <Estado>`.
- [ ] Toda célula "C" tem nota na tela apontando o quadro ou componente que cobre o estado.
- [ ] Os sete estados do pedido têm quadro no detalhe (E1.3) com a próxima ação.
- [ ] `PROCESSANDO` não é apresentado como concluído; `SEM_PENDENCIAS_IDENTIFICADAS` não é apresentado como aprovação.
- [ ] 401 volta ao login; 403 mostra "sem permissão"; 404 mostra "item não encontrado".
- [ ] Falha de conexão tem mensagem, "tentar novamente" e campos preservados; nenhum envio é declarado concluído sem confirmação.

## 3. Acessibilidade
- [ ] Lista de `acessibilidade.md` aplicada ao quadro (contraste, toque, texto, cor, erros, leitura).

## 4. Ligação com o contrato
- [ ] Cada quadro traz a rota da API e o contrato de que depende (ou "sem chamada").
- [ ] Rotas marcadas *planejada* estão anotadas como "a confirmar com a história de API".
- [ ] Diferenças entre a tela e o contrato atual estão anotadas, não inventadas.

## 5. Registro da revisão
Copiar o bloco abaixo para cada história revisada.

```
História: E_ (nome)
Data: __/__/2026   Revisor: ________
Quadros revisados: __ de __
Itens reprovados e correção: ________
Busca de termos: ocorrências __ (afirmações corrigidas __; negações/avisos __)
Não revisado: ________
Aprovada pelo responsável? (sim/não/data): ________
```

### Revisão de linguagem dos textos (12/11/2026, só sobre `textos.md`, sem quadros)
Ocorrências por termo (linhas): SUS 6, fila 1, vaga 2, aprov 2, garant 2, prazo 1; urgência, autorização e diagnóstico 0. Todas lidas: negações, avisos ou o nome "cartão do SUS"; **0 afirmações**. Isto não revisa nenhum desenho.

| História | Revisão | Aprovação |
|---|---|---|
| E1 Entrada | não feita | pendente |
| E2 Envio | não feita | pendente |
| E3 Resultado | não feita | pendente |
| E4 Sensores, administrador e acessibilidade | não feita | pendente |
