# Arquitetura

Monolito modular Java/Spring Boot: um processo, um módulo Maven (`backend/`), pacotes por funcionalidade em `br.com.regulacheck`, cada um com as camadas controller/service/repository/entity/dto conforme necessário. Cada pacote ainda vazio tem um `package-info.java` descrevendo sua finalidade; não há classes de negócio.

## Módulos
config, auth, usuario, solicitacao, documento, extracao, protocolo, analise, validacao (model, regra), integracao/ia, integracao/extracao, armazenamento, iot, submissao, indicador, common/exception.

## Regras de dependência
- Controller -> Service (nunca Repository). Service -> Repository, motor de regras, contratos de integração.
- Repository não chama Controller, Service nem IA.
- Regras documentais não dependem de HTTP, banco ou SDK de IA.
- Adaptadores externos não decidem o estado de negócio.
- IA explica resultados; não substitui o motor determinístico.
- Entidades não são expostas na API (usar DTOs). Nenhuma credencial vai ao mobile.

## Estados da solicitação (propostos)
RASCUNHO, PROCESSANDO, REQUER_CONFERENCIA, COM_PENDENCIAS, SEM_PENDENCIAS_IDENTIFICADAS, FALHA_ANALISE, SUBMETIDA_SIMULACAO.

Transições previstas: RASCUNHO -> PROCESSANDO -> (REQUER_CONFERENCIA | COM_PENDENCIAS | SEM_PENDENCIAS_IDENTIFICADAS | FALHA_ANALISE); após correção/reenvio volta a PROCESSANDO; somente SEM_PENDENCIAS_IDENTIFICADAS -> SUBMETIDA_SIMULACAO. O estado do resumo de IA é distinto do estado da validação. Implementação futura.

## Contratos
Ver [api/README.md](api/README.md) e [../CONTRATO_API.md](../CONTRATO_API.md). A conferência da transcrição é separada de alteração clínica do documento original; informação clínica ausente é corrigida pela unidade emissora. O sistema não diagnostica, não define urgência nem autoriza atendimento; a submissão é simulada.
