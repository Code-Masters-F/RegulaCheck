# Simulador IoT

Sem implementacao nesta etapa. Decisao (12/11/2026): 2 sensores simulados por **script**, enviando as leituras em **JSON por HTTP** (`POST /sessoes-captura/{id}/leituras`) com credencial restrita (`IOT_API_KEY`). Luminosidade em lux e distancia em cm; cada leitura com tipo, valor, unidade e data. MQTT foi descartado. Linguagem: **Java** (cliente `java.net.http.HttpClient`, sem Spring).
