"""Gera entrega/RegulaCheck-entregaveis.pdf a partir dos documentos do repositório.

Uso (a partir da raiz do repositório): uv run python -X utf8 entrega/gerar_pdf.py
Requer o Google Chrome ou o Microsoft Edge instalado (impressão em PDF sem cabeçalho) e acesso
à internet para carregar o Mermaid (diagrama ER) de cdn.jsdelivr.net.
Capturas de tela do app: coloque PNGs em entrega/prints/ com os nomes listados em TELAS e gere de novo.
"""
import html
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
SAIDA = RAIZ / "entrega" / "RegulaCheck-entregaveis.pdf"
CASOS = ["T01-01", "T01-02", "T01-03", "T01-04", "T01-05", "T02-01", "T02-02", "T04-01", "T04-02", "T04-03"]
TELAS = [("login.png", "Login"), ("pedidos.png", "Meus pedidos"), ("pedido.png", "Detalhe do pedido"),
         ("documentos.png", "Envio de documentos"), ("resultado.png", "Resultado da análise")]
INTEGRANTES = [("557517", "Guilherme Carneiro Avellar"), ("557577", "João Vitor Oliveira Santos"),
               ("98493", "Eliseu Fernando Mendes dos Santos"), ("557449", "Victor Martins Ferreira Brandão Ameno"),
               ("556357", "Mere Angela da Silva")]
ENDPOINTS = [
    ("POST", "/auth/login", "Autentica a conta; devolve token de sessão", "200, 400, 401, 415"),
    ("GET", "/auth/sessao", "Identidade e expiração da sessão", "200, 401"),
    ("POST", "/auth/logout", "Encerra a sessão", "204, 401"),
    ("GET", "/protocolos", "Serviços (protocolos) ativos", "200, 401, 403"),
    ("POST", "/solicitacoes", "Cria o rascunho do pedido", "201, 400, 401, 422"),
    ("GET", "/solicitacoes", "Lista os pedidos da paciente da sessão", "200, 401"),
    ("GET", "/solicitacoes/{id}", "Detalhe do pedido próprio", "200, 401, 404"),
    ("PUT", "/solicitacoes/{id}", "Troca o serviço do rascunho", "200, 400, 404, 409, 422"),
    ("DELETE", "/solicitacoes/{id}", "Exclui o rascunho (confirmação obrigatória)", "204, 400, 404, 409"),
    ("POST", "/solicitacoes/{id}/documentos", "Envia um documento (PDF, JPEG, PNG)", "201, 400, 404, 413, 415"),
    ("PUT", "/solicitacoes/{id}/documentos/{docId}", "Substitui o documento", "200, 201, 404, 409"),
    ("DELETE", "/solicitacoes/{id}/documentos/{docId}", "Remove o documento", "200, 404, 409"),
    ("POST", "/solicitacoes/{id}/extracoes", "Leitura simulada dos documentos", "200, 201, 422"),
    ("PUT", "/solicitacoes/{id}/extracoes/{extracaoId}/conferencia", "Paciente confirma ou marca divergência", "200, 400, 409"),
    ("POST", "/solicitacoes/{id}/analises", "Executa o motor de regras e grava o resultado", "200, 201, 409, 503"),
    ("POST", "/solicitacoes/{id}/resumo", "Resumo de pendências (simulado)", "200, 201, 409"),
    ("POST", "/solicitacoes/{id}/reenvio", "Verifica a versão corrigida e compara", "200, 201"),
    ("POST", "/solicitacoes/{id}/submissoes-simuladas", "Submissão simulada com comprovante", "200, 201, 409"),
    ("POST", "/solicitacoes/{id}/sessoes-captura", "Abre a sessão de captura dos sensores", "200, 201, 409"),
    ("POST", "/sessoes-captura/{id}/leituras", "Simulador envia leitura (X-Chave-Simulador)", "201, 400, 401, 404"),
    ("GET", "/solicitacoes/{id}/leituras", "Situação e orientação dos sensores", "200, 404"),
]

CSS = """
@page { size: A4; margin: 16mm 14mm; }
body { font: 10.5pt/1.45 "Segoe UI", Arial, sans-serif; color: #1a1a1a; }
h1 { font-size: 24pt; margin: 0 0 4pt }
h2 { font-size: 15pt; border-bottom: 2px solid #0b5; padding-bottom: 3pt; margin-top: 22pt; page-break-after: avoid }
h3 { font-size: 12pt; margin: 14pt 0 4pt } h4 { font-size: 10.5pt; margin: 10pt 0 3pt }
table { border-collapse: collapse; width: 100%; margin: 6pt 0; font-size: 9pt }
th, td { border: 1px solid #bbb; padding: 3pt 5pt; vertical-align: top; text-align: left } th { background: #eef5ef }
pre { background: #f5f5f5; border: 1px solid #ddd; padding: 6pt; font-size: 8pt; white-space: pre-wrap; word-break: break-word }
code { font-size: 8.5pt } ul { margin: 0; padding-left: 14pt }
.aviso { border-left: 4px solid #c80; background: #fff8e8; padding: 6pt 10pt; margin: 8pt 0 }
.mermaid { text-align: center } .mermaid svg { max-width: 100%; height: auto }
.telas { display: flex; flex-wrap: wrap; gap: 8pt } figure { margin: 0; width: 31%; text-align: center }
figure img { width: 100%; border: 1px solid #ccc }
figure.falta div { border: 2px dashed #c33; color: #c33; padding: 60pt 4pt; font-size: 8pt } figcaption { font-size: 8.5pt }
.capa { page-break-after: always; padding-top: 60pt } .quebra { page-break-before: always }
"""


def esc(texto):
    return html.escape(texto, quote=False)


def secao_er():
    md = (RAIZ / "docs" / "modelo-de-dados.md").read_text(encoding="utf-8")
    er = re.search(r"```mermaid\n(.*?)```", md, re.S).group(1)
    return '<div class="mermaid">' + esc(er) + "</div>"


def consultas():
    sql = (RAIZ / "database" / "scripts" / "03_consultas_uso.sql").read_text(encoding="utf-8")
    partes = re.split(r"-- -{70,}\n-- Consulta ", sql)[1:]
    saida = []
    for p in partes:
        titulo = p.split("\n", 1)[0]
        achado = re.search(r"-- Esperado\s*:\s*(.*?)\n-- -{20,}", p, re.S)
        esperado = re.sub(r"\n--\s+", " ", achado.group(1)) if achado else ""
        corpo = p.split("-- " + "-" * 77 + "\n", 1)[-1]
        corpo = "\n".join(l for l in corpo.splitlines() if not l.startswith("--")).strip()
        saida.append("<h4>Consulta " + esc(titulo) + "</h4><pre>" + esc(corpo) + "</pre><p><b>Resultado esperado na carga sintética:</b> " + esc(esperado) + "</p>")
    return "\n".join(saida)


def tabela_casos():
    linhas = []
    for c in CASOS:
        reg = (RAIZ / "docs" / "evidencias" / c / "registro.md").read_text(encoding="utf-8")
        titulo = re.match(r"# \S+ — (.*)", reg).group(1)
        itens = re.search(r"### Esperado × obtido\n(.*?)\n\n", reg, re.S).group(1).splitlines()
        status = re.search(r"\| Status \| (\w+) \|", reg).group(1)
        lis = "".join("<li>" + esc(i[2:]) + "</li>" for i in itens)
        linhas.append("<tr><td>" + c + "</td><td>" + esc(titulo) + "</td><td>" + status + "</td><td><ul>" + lis + "</ul></td></tr>")
    return "<table><tr><th>Caso</th><th>Cenário</th><th>Status</th><th>Esperado × obtido (execução real)</th></tr>" + "".join(linhas) + "</table>"


def trecho(caso, arquivo, limite=900):
    texto = (RAIZ / "docs" / "evidencias" / caso / "bruto" / arquivo).read_text(encoding="utf-8")
    return esc(texto[:limite])


def telas():
    blocos = []
    for arquivo, nome in TELAS:
        p = RAIZ / "entrega" / "prints" / arquivo
        if p.exists():
            blocos.append('<figure><img src="' + p.as_uri() + '"><figcaption>' + nome + "</figcaption></figure>")
        else:
            blocos.append('<figure class="falta"><div>INSERIR CAPTURA<br>entrega/prints/' + arquivo + "</div><figcaption>" + nome + "</figcaption></figure>")
    return '<div class="telas">' + "".join(blocos) + "</div>"


CORPO = """
<div class="capa">
<h1>RegulaCheck</h1>
<p><b>Global Solution 2026/2 — CitizenTech · FIAP · 3ESOA</b><br>Grupo CodeMasters</p>
<table><tr><th>RM</th><th>Integrante</th></tr>@@INTEGRANTES@@</table>
<div class="aviso"><b>Dados sintéticos e protocolo acadêmico fictício.</b> A leitura de documentos, o resumo de pendências e os sensores são <b>simulados</b>. O sistema não diagnostica, não define urgência, não autoriza atendimento e não coloca ninguém em fila real do SUS. “Sem pendências identificadas” não é aprovação do SUS.</div>
<h3>Problema, cidadã e ODS</h3>
<p>A paciente que precisa enviar um encaminhamento a um serviço de saúde não sabe se a documentação está completa; a falta só aparece depois, e o retorno custa tempo e deslocamento. O RegulaCheck confere critérios documentais antes do envio, explica as pendências, permite corrigir e reenviar e simula a submissão. ODS: <b>3</b> (saúde e bem-estar), <b>10</b> (redução das desigualdades) e <b>9</b> (inovação).</p>
<h3>Pitch e código</h3>
<p>Vídeo pitch: <b>&lt;PREENCHER o link&gt;</b> · Execução passo a passo: README do projeto (.zip).</p>
</div>

<h2>Parte 1 — Banco de dados (Oracle)</h2>
<h3>1.1 Diagrama ER</h3>
<p>Tabelas com prefixo <code>RC_</code> (o esquema da faculdade é compartilhado), com chaves primárias, estrangeiras (inclusive compostas) e cardinalidades. Dicionário e regras de integridade: <code>docs/modelo-de-dados.md</code>.</p>
@@ER@@
<h3 class="quebra">1.2 Script SQL</h3>
<p><code>database/scripts/01_criar_esquema.sql</code> (CREATE TABLE com PK, FK, CHECK, UNIQUE e índices), <code>02_carga_sintetica.sql</code> (INSERTs: 3 contas, protocolo v0.1, 6 regras, 4 pedidos e histórico) e <code>04_testes_restricoes.sql</code> (tenta 33 violações e confere que o banco as recusa). Executados no Oracle 19c da faculdade; ordem e contagens em <code>database/scripts/README.md</code>. Senhas guardadas só como hash BCrypt.</p>
<h3>1.3 Consultas de uso (<code>03_consultas_uso.sql</code>)</h3>
@@CONSULTAS@@

<h2 class="quebra">Parte 2 — API RESTful (Java, Jakarta Servlet + Jetty, JDBC)</h2>
<p><b>Camadas:</b> Controller (HTTP, validação de contrato) → Service (casos de uso, autorização por recurso) → Repository (JDBC com SQL parametrizado). Sem Spring e sem JPA em produção. Motor de regras determinístico, independente de HTTP e de banco.</p>
<p><b>Documentação:</b> <code>docs/api/openapi.yaml</code> e <code>docs/api/RegulaCheck.postman_collection.json</code>. Erros no formato padrão (<code>codigo</code>, <code>mensagem</code>, <code>detalhes[]</code>), sem stack trace nem SQL.</p>
<table><tr><th>Método</th><th>Rota</th><th>Finalidade</th><th>Status HTTP</th></tr>@@ENDPOINTS@@</table>
<p>Mais de 20 endpoints cobrindo GET, POST, PUT e DELETE; mapa completo e perfil por rota em <code>CONTRATO_API.md</code>. Validação no servidor: corpo inválido → 400, serviço inexistente → 422, recurso alheio ou inexistente → 404 idêntico, formato de arquivo inválido → 415, arquivo grande → 413.</p>

<h2 class="quebra">Parte 3 — Plano de testes</h2>
<p>Plano completo com 94 casos (cenário, entrada, passos, resultado esperado, status): <code>docs/testes/plano-de-testes.md</code>. Em <b>2026-11-19</b> foram executados <b>10 casos</b> contra a API real (<code>regulacheck-backend.jar</code>) e o Oracle da faculdade, todos <b>Aprovados</b>; os outros 84 continuam “Não executado” no plano. Evidências (requisição, resposta bruta com token e senha mascarados, esperado × obtido) em <code>docs/evidencias/&lt;ID&gt;/</code>. Revisão por outro integrante: pendente.</p>
@@CASOS@@
<h4>Exemplo de evidência — T01-02 (senha incorreta), resposta bruta</h4><pre>@@EV1@@</pre>
<h4>Exemplo de evidência — T02-01 (isolamento): pedido de outra paciente</h4><pre>@@EV2@@</pre>

<h2 class="quebra">Parte 4 — Frontend mobile (Android, Kotlin, Jetpack Compose)</h2>
<p>O app consome somente a API do backend (OkHttp + kotlinx-serialization), sem acesso a banco nem a provedor de IA. Telas: <b>login</b>, <b>meus pedidos</b> (lista), <b>detalhe do pedido</b>, <b>envio de documentos</b> (cadastro) e <b>resultado da análise</b>. Estados tratados: carregando, vazio, sucesso, falha de conexão (“Sem conexão com a internet.” com “Tentar de novo”), serviço indisponível e sessão expirada (volta ao login). Botões bloqueados durante o envio; e-mail preservado após falha; token de sessão cifrado (AES-256-GCM, Android Keystore). Build e testes de API verificados em 17/11/2026 (<code>docs/evidencias/2026-11-17_rc18-app-android.md</code> e <code>…rc19…</code>). Conferência da leitura, substituição/remoção, comprovante e telas do administrador estão só desenhadas, não implementadas.</p>
@@TELAS@@

<h2 class="quebra">Parte 5 — Segurança</h2>
<table><tr><th>Item</th><th>Como atende</th></tr>
<tr><td>Login seguro</td><td>Senha armazenada só como hash <b>BCrypt</b> (custo 12, <code>$2a$12$…</code>); a sessão guarda só o hash do token; resposta de erro idêntica para senha errada e e-mail inexistente (T01-02 e T01-03 aprovados).</td></tr>
<tr><td>Controle de acesso</td><td>Perfis <code>PACIENTE</code> e <code>ADMINISTRADOR</code> com permissões diferentes por rota (<code>docs/api/autorizacao.md</code>); a paciente é sempre a da sessão; recurso de outra paciente responde 404 idêntico ao inexistente (T02-01 e T02-02 aprovados); o simulador tem chave restrita à telemetria.</td></tr>
<tr><td>Boas práticas (3+)</td><td>(1) validação de entrada no servidor com DTOs explícitos (T01-04, T04-02 e T04-03 aprovados); (2) SQL sempre parametrizado, sem concatenação (proteção contra SQL injection), e restrições CHECK/FK no banco (<code>04_testes_restricoes.sql</code>); (3) arquivos fora de diretório público, nome interno gerado, tipo real, tamanho e páginas conferidos, download autenticado (<code>docs/api/protecoes.md</code>); (4) respostas e logs sem senha, token, hash, SQL ou stack trace; (5) segredos fora do repositório e HTTPS exigido para URL remota.</td></tr></table>

<h2>Parte 6 — Simulação de IoT</h2>
<table><tr><th>Sensor</th><th>O que mede</th><th>Faixa demonstrativa</th><th>Como influencia o sistema</th></tr>
<tr><td><code>LUMINOSIDADE</code></td><td>Iluminação sobre o documento (lux)</td><td>300 a 1000 lux</td><td>Abaixo → “Ilumine melhor o documento antes de capturar”; acima → reduzir iluminação ou reflexo</td></tr>
<tr><td><code>DISTANCIA</code></td><td>Distância câmera–documento (cm)</td><td>20 a 40 cm</td><td>Abaixo → afastar a câmera; acima → “Aproxime um pouco a câmera do documento”</td></tr></table>
<p>Um script Java (<code>simulador-iot/SimuladorSensores.java</code>) simula os dois sensores e envia as leituras em JSON por HTTP a <code>POST /sessoes-captura/{id}/leituras</code> com credencial restrita; a API valida, grava no Oracle e a paciente consulta a situação e a orientação em <code>GET /solicitacoes/{id}/leituras</code>. Valores fora da faixa disparam a orientação; leitura com mais de 60 s aparece como desatualizada. Limites demonstrativos, sem calibração: as leituras <b>não comprovam a legibilidade</b> do arquivo e nunca bloqueiam o envio.</p>
<h4>Execução real (2026-11-19, pedido 153, sessão 66) — <code>docs/evidencias/IOT-simulacao/</code></h4>
<pre>@@IOT@@</pre>
<p>Após <code>fora-da-faixa</code>: luminosidade 110 lux → <b>ABAIXO_DA_FAIXA / MELHORAR_ILUMINACAO</b>; distância 58 cm → <b>ACIMA_DA_FAIXA / APROXIMAR_DOCUMENTO</b>. Após <code>recuperacao</code>: 650 lux e 28 cm → <b>DENTRO_DA_FAIXA / CONDICAO_ADEQUADA</b> nos dois sensores.</p>

<h2>Declaração de uso de IA generativa</h2>
<p>Claude Code (Anthropic) apoiou a implementação, os testes, os scripts SQL e a documentação, e o GitHub Spec Kit organizou especificação, plano e tarefas; o grupo revisou o resultado. A IA generativa não faz parte do funcionamento do produto.</p>
"""


def montar():
    integrantes = "".join("<tr><td>" + rm + "</td><td>" + esc(n) + "</td></tr>" for rm, n in INTEGRANTES)
    eps = "".join("<tr><td><b>" + m + "</b></td><td><code>" + esc(p) + "</code></td><td>" + esc(d) + "</td><td>" + s + "</td></tr>" for m, p, d, s in ENDPOINTS)
    corpo = (CORPO.replace("@@INTEGRANTES@@", integrantes).replace("@@ER@@", secao_er())
             .replace("@@CONSULTAS@@", consultas()).replace("@@ENDPOINTS@@", eps)
             .replace("@@CASOS@@", tabela_casos()).replace("@@EV1@@", trecho("T01-02", "01-login-errada.txt"))
             .replace("@@EV2@@", trecho("T02-01", "02-detalhe-b.txt")).replace("@@TELAS@@", telas())
             .replace("@@IOT@@", trecho("IOT-simulacao", "02-simulador-saida.txt", 1600)))
    cabeca = ('<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>RegulaCheck - Entregáveis</title><style>'
              + CSS + "</style>"
              '<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>'
              "<script>mermaid.initialize({startOnLoad:true, theme:'neutral'});</script></head><body>")
    return cabeca + corpo + "</body></html>"


def navegador():
    for caminho in (r"C:\Program Files\Google\Chrome\Application\chrome.exe",
                    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"):
        if os.path.exists(caminho):
            return caminho
    sys.exit("Instale o Google Chrome ou o Microsoft Edge.")


if __name__ == "__main__":
    (RAIZ / "entrega" / "prints").mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory() as pasta:
        pagina = Path(pasta) / "entregaveis.html"
        pagina.write_text(montar(), encoding="utf-8")
        subprocess.run([navegador(), "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                        "--virtual-time-budget=30000", "--print-to-pdf=" + str(SAIDA), pagina.as_uri()], check=True)
    print("PDF gerado:", SAIDA)
