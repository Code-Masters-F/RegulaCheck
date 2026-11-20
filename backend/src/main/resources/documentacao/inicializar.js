// RC-28: configuracao do Swagger UI servido em GET /docs.
// - url: o contrato escrito a mao (docs/api/openapi.yaml), servido pela propria API.
// - BaseLayout: sem a barra superior que permitiria carregar outro contrato por URL.
// - validatorUrl null: o contrato nao e enviado a validador externo.
// - persistAuthorization false: o token informado em "Authorize" fica so na memoria da aba.
window.addEventListener("load", function () {
  window.ui = SwaggerUIBundle({
    url: "/docs/openapi.yaml",
    dom_id: "#swagger-ui",
    layout: "BaseLayout",
    presets: [SwaggerUIBundle.presets.apis],
    deepLinking: true,
    filter: true,
    displayRequestDuration: true,
    tryItOutEnabled: false,
    persistAuthorization: false,
    validatorUrl: null
  });
});
