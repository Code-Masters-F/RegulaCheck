# Quickstart: Conjunto sintético de demonstração (RC-23)

Verificado em 18/11/2026 só na regressão automática em memória (ver "Evidência de execução" em `tasks.md`). A execução manual pelo aplicativo ou pela API com Oracle **não foi feita**.

## Ler um conjunto (sem executar nada)

1. Abra `samples/catalogo-conjuntos.md` e escolha um conjunto.
2. Em `samples/conjuntos/<NN>-<nome>/`, leia o `gabarito.md`: regras, campos e resultado geral esperados.

## Executar pelo percurso da aplicação

1. Suba o backend e o banco conforme `docs/ambiente.md` (a leitura é **simulada**: o arquivo é reconhecido pelo hash).
2. Entre com a conta fictícia, crie um pedido e envie os arquivos do conjunto.
3. Peça a leitura e a análise; compare cada regra e o resultado geral com o `gabarito.md`.
4. Registre "coincide" ou "diverge" por regra (insumo da RC-25). Não altere o gabarito para igualar a saída.

## Executar a regressão automática (sem banco, sem rede)

A partir de `backend/`, no PowerShell:

```powershell
.\mvnw.cmd -Dtest=ConjuntosDemonstracaoTest test
```

Resultado esperado: todos os conjuntos coincidem com o gabarito. Os arquivos e o catálogo de leituras são commitados; só se criam arquivos novos de propósito com o gerador:

```powershell
.\mvnw.cmd -Dtest=GeradorDeAmostras -Dgerar.amostras=SIM test
```

O gerador não sobrescreve arquivo existente (o hash é a chave da leitura).
