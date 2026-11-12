# Simulador IoT

**Não implementado nesta etapa.** A pasta reserva o lugar do simulador.

Decisão (12/11/2026): 2 sensores simulados por **script Java** (`java.net.http.HttpClient`, sem Spring), que enviam as leituras em **JSON por HTTP** (`POST /sessoes-captura/{id}/leituras`) com credencial restrita (`IOT_API_KEY`). Luminosidade em lux e distância em cm; cada leitura com sessão de captura, tipo, valor, unidade e instante da medição. MQTT foi descartado. Os limites dos sensores são demonstrativos e não comprovam a legibilidade do documento. Contrato de rota em [CONTRATO_API.md](../CONTRATO_API.md); implementação na história de IoT (RC-21).
