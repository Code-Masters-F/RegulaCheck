# Autorização por perfil e por dona (RC-05)

Guia para quem implementa uma rota privada. Contrato completo: [specs/005-perfis-isolamento/contracts/api-autorizacao.md](../../specs/005-perfis-isolamento/contracts/api-autorizacao.md). Códigos e ordem: [erros.md](erros.md).

## Regras em uma frase
- Quem chama é a **conta da sessão**, nunca um identificador enviado no corpo, caminho ou parâmetro.
- Cada rota privada declara **quais perfis** podem usá-la; sem declaração a rota nem é registrada.
- Recurso de outra paciente responde **igual a um recurso que não existe** (404).
- O administrador vê indicadores agregados; **não** vê documentos, análises, resumos nem submissões individuais.
- O simulador de sensores usa só a chave `IOT_API_KEY` e só na rota de telemetria.

## Como proteger uma rota nova (5 passos)
1. **Declare a rota** em `Composicao`, pelo registro (nunca com `contexto.addServlet` direto):
   ```java
   rotas.registrar("/solicitacoes/*", new SolicitacaoController(servico), Perfil.PACIENTE);
   ```
   Para a telemetria: `rotas.registrarTelemetria("/sessoes-captura/*", controller)`.
   *Uso real (RC-07):* `/protocolos` e `/solicitacoes/*` estão registradas assim em `Composicao`; `SolicitacaoService.buscarPropria` aplica o passo 3 e `trocarProtocolo` devolve o 404 de dona (`Propriedade.naoEncontrada`) quando a operação atômica do repositório não acha o pedido da paciente. Como o mecanismo recusa dois registros para o mesmo caminho, as próximas histórias (extração, análise) estendem o roteamento com os subcaminhos, ou o trocam por um roteador interno.
   *Uso real (RC-08):* os subcaminhos `/{id}/documentos[/{docId}]` são entregues pelo `SolicitacaoController` ao `DocumentoController` (`DocumentoController.reconhece(pathInfo)`; sem herança), e a rota `/solicitacoes/*` passou a ser registrada com a sobrecarga `registrar(caminho, servlet, MultipartConfigElement, perfis…)` para aceitar `multipart/form-data`. `DocumentoService` confere o pedido pelo passo 3 **antes** de ler o corpo, e o documento pelo repositório já filtrado pela dona **e** amarrado ao pedido do caminho (documento de outro pedido da mesma paciente também é 404). Qualquer outro subcaminho desconhecido responde 404 padrão.
2. **Identifique a paciente pela sessão** no controller: `AutenticacaoFiltro.sessaoDe(requisicao)` e passe `usuarioId()` ao Service. Ignore qualquer campo de dono do corpo.
3. **Busque já filtrado pela dona** no Repository (junção até `RC_SOLICITACAO.PACIENTE_ID` e `PACIENTE_ID = ?` na mesma instrução, parametrizada) e passe o resultado por `Propriedade.exigir(optional, sessao)`: vazio vira o 404 padrão, tanto para inexistente quanto para alheio. Em rota aninhada confira também que o pai informado é o pai real do filho.
4. **Valide o corpo depois** de achar o recurso (um corpo ruim em recurso alheio deve continuar 404).
5. **Copie os testes** de `backend/src/test/java/br/com/regulacheck/auth/autorizacao/` (`MatrizPacienteTest`, `MatrizAdministradorTest`, `OrdemDeVerificacaoTest`) trocando o recurso de exemplo pelo seu: o esperado é 401 sem token, 403 com perfil indevido, 404 para alheio ou inexistente e sucesso para a dona.

## O que o build acusa
- `RotasPrivadas.registrar` recusa lista de perfis vazia, caminho repetido e caminho público.
- `Composicao.montar` termina chamando `exigirTudoDeclarado()`: se algum servlet foi publicado fora do registro (e não é `/health`, `/auth/login` nem o coringa 404), a API **não sobe** e `RotasPrivadasTest` falha.

## Limites conhecidos
- Não existe guarda genérica em tempo de execução; a proteção contra rota esquecida é a falha na montagem e nos testes.
- A chave do simulador é única por ambiente: não distingue de qual paciente é a sessão de captura. A telemetria só aceita sessão existente e aberta.
- Um servlet de exemplo não prova as rotas de negócio futuras: cada história repete a matriz do seu recurso.
